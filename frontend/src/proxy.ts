import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { SESSION_COOKIE } from "@/lib/auth/session";

/**
 * Route gate (Next.js 16 renamed `middleware` -> `proxy`, same mechanics).
 * Cheap check only: does a session cookie exist? The authoritative check
 * (is it still valid?) happens per-page via `getCurrentUser()`, which redirects
 * to /login itself if the token turned out to be expired/invalid. Per
 * ADR-012, browsing pages (home, categories, models, compare, leaderboard,
 * community reads) stay public for SEO/GEO; only write/personal routes gate.
 */
const PROTECTED_PREFIXES = [
  "/submit-model",
  "/submit-review",
  "/profile",
  "/community/new",
  "/admin",
];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isProtected = PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
  );
  if (!isProtected) {
    return NextResponse.next();
  }

  const hasSession = request.cookies.has(SESSION_COOKIE);
  if (hasSession) {
    return NextResponse.next();
  }

  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("next", pathname);
  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: [
    "/submit-model/:path*",
    "/submit-review/:path*",
    "/profile/:path*",
    "/community/new",
    "/admin/:path*",
  ],
};
