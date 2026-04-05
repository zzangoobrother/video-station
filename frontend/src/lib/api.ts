import type { ErrorResponse } from '@/types';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || '';

let accessToken: string | null = null;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function getAccessToken(): string | null {
  if (!accessToken && typeof window !== 'undefined') {
    accessToken = localStorage.getItem('accessToken');
  }
  return accessToken;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error: ErrorResponse = await response.json().catch(() => ({
      status: response.status,
      code: 'UNKNOWN',
      message: response.statusText,
    }));
    throw new ApiError(error.status, error.code, error.message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
  });
  return handleResponse<T>(response);
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

export async function apiPut<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

export async function apiPatch<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
    body: body ? JSON.stringify(body) : undefined,
  });
  return handleResponse<T>(response);
}

export async function apiDelete(path: string): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'DELETE',
    headers: {
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
  });
  return handleResponse<void>(response);
}

export async function apiUpload<T>(path: string, formData: FormData): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: {
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
    body: formData,
  });
  return handleResponse<T>(response);
}
