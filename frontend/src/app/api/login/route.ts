import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { backendFetch, BackendError } from "@/lib/api/backend-fetch";
import { SESSION_COOKIE, sessionCookieOptions } from "@/lib/auth/session";
import type { AuthResponse } from "@/lib/api/types";

export async function POST(request: NextRequest) {
  const body = await request.json();

  try {
    const data = await backendFetch<AuthResponse>("/auth/login", {
      method: "POST",
      body,
      authenticated: false,
    });

    (await cookies()).set(SESSION_COOKIE, data.token, sessionCookieOptions);
    return NextResponse.json({ user: data.user });
  } catch (err) {
    if (err instanceof BackendError) {
      return NextResponse.json(err.apiError ?? { message: err.message }, { status: err.status });
    }
    return NextResponse.json({ message: "Unexpected error" }, { status: 500 });
  }
}
