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

type JvmSnapshot = {
  heapUsedMb: number | null;
  heapMaxMb: number | null;
  liveThreads: number | null;
  daemonThreads: number | null;
  loadedClasses: number | null;
  gcPauseMs: number | null;
  generatedAt: string;
};

const POLLING_INTERVAL_MS = 15000;

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

function toMegabytes(value: number | null, baseUnit: string | null): number | null {
  if (value === null) {
    return null;
  }

  if (!baseUnit) {
    return value;
  }

  const normalized = baseUnit.toLowerCase();
  if (normalized.includes('byte')) {
    return value / (1024 * 1024);
  }

  return value;
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

async function safeGetMetric(metricName: string, tags: string[] = []): Promise<MetricDetail | null> {
  try {
    return await getMonitoringMetric(metricName, tags);
  } catch {
    return null;
  }
}

export function SystemJvmView() {
  const [snapshot, setSnapshot] = useState<JvmSnapshot | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchMetrics = useCallback(async () => {
    setIsRefreshing(true);

    try {
      const [
        heapUsedMetric,
        heapMaxMetric,
        liveThreadsMetric,
        daemonThreadsMetric,
        loadedClassesMetric,
        gcPauseMetric,
      ] = await Promise.all([
        safeGetMetric('jvm.memory.used', ['area:heap']),
        safeGetMetric('jvm.memory.max', ['area:heap']),
        safeGetMetric('jvm.threads.live'),
        safeGetMetric('jvm.threads.daemon'),
        safeGetMetric('jvm.classes.loaded'),
        safeGetMetric('jvm.gc.pause'),
      ]);

      const generatedAt = new Date().toISOString();
      const heapUsedRaw = pickMeasurementValue(heapUsedMetric, ['VALUE']);
      const heapMaxRaw = pickMeasurementValue(heapMaxMetric, ['VALUE']);
      const liveThreads = pickMeasurementValue(liveThreadsMetric, ['VALUE']);
      const daemonThreads = pickMeasurementValue(daemonThreadsMetric, ['VALUE']);
      const loadedClasses = pickMeasurementValue(loadedClassesMetric, ['VALUE']);
      const gcPauseRaw = pickMeasurementValue(gcPauseMetric, ['MEAN', 'MAX', 'VALUE']);

      const heapUsedMb = toMegabytes(heapUsedRaw, heapUsedMetric?.baseUnit ?? null);
      const heapMaxMb = toMegabytes(heapMaxRaw, heapMaxMetric?.baseUnit ?? null);
      const gcPauseMs = toMilliseconds(gcPauseRaw, gcPauseMetric?.baseUnit ?? null);

      const nextSnapshot: JvmSnapshot = {
        heapUsedMb,
        heapMaxMb,
        liveThreads,
        daemonThreads,
        loadedClasses,
        gcPauseMs,
        generatedAt,
      };

      setSnapshot(nextSnapshot);
      setErrorMessage('');
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'JVM 메트릭 조회 중 오류가 발생했습니다.');
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

  const jvmBarOptions = useChart({
    xaxis: {
      categories: ['Live Threads', 'Daemon Threads'],
    },
    tooltip: {
      y: {
        formatter: (value: number) => fNumber(value, { maximumFractionDigits: 4 }),
      },
    },
  });

  const loadedClassBarOptions = useChart({
    xaxis: {
      categories: ['Loaded Classes'],
    },
    tooltip: {
      y: {
        formatter: (value: number) => fNumber(value, { maximumFractionDigits: 2 }),
      },
    },
  });

  const gcPauseLineOptions = useChart({
    xaxis: {
      categories: ['GC Pause(ms)'],
    },
    tooltip: {
      y: {
        formatter: (value: number) => fNumber(value, { maximumFractionDigits: 4 }),
      },
    },
    stroke: {
      curve: 'smooth',
      width: 3,
    },
    markers: {
      size: 5,
    },
  });

  const heapUsed = snapshot?.heapUsedMb ?? null;
  const heapMax = snapshot?.heapMaxMb ?? null;
  const heapUsedPct = heapUsed !== null && heapMax !== null && heapMax > 0 ? (heapUsed / heapMax) * 100 : null;
  const heapFreePct =
    heapUsedPct !== null ? Math.max(100 - heapUsedPct, 0) : null;

  const jvmPieOptions = useChart({
    labels: ['Heap Used', 'Heap Free'],
    legend: { show: true },
    plotOptions: { pie: { donut: { labels: { show: true } } } },
    tooltip: {
      y: {
        formatter: (value: number) => `${fNumber(value, { maximumFractionDigits: 2 })}%`,
      },
    },
  });

  const hasData = useMemo(() => {
    if (!snapshot) {
      return false;
    }

    return (
      snapshot.heapUsedMb !== null ||
      snapshot.heapMaxMb !== null ||
      snapshot.liveThreads !== null ||
      snapshot.daemonThreads !== null ||
      snapshot.loadedClasses !== null ||
      snapshot.gcPauseMs !== null
    );
  }, [snapshot]);

  return (
    <DashboardContent maxWidth="xl">
      <Stack direction={{ xs: 'column', md: 'row' }} justifyContent="space-between" sx={{ mb: 3 }}>
        <Box>
          <Typography variant="h4">System Metrics - JVM</Typography>
          <Typography variant="body2" sx={{ mt: 0.5, color: 'text.secondary' }}>
            JVM 리소스 모니터링
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
          현재 조회 가능한 JVM 메트릭 값이 없습니다.
        </Alert>
      )}

      {!isLoading && snapshot && (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, lg: 4 }}>
            <Card sx={{ height: 1 }}>
              <CardHeader title="Current Snapshot" />
              <CardContent>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Heap Used: {toDisplay(snapshot.heapUsedMb, ' MB')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Heap Max: {toDisplay(snapshot.heapMaxMb, ' MB')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Live Threads: {toDisplay(snapshot.liveThreads)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Daemon Threads: {toDisplay(snapshot.daemonThreads)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Loaded Classes: {toDisplay(snapshot.loadedClasses)}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  GC Pause: {toDisplay(snapshot.gcPauseMs, ' ms')}
                </Typography>
                <Chip
                  size="small"
                  sx={{ mt: 1.5 }}
                  color={heapUsed !== null && heapMax !== null && heapMax > 0 && (heapUsed / heapMax) * 100 > 80 ? 'warning' : 'success'}
                  label={
                    heapUsed !== null && heapMax !== null && heapMax > 0 && (heapUsed / heapMax) * 100 > 80
                      ? 'Heap usage high'
                      : 'Heap usage stable'
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
              <CardHeader title="Heap Composition (Pie)" />
              <Chart
                type="pie"
                series={[heapUsedPct ?? 0, heapFreePct ?? 0]}
                options={jvmPieOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 6 }}>
            <Card>
              <CardHeader title="Thread Status" />
              <Chart
                type="bar"
                series={[
                  {
                    name: 'Value',
                    data: [
                      snapshot.liveThreads ?? 0,
                      snapshot.daemonThreads ?? 0,
                    ],
                  },
                ]}
                options={jvmBarOptions}
                sx={{ p: 2.5, pb: 1, height: 360 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12, lg: 6 }}>
            <Card>
              <CardHeader title="Loaded Classes" />
              <Chart
                type="bar"
                series={[
                  { name: 'Loaded Classes', data: [snapshot.loadedClasses ?? 0] },
                ]}
                options={loadedClassBarOptions}
                sx={{ p: 2.5, pb: 1, height: 360 }}
              />
            </Card>
          </Grid>

          <Grid size={{ xs: 12 }}>
            <Card>
              <CardHeader title="GC Pause (ms)" />
              <Chart
                type="line"
                series={[
                  { name: 'GC Pause(ms)', data: [snapshot.gcPauseMs ?? 0] },
                ]}
                options={gcPauseLineOptions}
                sx={{ p: 2.5, pb: 1, height: 320 }}
              />
            </Card>
          </Grid>
        </Grid>
      )}
    </DashboardContent>
  );
}
