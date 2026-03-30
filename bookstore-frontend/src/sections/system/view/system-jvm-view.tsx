import { useMemo, useState, useEffect } from 'react';

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
import {
  type MonitoringJvmSnapshot,
  subscribeMonitoringJvmStream,
} from 'src/services/admin-monitoring';

import { Chart, useChart } from 'src/components/chart';

type JvmSnapshot = MonitoringJvmSnapshot;

function toDisplay(value: number | null, suffix = ''): string {
  if (value === null) {
    return 'No data';
  }

  return `${fNumber(value, { maximumFractionDigits: 4 })}${suffix}`;
}

export function SystemJvmView() {
  const [snapshot, setSnapshot] = useState<JvmSnapshot | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [streamVersion, setStreamVersion] = useState(0);

  useEffect(() => {
    const unsubscribe = subscribeMonitoringJvmStream({
      onOpen: () => {
        setErrorMessage('');
        setIsRefreshing(false);
      },
      onJvm: (nextSnapshot) => {
        setSnapshot(nextSnapshot);
        setErrorMessage('');
        setIsLoading(false);
        setIsRefreshing(false);
      },
      onError: (message) => {
        setErrorMessage(message);
        setIsRefreshing(false);
      },
      onReconnect: () => {
        setIsRefreshing(true);
      },
    });

    return () => {
      unsubscribe();
    };
  }, [streamVersion]);

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

        <Button
          variant="contained"
          color="inherit"
          onClick={() => {
            setIsRefreshing(true);
            setStreamVersion((previous) => previous + 1);
          }}
          disabled={isRefreshing}
        >
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
