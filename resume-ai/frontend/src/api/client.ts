import type { ApiErrorBody } from "./types";

/**
 * Thrown for any non-2xx response. Carries the parsed ApiError body when the
 * backend returned one (GlobalExceptionHandler always does for its own
 * errors), so callers can show field-level validation messages instead of a
 * generic "something went wrong".
 */
export class ApiRequestError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | null;

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.message ?? `Request failed with status ${status}`);
    this.status = status;
    this.body = body;
  }
}

// Requests go to a relative /api/v1/... path. In dev, vite.config.ts proxies
// /api to the backend (localhost:8080) so no CORS configuration is needed on
// the Spring Boot side. In a built/served bundle, whatever serves the static
// files is expected to proxy /api the same way (see frontend/Dockerfile).
const API_BASE = "/api/v1";

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, init);

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null;
    throw new ApiRequestError(response.status, body);
  }

  return (await response.json()) as T;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>(path, { method: "GET" });
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

// No Content-Type header here deliberately - the browser sets
// multipart/form-data with the correct boundary itself; setting it manually
// would produce a malformed request the backend can't parse.
export function apiPostMultipart<T>(path: string, formData: FormData): Promise<T> {
  return request<T>(path, { method: "POST", body: formData });
}

// Doesn't go through request<T>() - a 204 No Content response has no body,
// so calling .json() on it (request<T>'s normal success path) would throw.
export async function apiDelete(path: string): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`, { method: "DELETE" });
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null;
    throw new ApiRequestError(response.status, body);
  }
}
