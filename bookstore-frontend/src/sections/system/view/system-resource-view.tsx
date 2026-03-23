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

type SummaryItem = {
  title: string;
  value: string;
  detail: string;
  tone: 'success' | 'warning' | 'error' | 'info';
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

function formatPercent(value: number | null): string {
  if (value === null) {
    return 'No data';
  }

  return `${fNumber(value, { maximumFractionDigits: 2 })}%`;
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
  const dbPoolUsageNow = buildDbPoolUsage(resources);
  const heapUsedMb = resources?.heapUsedMb ?? null;
  const heapMaxMb = resources?.heapMaxMb ?? null;
  const heapFreeMb = heapUsedMb !== null && heapMaxMb !== null ? Math.max(heapMaxMb - heapUsedMb, 0) : null;
  const successRateNow =
    traffic?.errorRate5xxPct !== null && traffic?.errorRate5xxPct !== undefined
      ? Math.max(100 - traffic.errorRate5xxPct, 0)
      : null;

  const summaryItems: SummaryItem[] = [
    {
      title: 'Service',
      value: status?.service ?? 'UNKNOWN',
      detail: status ? `Uptime ${formatUptime(status.uptimeSec)}` : 'Uptime No data',
      tone: status?.service === 'UP' ? 'success' : 'warning',
    },
    {
      title: 'App CPU / Heap',
      value: `${formatPercent(resources?.appCpuPct ?? null)} / ${formatPercent(resources?.heapUsedPct ?? null)}`,
      detail: `Heap ${formatMetric(resources?.heapUsedMb ?? null, ' MB')} / ${formatMetric(resources?.heapMaxMb ?? null, ' MB')}`,
      tone: 'info',
    },
    {
      title: 'DB Pool',
      value: `${formatMetric(resources?.dbPoolActive ?? null)} / ${formatMetric(resources?.dbPoolMax ?? null)}`,
      detail: `Utilization ${formatPercent(dbPoolUsageNow)}`,
      tone: dbPoolUsageNow !== null && dbPoolUsageNow > 80 ? 'warning' : 'success',
    },
    {
      title: 'Traffic',
      value: `${formatMetric(traffic?.requestsPerSecond ?? null)} req/s`,
      detail: `Latency ${formatMetric(traffic?.avgLatencyMs ?? null, ' ms')} | 5xx ${formatPercent(traffic?.errorRate5xxPct ?? null)}`,
      tone: (traffic?.errorRate5xxPct ?? 0) > 1 ? 'error' : 'success',
    },
  ];

  const resourceSnapshotOptions = useChart({
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

  const resourceTrendOptions = useChart({
    legend: { show: true, position: 'top', horizontalAlign: 'left' },
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    yaxis: {
      min: 0,
      max: 100,
      tickAmount: 5,
      labels: {
        formatter: (value: number) => `${value.toFixed(0)}%`,
      },
    },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)}%`,
      },
    },
  });

  const trafficTrendOptions = useChart({
    chart: { stacked: false },
    legend: { show: true, position: 'top', horizontalAlign: 'left' },
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    stroke: { width: [3, 3], curve: 'smooth' },
    fill: { type: ['solid', 'gradient'], opacity: [1, 0.2] },
    yaxis: [
      {
        title: {
          text: 'Requests/s',
        },
        labels: {
          formatter: (value: number) => value.toFixed(2),
        },
      },
      {
        opposite: true,
        title: {
          text: 'Latency (ms)',
        },
        labels: {
          formatter: (value: number) => value.toFixed(0),
        },
      },
    ],
    tooltip: {
      shared: true,
      intersect: false,
      y: {
        formatter: (value: number, context?: { seriesIndex?: number }) => {
          if (context?.seriesIndex === 0) {
            return `${value.toFixed(2)} req/s`;
          }
          return `${value.toFixed(2)} ms`;
        },
      },
    },
  });

  const serviceReliabilityOptions = useChart({
    legend: { show: true, position: 'top', horizontalAlign: 'left' },
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    stroke: { width: [3, 2.5], curve: 'smooth' },
    fill: { type: ['solid', 'gradient'], opacity: [1, 0.2] },
    yaxis: [
      {
        min: 0,
        max: 1,
        tickAmount: 1,
        labels: {
          formatter: (value: number) => (value >= 0.5 ? 'UP' : 'DOWN'),
        },
      },
      {
        min: 0,
        max: 100,
        tickAmount: 5,
        opposite: true,
        labels: {
          formatter: (value: number) => `${value.toFixed(0)}%`,
        },
      },
    ],
    tooltip: {
      y: {
        formatter: (value: number, context?: { seriesIndex?: number }) => {
          if (context?.seriesIndex === 0) {
            return value >= 0.5 ? 'UP' : 'DOWN';
          }
          return `${value.toFixed(2)}%`;
        },
      },
    },
  });

  const trafficQualityOptions = useChart({
    labels: ['Success', '5xx Error'],
    legend: { show: true, position: 'bottom', horizontalAlign: 'center' },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)}%`,
      },
    },
  });

  const heapSplitOptions = useChart({
    labels: ['Heap Used', 'Heap Free'],
    legend: { show: true, position: 'bottom', horizontalAlign: 'center' },
    tooltip: {
      y: {
        formatter: (value: number) => `${value.toFixed(2)} MB`,
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
          {summaryItems.map((item) => (
            <Grid key={item.title} size={{ xs: 12, sm: 6, lg: 3 }}>
              <Card sx={{ height: 1 }}>
                <CardContent>
                  <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
                    <Typography variant="subtitle2" sx={{ color: 'text.secondary' }}>
                      {item.title}
                    </Typography>
                    <Chip size="small" color={item.tone} label={item.tone === 'success' ? 'Good' : 'Check'} />
                  </Stack>
                  <Typography variant="h5" sx={{ mb: 1 }}>
                    {item.value}
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    {item.detail}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}

          <Grid size={{ xs: 12, lg: 8 }}>
            <Card>
              <CardHeader title="Resource Trend (Line)" subheader="CPU / Memory / DB Pool utilization in percentage" />
              <Chart
                type="line"
                series={[
                  { name: 'App CPU', data: trend.map((point) => point.appCpuPct) },
                  { name: 'Heap Used', data: trend.map((point) => point.heapUsedPct) },
                  { name: 'Host CPU', data: trend.map((point) => point.hostCpuPct) },
                  { name: 'Host Memory', data: trend.map((point) => point.hostMemPct) },
                  { name: 'DB Pool', data: trend.map((point) => point.dbPoolUsagePct) },
                ]}
                options={resourceTrendOptions}
                sx={{ p: 2.5, pb: 1, height: 360 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 4 }}>
            <Card>
              <CardHeader
                title="Current Resource Snapshot (Bar)"
                action={
                  <Chip
                    size="small"
                    label={snapshotMissing.length > 0 ? `No data ${snapshotMissing.length}` : 'Complete'}
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
                options={resourceSnapshotOptions}
                sx={{ p: 2.5, pb: 1, height: 360 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 8 }}>
            <Card>
              <CardHeader title="Traffic Trend (Line + Area)" subheader="Requests/s and average latency" />
              <Chart
                type="line"
                series={[
                  { name: 'Requests/s', type: 'line', data: trend.map((point) => point.requestsPerSecond) },
                  { name: 'Avg Latency', type: 'area', data: trend.map((point) => point.avgLatencyMs) },
                ]}
                options={trafficTrendOptions}
                sx={{ p: 2.5, pb: 1, height: 340 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 4 }}>
            <Card>
              <CardHeader
                title="Traffic Quality (Pie)"
                subheader="Latest request quality split"
                action={
                  <Chip
                    size="small"
                    color={(traffic?.errorRate5xxPct ?? 0) > 1 ? 'warning' : 'success'}
                    label={formatPercent(traffic?.errorRate5xxPct ?? null)}
                  />
                }
              />
              <Chart
                type="pie"
                series={[successRateNow ?? 0, traffic?.errorRate5xxPct ?? 0]}
                options={trafficQualityOptions}
                sx={{ p: 2.5, pb: 1, height: 340 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 8 }}>
            <Card>
              <CardHeader
                title="Service Reliability (Line + Area)"
                subheader="Service state and 5xx error rate over time"
                action={<Chip label={status?.service ?? 'UNKNOWN'} size="small" color={status?.service === 'UP' ? 'success' : 'warning'} />}
              />
              <Chart
                type="line"
                series={[
                  { name: 'Service', type: 'line', data: trend.map((point) => point.serviceState) },
                  { name: '5xx Error Rate', type: 'area', data: trend.map((point) => point.errorRate5xxPct) },
                ]}
                options={serviceReliabilityOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 4 }}>
            <Card sx={{ height: 1 }}>
              <CardHeader title="Heap Split (Pie)" subheader="Latest heap used/free in MB" />
              <Chart
                type="pie"
                series={[heapUsedMb ?? 0, heapFreeMb ?? 0]}
                options={heapSplitOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
              <CardContent>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Updated: {new Date(overview.generatedAt).toLocaleString()}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </DashboardContent>
  );
}
