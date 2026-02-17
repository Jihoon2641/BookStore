const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

const ADMIN_ACCESS_TOKEN_KEY = 'bookstore_admin_access_token';
const ADMIN_PROFILE_KEY = 'bookstore_admin_profile';

type ApiResponse<T> = {
  data: T;
  message: string;
  statusCode: string;
};

type AdminLoginResponse = {
  accessToken: string;
  id: number;
  adminId: string;
  role: string;
};

type AdminSignUpResponse = {
  adminId: string;
  role: string;
};

export type AdminProfile = {
  id: number;
  adminId: string;
  role: string;
};

type ErrorResponse = {
  message?: string;
  error?: string;
  detail?: string;
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

async function requestPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(body),
  });

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

export async function signInAdmin(adminId: string, password: string): Promise<AdminProfile> {
  const response = await requestPost<AdminLoginResponse>('/api/v1/admin/login', {
    adminId,
    password,
  });

  const profile: AdminProfile = {
    id: response.id,
    adminId: response.adminId,
    role: response.role,
  };

  localStorage.setItem(ADMIN_ACCESS_TOKEN_KEY, response.accessToken);
  localStorage.setItem(ADMIN_PROFILE_KEY, JSON.stringify(profile));

  return profile;
}

export async function signUpAdmin(adminId: string, password: string): Promise<AdminSignUpResponse> {
  return requestPost<AdminSignUpResponse>('/api/v1/admin/signup', {
    adminId,
    password,
  });
}

export function getAdminAccessToken(): string | null {
  return localStorage.getItem(ADMIN_ACCESS_TOKEN_KEY);
}

export function getAdminProfile(): AdminProfile | null {
  const profileRaw = localStorage.getItem(ADMIN_PROFILE_KEY);
  if (!profileRaw) {
    return null;
  }

  try {
    return JSON.parse(profileRaw) as AdminProfile;
  } catch {
    localStorage.removeItem(ADMIN_PROFILE_KEY);
    return null;
  }
}

export function getAdminAuthorizationHeader(): Record<string, string> {
  const token = getAdminAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function clearAdminSession(): void {
  localStorage.removeItem(ADMIN_ACCESS_TOKEN_KEY);
  localStorage.removeItem(ADMIN_PROFILE_KEY);
}

