import { getAdminAccessToken, getAdminAuthorizationHeader } from './admin-auth';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

type ApiResponse<T> = {
  data: T;
  message: string;
  statusCode: string;
};

type ErrorResponse = {
  message?: string;
  error?: string;
  detail?: string;
};

export type MonitoringHealth = {
  generatedAt: string;
  service: string;
  uptimeSec: number;
};

export type MonitoringResourceUsage = {
  appCpuPct: number | null;
  heapUsedPct: number | null;
  heapUsedMb: number | null;
  heapMaxMb: number | null;
  dbPoolActive: number | null;
  dbPoolMax: number | null;
  hostCpuPct: number | null;
  hostMemPct: number | null;
  diskUsagePct: number | null;
  diskIoUtilizationPct: number | null;
  networkRxKbs: number | null;
  networkTxKbs: number | null;
};

export type MonitoringTrafficUsage = {
  requestsPerSecond: number | null;
  errorRate5xxPct: number | null;
  avgLatencyMs: number | null;
};

export type MonitoringOverview = {
  generatedAt: string;
  status: MonitoringHealth;
  resources: MonitoringResourceUsage | null;
  traffic: MonitoringTrafficUsage | null;
};

export type MonitoringMetric = {
  generatedAt: string;
  name: string;
  description: string | null;
  baseUnit: string | null;
  measurements: Array<{
    statistic: string;
    value: number | null;
  }>;
  availableTags: Array<{
    tag: string;
    values: string[];
  }>;
};

export type MonitoringJvmSnapshot = {
  generatedAt: string;
  heapUsedMb: number | null;
  heapMaxMb: number | null;
  liveThreads: number | null;
  daemonThreads: number | null;
  loadedClasses: number | null;
  gcPauseMs: number | null;
};

export type MonitoringDbSnapshot = {
  generatedAt: string;
  active: number | null;
  idle: number | null;
  pending: number | null;
  max: number | null;
  timeoutCount: number | null;
  avgUsageMs: number | null;
};

type MonitoringStreamEvent = {
  event: string;
  data: string;
};

type MonitoringOverviewStreamOptions = {
  onOverview: (overview: MonitoringOverview) => void;
  onError?: (message: string) => void;
  onOpen?: () => void;
  onReconnect?: () => void;
};

type MonitoringJvmStreamOptions = {
  onJvm: (snapshot: MonitoringJvmSnapshot) => void;
  onError?: (message: string) => void;
  onOpen?: () => void;
  onReconnect?: () => void;
};

type MonitoringDbStreamOptions = {
  onDb: (snapshot: MonitoringDbSnapshot) => void;
  onError?: (message: string) => void;
  onOpen?: () => void;
  onReconnect?: () => void;
};

type MonitoringStreamOptions<T> = {
  onData: (payload: T) => void;
  onError?: (message: string) => void;
  onOpen?: () => void;
  onReconnect?: () => void;
};

class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function resolveErrorMessage(payload: unknown, fallback: string): string {
  if (typeof payload === 'object' && payload !== null) {
    const errorPayload = payload as ErrorResponse;

    if (typeof errorPayload.message === 'string' && errorPayload.message.trim().length > 0) {
      return errorPayload.message;
    }

    if (typeof errorPayload.detail === 'string' && errorPayload.detail.trim().length > 0) {
      return errorPayload.detail;
    }

    if (typeof errorPayload.error === 'string' && errorPayload.error.trim().length > 0) {
      return errorPayload.error;
    }
  }

  if (typeof payload === 'string' && payload.trim().length > 0) {
    return payload;
  }

  return fallback;
}

async function requestGet<T>(path: string): Promise<T> {
  const accessToken = getAdminAccessToken();
  if (!accessToken) {
    throw new ApiError('관리자 로그인 후 이용해 주세요.', 401);
  }

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'GET',
      headers: {
        ...getAdminAuthorizationHeader(),
      },
    });
  } catch {
    throw new ApiError('모니터링 API 연결에 실패했습니다. 백엔드/CORS/네트워크 상태를 확인해 주세요.', 0);
  }

  let payload: unknown = null;

  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  if (!response.ok) {
    const defaultMessage = `요청 처리에 실패했습니다. (HTTP ${response.status})`;
    throw new ApiError(resolveErrorMessage(payload, defaultMessage), response.status);
  }

  const apiResponse = payload as ApiResponse<T>;

  if (!apiResponse || typeof apiResponse !== 'object' || !('data' in apiResponse)) {
    throw new ApiError('서버 응답 형식이 올바르지 않습니다.', response.status);
  }

  return apiResponse.data;
}

export async function getMonitoringOverview(): Promise<MonitoringOverview> {
  return requestGet<MonitoringOverview>('/api/v1/admin/monitoring/overview');
}

export async function getMonitoringMetric(
  metricName: string,
  tags: string[] = []
): Promise<MonitoringMetric> {
  const params = new URLSearchParams();
  tags.forEach((tag) => {
    params.append('tag', tag);
  });

  const queryString = params.toString();
  const path = `/api/v1/admin/monitoring/metrics/${encodeURIComponent(metricName)}${queryString ? `?${queryString}` : ''}`;

  return requestGet<MonitoringMetric>(path);
}

const STREAM_RETRY_DELAY_MS = 3000;

function parseStreamEvent(rawEventBlock: string): MonitoringStreamEvent | null {
  const normalized = rawEventBlock.replace(/\r/g, '');
  if (!normalized.trim()) {
    return null;
  }

  let eventName = 'message';
  const dataLines: string[] = [];
  const lines = normalized.split('\n');

  lines.forEach((line) => {
    if (line.startsWith(':')) {
      return;
    }

    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
      return;
    }

    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart());
    }
  });

  if (dataLines.length === 0) {
    return null;
  }

  return {
    event: eventName,
    data: dataLines.join('\n'),
  };
}

async function consumeEventStream(
  stream: ReadableStream<Uint8Array>,
  signal: AbortSignal,
  onEvent: (event: MonitoringStreamEvent) => void
): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (!signal.aborted) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }

      buffer += decoder.decode(value, { stream: true });
      buffer = buffer.replace(/\r\n/g, '\n');

      let separatorIndex = buffer.indexOf('\n\n');
      while (separatorIndex >= 0) {
        const rawEvent = buffer.slice(0, separatorIndex);
        buffer = buffer.slice(separatorIndex + 2);

        const parsed = parseStreamEvent(rawEvent);
        if (parsed) {
          onEvent(parsed);
        }

        separatorIndex = buffer.indexOf('\n\n');
      }
    }
  } finally {
    reader.releaseLock();
  }
}

function resolveStreamErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return '실시간 모니터링 스트림 연결에 실패했습니다.';
}

function subscribeMonitoringStream<T>(
  path: string,
  eventName: string,
  options: MonitoringStreamOptions<T>
): () => void {
  const accessToken = getAdminAccessToken();
  if (!accessToken) {
    options.onError?.('관리자 로그인 후 이용해 주세요.');
    return () => {};
  }

  const controller = new AbortController();
  let closed = false;
  let reconnectTimerId: number | null = null;

  const clearReconnectTimer = () => {
    if (reconnectTimerId !== null) {
      window.clearTimeout(reconnectTimerId);
      reconnectTimerId = null;
    }
  };

  const scheduleReconnect = () => {
    if (closed || controller.signal.aborted) {
      return;
    }

    clearReconnectTimer();
    options.onReconnect?.();
    reconnectTimerId = window.setTimeout(() => {
      void connect();
    }, STREAM_RETRY_DELAY_MS);
  };

  const connect = async () => {
    if (closed || controller.signal.aborted) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}${path}`, {
        method: 'GET',
        headers: {
          ...getAdminAuthorizationHeader(),
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache',
        },
        signal: controller.signal,
      });

      if (response.status === 401 || response.status === 403) {
        options.onError?.('스트림 인증이 만료되었습니다. 다시 로그인해 주세요.');
        return;
      }

      if (!response.ok) {
        throw new ApiError(`스트림 연결에 실패했습니다. (HTTP ${response.status})`, response.status);
      }

      if (!response.body) {
        throw new ApiError('스트림 응답 본문이 비어 있습니다.', response.status);
      }

      options.onOpen?.();

      await consumeEventStream(response.body, controller.signal, (event) => {
        if (event.event !== eventName) {
          return;
        }

        try {
          const payload = JSON.parse(event.data) as T;
          options.onData(payload);
        } catch {
          options.onError?.('실시간 모니터링 데이터 파싱에 실패했습니다.');
        }
      });

      if (!closed && !controller.signal.aborted) {
        scheduleReconnect();
      }
    } catch (error) {
      if (closed || controller.signal.aborted) {
        return;
      }

      options.onError?.(resolveStreamErrorMessage(error));
      scheduleReconnect();
    }
  };

  void connect();

  return () => {
    closed = true;
    clearReconnectTimer();
    controller.abort();
  };
}

export function subscribeMonitoringOverviewStream(options: MonitoringOverviewStreamOptions): () => void {
  return subscribeMonitoringStream<MonitoringOverview>('/api/v1/admin/monitoring/stream/overview', 'overview', {
    onData: options.onOverview,
    onError: options.onError,
    onOpen: options.onOpen,
    onReconnect: options.onReconnect,
  });
}

export function subscribeMonitoringJvmStream(options: MonitoringJvmStreamOptions): () => void {
  return subscribeMonitoringStream<MonitoringJvmSnapshot>('/api/v1/admin/monitoring/stream/jvm', 'jvm', {
    onData: options.onJvm,
    onError: options.onError,
    onOpen: options.onOpen,
    onReconnect: options.onReconnect,
  });
}

export function subscribeMonitoringDbStream(options: MonitoringDbStreamOptions): () => void {
  return subscribeMonitoringStream<MonitoringDbSnapshot>('/api/v1/admin/monitoring/stream/db', 'db', {
    onData: options.onDb,
    onError: options.onError,
    onOpen: options.onOpen,
    onReconnect: options.onReconnect,
  });
}
