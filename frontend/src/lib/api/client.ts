"use client";

import type { ApiError } from "@/lib/api/types";

export class ApiClientError extends Error {
  status: number;
  fieldErrors?: Record<string, string> | null;

  constructor(status: number, message: string, fieldErrors?: Record<string, string> | null) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export interface ApiFetchOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

/**
 * Browser-side call, always same-origin through `/api/backend/*` (see that
 * route handler for why: it's the only place that can read the httpOnly
 * session cookie and turn it into a Bearer token).
 */
export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options;
  const finalHeaders = new Headers(headers);
  if (body !== undefined) {
    finalHeaders.set("Content-Type", "application/json");
  }

  const response = await fetch(`/api/backend${path}`, {
    ...rest,
    headers: finalHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const apiError = isJson ? (payload as ApiError) : null;
    throw new ApiClientError(
      response.status,
      apiError?.message ?? (typeof payload === "string" ? payload : "Request failed"),
      apiError?.fieldErrors
    );
  }

  return payload as T;
}
