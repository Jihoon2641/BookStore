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
