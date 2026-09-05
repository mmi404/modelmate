import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { SESSION_COOKIE } from "@/lib/auth/session";

const BACKEND_BASE = `${process.env.BACKEND_INTERNAL_URL ?? "http://localhost:8080"}/api/v1`;

/** Upstream calls give up rather than pinning a server thread on a hung backend. */
const UPSTREAM_TIMEOUT_MS = 15_000;

/**
 * Same-origin proxy so Client Components never need to know the backend's
 * address or CORS: `fetch('/api/backend/models')` etc. The browser's cookie
 * (httpOnly `mm_session`) rides along automatically on this same-origin
 * request; we read it here and forward it as a Bearer token server-to-server.
 */
async function forward(request: NextRequest, segments: string[]) {
  // Refuse traversal segments: without this, /api/backend/../actuator would
  // normalise out of the /api/v1 prefix and reach endpoints this proxy is not
  // meant to expose.
  if (segments.some((s) => s === ".." || s === "." || s.length === 0)) {
    return NextResponse.json({ message: "Invalid path" }, { status: 400 });
  }

  const path = "/" + segments.join("/");
  const token = (await cookies()).get(SESSION_COOKIE)?.value;

  const headers = new Headers();
  const contentType = request.headers.get("content-type");
  if (contentType) headers.set("content-type", contentType);
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const hasBody = !["GET", "HEAD"].includes(request.method);

  let response: Response;
  try {
    response = await fetch(`${BACKEND_BASE}${path}${request.nextUrl.search}`, {
      method: request.method,
      headers,
      body: hasBody ? await request.text() : undefined,
      cache: "no-store",
      signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
    });
  } catch (err) {
    const timedOut = err instanceof Error && err.name === "TimeoutError";
    console.error(`Backend proxy ${request.method} ${path} failed:`, err);
    return NextResponse.json(
      { message: timedOut ? "The server took too long to respond" : "Service unavailable" },
      { status: timedOut ? 504 : 502 }
    );
  }

  const responseContentType = response.headers.get("content-type");
  const responseBody = response.status === 204 ? null : await response.arrayBuffer();

  return new NextResponse(responseBody, {
    status: response.status,
    headers: {
      ...(responseContentType ? { "content-type": responseContentType } : {}),
      ...(response.headers.get("retry-after")
        ? { "retry-after": response.headers.get("retry-after")! }
        : {}),
    },
  });
}

type RouteParams = { params: Promise<{ path: string[] }> };

export async function GET(request: NextRequest, { params }: RouteParams) {
  return forward(request, (await params).path);
}
export async function POST(request: NextRequest, { params }: RouteParams) {
  return forward(request, (await params).path);
}
export async function PUT(request: NextRequest, { params }: RouteParams) {
  return forward(request, (await params).path);
}
export async function PATCH(request: NextRequest, { params }: RouteParams) {
  return forward(request, (await params).path);
}
export async function DELETE(request: NextRequest, { params }: RouteParams) {
  return forward(request, (await params).path);
}
