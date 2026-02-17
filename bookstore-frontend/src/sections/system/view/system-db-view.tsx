import { useMemo, useState, useEffect, useCallback } from 'react';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Alert from '@mui/material/Alert';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import CardHeader from '@mui/material/CardHeader';
import Typography from '@mui/material/Typography';
import CardContent from '@mui/material/CardContent';
import CircularProgress from '@mui/material/CircularProgress';

import { fNumber } from 'src/utils/format-number';

import { DashboardContent } from 'src/layouts/dashboard';
import { getMonitoringMetric } from 'src/services/admin-monitoring';

import { Chart, useChart } from 'src/components/chart';

type MetricDetail = Awaited<ReturnType<typeof getMonitoringMetric>>;

type DbSnapshot = {
  active: number | null;
  idle: number | null;
  pending: number | null;
  max: number | null;
  timeoutCount: number | null;
  avgUsageMs: number | null;
  generatedAt: string;
};

type DbTrendPoint = {
  label: string;
  avgUsageMs: number | null;
  timeoutCount: number | null;
};

const POLLING_INTERVAL_MS = 15000;
const MAX_POINTS = 30;

function toTimeLabel(isoDateTime: string): string {
  return new Date(isoDateTime).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function pickMeasurementValue(metric: MetricDetail | null, statistics: string[]): number | null {
  if (!metric) {
    return null;
  }

  for (const statistic of statistics) {
    const measurement = metric.measurements.find((item) => item.statistic === statistic);
    if (measurement && measurement.value !== null) {
      return measurement.value;
    }
  }

  return metric.measurements.find((item) => item.value !== null)?.value ?? null;
}

function toMilliseconds(value: number | null, baseUnit: string | null): number | null {
  if (value === null) {
    return null;
  }

  if (!baseUnit) {
    return value;
  }

  const normalized = baseUnit.toLowerCase();
  if (normalized.includes('second')) {
    return value * 1000;
  }

  return value;
}

function toDisplay(value: number | null, suffix = ''): string {
  if (value === null) {
    return 'No data';
  }

  return `${fNumber(value, { maximumFractionDigits: 4 })}${suffix}`;
}

async function safeGetMetric(metricName: string): Promise<MetricDetail | null> {
  try {
    return await getMonitoringMetric(metricName);
  } catch {
    return null;
  }
}

export function SystemDbView() {
  const [snapshot, setSnapshot] = useState<DbSnapshot | null>(null);
  const [trend, setTrend] = useState<DbTrendPoint[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchMetrics = useCallback(async () => {
    setIsRefreshing(true);

    try {
      const [activeMetric, idleMetric, pendingMetric, maxMetric, timeoutMetric, usageMetric] =
        await Promise.all([
          safeGetMetric('hikaricp.connections.active'),
          safeGetMetric('hikaricp.connections.idle'),
          safeGetMetric('hikaricp.connections.pending'),
          safeGetMetric('hikaricp.connections.max'),
          safeGetMetric('hikaricp.connections.timeout'),
          safeGetMetric('hikaricp.connections.usage'),
        ]);

      const generatedAt = new Date().toISOString();
      const active = pickMeasurementValue(activeMetric, ['VALUE']);
      const idle = pickMeasurementValue(idleMetric, ['VALUE']);
      const pending = pickMeasurementValue(pendingMetric, ['VALUE']);
      const max = pickMeasurementValue(maxMetric, ['VALUE']);
      const timeoutCount = pickMeasurementValue(timeoutMetric, ['COUNT', 'VALUE']);
      const usageRaw = pickMeasurementValue(usageMetric, ['MEAN', 'MAX', 'TOTAL_TIME', 'VALUE']);
      const avgUsageMs = toMilliseconds(usageRaw, usageMetric?.baseUnit ?? null);

      const nextSnapshot: DbSnapshot = {
        active,
        idle,
        pending,
        max,
        timeoutCount,
        avgUsageMs,
        generatedAt,
      };

      setSnapshot(nextSnapshot);
      setErrorMessage('');
      setTrend((previous) => {
        const nextPoint: DbTrendPoint = {
          label: toTimeLabel(generatedAt),
          avgUsageMs,
          timeoutCount,
        };

        return [...previous, nextPoint].slice(-MAX_POINTS);
      });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'DB 메트릭 조회 중 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void fetchMetrics();

    const intervalId = window.setInterval(() => {
      void fetchMetrics();
    }, POLLING_INTERVAL_MS);

    return () => {
      window.clearInterval(intervalId);
    };
  }, [fetchMetrics]);

  const categories = trend.map((point) => point.label);
  const labelStep = Math.max(1, Math.ceil(categories.length / 8));
  const sparseCategories = categories.map((label, index) => (index % labelStep === 0 ? label : ''));

  const dbLineOptions = useChart({
    xaxis: { categories: sparseCategories, labels: { hideOverlappingLabels: true } },
    tooltip: {
      y: {
        formatter: (value: number) => fNumber(value, { maximumFractionDigits: 3 }),
      },
    },
  });

  const dbBarOptions = useChart({
    xaxis: {
      categories: ['Active', 'Idle', 'Pending', 'Max'],
    },
    tooltip: {
      y: {
        formatter: (value: number) => fNumber(value, { maximumFractionDigits: 4 }),
      },
    },
  });

  const dbPieOptions = useChart({
    labels: ['Active', 'Idle', 'Pending'],
    legend: { show: true },
    plotOptions: { pie: { donut: { labels: { show: true } } } },
    tooltip: {
      y: {
        formatter: (value: number) => fNumber(value, { maximumFractionDigits: 2 }),
      },
    },
  });

  const hasData = useMemo(() => {
    if (!snapshot) {
      return false;
    }

    return (
      snapshot.active !== null ||
      snapshot.idle !== null ||
      snapshot.pending !== null ||
      snapshot.max !== null ||
      snapshot.timeoutCount !== null ||
      snapshot.avgUsageMs !== null
    );
  }, [snapshot]);

  return (
    <DashboardContent maxWidth="xl">
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">System Metrics - DB (HikariCP)</Typography>
          <Typography variant="body2" sx={{ mt: 0.5, color: 'text.secondary' }}>
            HikariCP 모니터링
          </Typography>
        </Box>

        <Button variant="contained" color="inherit" onClick={() => void fetchMetrics()} disabled={isRefreshing}>
          {isRefreshing ? 'Refreshing...' : 'Refresh'}
        </Button>
      </Stack>

      {!!errorMessage && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {errorMessage}
        </Alert>
      )}

      {isLoading && (
        <Stack alignItems="center" sx={{ py: 8 }}>
          <CircularProgress />
        </Stack>
      )}

      {!isLoading && !hasData && (
        <Alert severity="info" sx={{ mb: 3 }}>
          현재 조회 가능한 DB 메트릭 값이 없습니다.
        </Alert>
      )}

      {!isLoading && snapshot && (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 4 }}>
            <Card sx={{ height: 1 }}>
              <CardHeader title="Current Snapshot" />
              <CardContent>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Active: {toDisplay(snapshot.active)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Idle: {toDisplay(snapshot.idle)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Pending: {toDisplay(snapshot.pending)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Max: {toDisplay(snapshot.max)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Timeout Count: {toDisplay(snapshot.timeoutCount)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Avg Usage: {toDisplay(snapshot.avgUsageMs, ' ms')}
                </Typography>
                <Chip
                  size="small"
                  sx={{ mt: 1.5 }}
                  color={snapshot.pending !== null && snapshot.pending > 0 ? 'warning' : 'success'}
                  label={
                    snapshot.pending !== null && snapshot.pending > 0
                      ? 'Pending exists'
                      : 'Pending stable'
                  }
                />
                <Typography variant="caption" sx={{ mt: 1.5, display: 'block', color: 'text.secondary' }}>
                  Updated: {new Date(snapshot.generatedAt).toLocaleString()}
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 8 }}>
            <Card>
              <CardHeader title="Connections Composition" />
              <Chart
                type="pie"
                series={[snapshot.active ?? 0, snapshot.idle ?? 0, snapshot.pending ?? 0]}
                options={dbPieOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card>
              <CardHeader title="Connections Pool Status" />
              <Chart
                type="bar"
                series={[
                  {
                    name: 'Value',
                    data: [
                      snapshot.active ?? 0,
                      snapshot.idle ?? 0,
                      snapshot.pending ?? 0,
                      snapshot.max ?? 0,
                    ],
                  },
                ]}
                options={dbBarOptions}
                sx={{ p: 2.5, pb: 1, height: 360 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card>
              <CardHeader title="Avg Usage(ms)" />
              <Chart
                type="line"
                series={[
                  { name: 'Avg Usage(ms)', data: trend.map((point) => point.avgUsageMs) },
                ]}
                options={dbLineOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card>
              <CardHeader title="Timeout Count" />
              <Chart
                type="line"
                series={[
                  { name: 'Timeout Count', data: trend.map((point) => point.timeoutCount) },
                ]}
                options={dbLineOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
            </Card>
          </Grid>
        </Grid>
      )}
    </DashboardContent>
  );
}
