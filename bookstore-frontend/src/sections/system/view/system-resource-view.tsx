import { useMemo, useState, useEffect, useCallback } from 'react';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Link from '@mui/material/Link';
import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import CardHeader from '@mui/material/CardHeader';
import Typography from '@mui/material/Typography';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';

import { RouterLink } from 'src/routes/components';

import { fNumber } from 'src/utils/format-number';

import { DashboardContent } from 'src/layouts/dashboard';
import { getMonitoringOverview } from 'src/services/admin-monitoring';

import { Chart, useChart } from 'src/components/chart';

type MonitoringOverviewData = Awaited<ReturnType<typeof getMonitoringOverview>>;
type MonitoringResources = NonNullable<MonitoringOverviewData['resources']>;

type TrendPoint = {
  label: string;
  serviceState: number;
  appCpuPct: number | null;
  heapUsedPct: number | null;
  hostCpuPct: number | null;
  hostMemPct: number | null;
  dbPoolUsagePct: number | null;
  requestsPerSecond: number | null;
  errorRate5xxPct: number | null;
  avgLatencyMs: number | null;
};

type SnapshotMetric = {
  label: string;
  value: number | null;
};

const POLLING_INTERVAL_MS = 15000;
const MAX_POINTS = 30;

function toServiceState(service: string | undefined): number {
  return service === 'UP' ? 1 : 0;
}

function toTimeLabel(isoDateTime: string): string {
  return new Date(isoDateTime).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function buildDbPoolUsage(resources: MonitoringResources | null): number | null {
  if (!resources || resources.dbPoolActive === null || resources.dbPoolMax === null || resources.dbPoolMax <= 0) {
    return null;
  }

  return (resources.dbPoolActive / resources.dbPoolMax) * 100;
}

function formatMetric(value: number | null, suffix = ''): string {
  if (value === null) {
    return 'No data';
  }

  return `${fNumber(value)}${suffix}`;
}

function formatUptime(uptimeSec: number): string {
  const days = Math.floor(uptimeSec / 86400);
  const hours = Math.floor((uptimeSec % 86400) / 3600);
  const minutes = Math.floor((uptimeSec % 3600) / 60);

  if (days > 0) {
    return `${days}d ${hours}h ${minutes}m`;
  }

  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }

  return `${minutes}m`;
}

function buildResourceSnapshot(resources: MonitoringResources | null): SnapshotMetric[] {
  return [
    { label: 'App CPU', value: resources?.appCpuPct ?? null },
    { label: 'Heap Used', value: resources?.heapUsedPct ?? null },
    { label: 'Host CPU', value: resources?.hostCpuPct ?? null },
    { label: 'Host Memory', value: resources?.hostMemPct ?? null },
    { label: 'DB Pool', value: buildDbPoolUsage(resources) },
  ];
}

export function SystemResourceView() {
  const [overview, setOverview] = useState<MonitoringOverviewData | null>(null);
  const [trend, setTrend] = useState<TrendPoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchOverview = useCallback(async () => {
    setIsRefreshing(true);

    try {
      const nextOverview = await getMonitoringOverview();
      const dbPoolUsagePct = buildDbPoolUsage(nextOverview.resources);

      setOverview(nextOverview);
      setErrorMessage('');
      setTrend((previous) => {
        const nextPoint: TrendPoint = {
          label: toTimeLabel(nextOverview.generatedAt),
          serviceState: toServiceState(nextOverview.status?.service),
          appCpuPct: nextOverview.resources?.appCpuPct ?? null,
          heapUsedPct: nextOverview.resources?.heapUsedPct ?? null,
          hostCpuPct: nextOverview.resources?.hostCpuPct ?? null,
          hostMemPct: nextOverview.resources?.hostMemPct ?? null,
          dbPoolUsagePct,
          requestsPerSecond: nextOverview.traffic?.requestsPerSecond ?? null,
          errorRate5xxPct: nextOverview.traffic?.errorRate5xxPct ?? null,
          avgLatencyMs: nextOverview.traffic?.avgLatencyMs ?? null,
        };

        return [...previous, nextPoint].slice(-MAX_POINTS);
      });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '시스템 리소스 조회 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void fetchOverview();

    const intervalId = window.setInterval(() => {
      void fetchOverview();
    }, POLLING_INTERVAL_MS);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [fetchOverview]);

  const resources = overview?.resources ?? null;
  const traffic = overview?.traffic ?? null;
  const status = overview?.status ?? null;

  const snapshotMetrics = useMemo(() => buildResourceSnapshot(resources), [resources]);
  const snapshotMissing = useMemo(
    () =>
      snapshotMetrics
        .filter((metric) => metric.value === null)
        .map((metric) => metric.label),
    [snapshotMetrics]
  );

  const categories = trend.map((point) => point.label);
  const labelStep = Math.max(1, Math.ceil(categories.length / 8));
  const sparseCategories = categories.map((label, index) => (index % labelStep === 0 ? label : ''));

  const serviceChartOptions = useChart({
    stroke: { width: 3, curve: 'straight' },
    markers: { size: 4 },
    xaxis: { categories },
    yaxis: {
      min: 0,
      max: 1,
      tickAmount: 1,
      labels: {
        formatter: (value: number) => (value >= 0.5 ? 'UP' : 'DOWN'),
      },
    },
    tooltip: {
      y: {
        formatter: (value: number) => (value >= 0.5 ? 'UP' : 'DOWN'),
      },
    },
  });

  const resourceChartOptions = useChart({
    xaxis: { categories: snapshotMetrics.map((metric) => metric.label), max: 100 },
    yaxis: { min: 0, max: 100, tickAmount: 5 },
    plotOptions: {
      bar: {
        horizontal: true,
        barHeight: '56%',
      },
    },
    dataLabels: {
      enabled: true,
      formatter: (value: number) => `${value.toFixed(2)}%`,
    },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)}%`,
      },
    },
  });

  const trafficRpsOptions = useChart({
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    stroke: { width: 2.5, curve: 'smooth' },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)} req/s`,
      },
    },
  });

  const trafficErrorOptions = useChart({
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    stroke: { width: 2.5, curve: 'smooth' },
    yaxis: { min: 0, max: 100, tickAmount: 5 },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)}%`,
      },
    },
  });

  const trafficLatencyOptions = useChart({
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    stroke: { width: 2.5, curve: 'smooth' },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)} ms`,
      },
    },
  });

  return (
    <DashboardContent maxWidth="xl">
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">System Monitoring</Typography>
          <Typography variant="body2" sx={{ mt: 0.5, color: 'text.secondary' }}>
            서버 상태 및 리소스 모니터링
          </Typography>
        </Box>

        <Button variant="contained" color="inherit" onClick={() => void fetchOverview()} disabled={isRefreshing}>
          {isRefreshing ? 'Refreshing...' : 'Refresh'}
        </Button>
      </Stack>

      {!!errorMessage && (
        <Alert
          severity="error"
          action={
            <Link component={RouterLink} href="/sign-in" color="inherit" underline="always">
              Sign in
            </Link>
          }
          sx={{ mb: 3 }}
        >
          {errorMessage}
        </Alert>
      )}

      {isLoading && (
        <Stack alignItems="center" sx={{ py: 8 }}>
          <CircularProgress />
        </Stack>
      )}

      {!isLoading && overview && (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 8 }}>
            <Card>
              <CardHeader
                title="Service Status Timeline"
                action={<Chip label={status?.service ?? 'UNKNOWN'} size="small" color={status?.service === 'UP' ? 'success' : 'warning'} />}
              />
              <Chart
                type="line"
                series={[{ name: 'Service', data: trend.map((point) => point.serviceState) }]}
                options={serviceChartOptions}
                sx={{ p: 2.5, pb: 1, height: 340 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 4 }}>
            <Card sx={{ height: 1 }}>
              <CardHeader title="Overview Snapshot" />
              <CardContent>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Uptime: {status ? formatUptime(status.uptimeSec) : 'No data'}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  App CPU: {formatMetric(resources?.appCpuPct ?? null, '%')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Heap Used: {formatMetric(resources?.heapUsedPct ?? null, '%')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Heap: {formatMetric(resources?.heapUsedMb ?? null, ' MB')} /{' '}
                  {formatMetric(resources?.heapMaxMb ?? null, ' MB')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  DB Pool: {formatMetric(resources?.dbPoolActive ?? null)} /{' '}
                  {formatMetric(resources?.dbPoolMax ?? null)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  RPS: {formatMetric(traffic?.requestsPerSecond ?? null)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  5xx Error: {formatMetric(traffic?.errorRate5xxPct ?? null, '%')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Avg latency: {formatMetric(traffic?.avgLatencyMs ?? null, ' ms')}
                </Typography>
                <Typography variant="caption" sx={{ mt: 1.5, display: 'block', color: 'text.secondary' }}>
                  Last updated: {new Date(overview.generatedAt).toLocaleString()}
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card>
              <CardHeader
                title="Resource Usage"
                subheader="Percentage"
                action={
                  <Chip
                    size="small"
                    label={snapshotMissing.length > 0 ? `No data: ${snapshotMissing.join(', ')}` : 'All metrics available'}
                    color={snapshotMissing.length > 0 ? 'warning' : 'success'}
                  />
                }
              />
              <Chart
                type="bar"
                series={[
                  {
                    name: 'Usage',
                    data: snapshotMetrics.map((metric) => metric.value ?? 0),
                  },
                ]}
                options={resourceChartOptions}
                sx={{ p: 2.5, pb: 1, height: 360 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 12 }}>
            <Card>
              <CardHeader title="Traffic: Avg Latency" />
              <Chart
                type="line"
                series={[
                  {
                    name: 'Latency',
                    data: trend.map((point) => point.avgLatencyMs),
                  },
                ]}
                options={trafficLatencyOptions}
                sx={{ p: 2.5, pb: 1, height: 280 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 12 }}>
            <Card>
              <CardHeader title="Traffic: Requests/s" />
              <Chart
                type="line"
                series={[
                  {
                    name: 'RPS',
                    data: trend.map((point) => point.requestsPerSecond),
                  },
                ]}
                options={trafficRpsOptions}
                sx={{ p: 2.5, pb: 1, height: 280 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 12 }}>
            <Card>
              <CardHeader title="Traffic: 5xx Error Rate" />
              <Chart
                type="line"
                series={[
                  {
                    name: '5xx Error',
                    data: trend.map((point) => point.errorRate5xxPct),
                  },
                ]}
                options={trafficErrorOptions}
                sx={{ p: 2.5, pb: 1, height: 280 }}
              />
            </Card>
          </Grid>
        </Grid>
      )}
    </DashboardContent>
  );
}
