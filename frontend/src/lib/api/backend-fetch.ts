import "server-only";
import { cookies } from "next/headers";
import { SESSION_COOKIE } from "@/lib/auth/session";
import type { ApiError } from "@/lib/api/types";

const BACKEND_BASE = `${process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080"}/api/v1`;

export class BackendError extends Error {
  status: number;
  apiError: ApiError | null;

  constructor(status: number, apiError: ApiError | null, message: string) {
    super(message);
    this.name = "BackendError";
    this.status = status;
    this.apiError = apiError;
  }
}

export interface BackendFetchOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  /** Attach the caller's session as `Authorization: Bearer <token>`. Default: true. */
  authenticated?: boolean;
  /** Forward straight through instead of JSON-encoding `body` (unused today, kept for future uploads). */
  rawBody?: boolean;
  /** Next.js fetch cache hints: `{ revalidate: 3600 }` or `{ tags: ['models'] }`. */
  next?: { revalidate?: number | false; tags?: string[] };
}

/**
 * Server-only call into the Spring Boot API. Used directly by Server
 * Components/pages, and by the `/api/backend/[...path]` proxy that Client
 * Components go through (see `lib/api/client.ts`).
 */
export async function backendFetch<T>(path: string, options: BackendFetchOptions = {}): Promise<T> {
  const { body, authenticated = true, rawBody, headers, next, ...rest } = options;

  const finalHeaders = new Headers(headers);
  if (body !== undefined && !rawBody) {
    finalHeaders.set("Content-Type", "application/json");
  }

  if (authenticated) {
    const token = (await cookies()).get(SESSION_COOKIE)?.value;
    if (token) {
      finalHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${BACKEND_BASE}${path}`, {
    ...rest,
    headers: finalHeaders,
    body: body === undefined ? undefined : rawBody ? (body as BodyInit) : JSON.stringify(body),
    next,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const isJson = response.headers.get("content-type")?.includes("application/json");
  const payload = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const apiError = isJson ? (payload as ApiError) : null;
    throw new BackendError(response.status, apiError, apiError?.message ?? String(payload));
  }

  return payload as T;
}
