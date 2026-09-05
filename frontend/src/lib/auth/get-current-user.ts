import "server-only";
import { cache } from "react";
import { cookies } from "next/headers";
import { backendFetch, BackendError } from "@/lib/api/backend-fetch";
import { SESSION_COOKIE } from "@/lib/auth/session";
import type { UserDto } from "@/lib/api/types";

/**
 * Resolves the current user from the session cookie, deduped per request
 * (React `cache`) so layout + page can both call it for free.
 * Returns `null` when signed out or the token is invalid/expired.
 */
export const getCurrentUser = cache(async (): Promise<UserDto | null> => {
  const token = (await cookies()).get(SESSION_COOKIE)?.value;
  if (!token) {
    return null;
  }
  try {
    return await backendFetch<UserDto>("/auth/me", { next: { revalidate: 0 } });
  } catch (err) {
    if (err instanceof BackendError && (err.status === 401 || err.status === 403)) {
      return null;
    }
    throw err;
  }
});
