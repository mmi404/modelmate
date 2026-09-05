/** Everything about the session cookie in one place. */

export const SESSION_COOKIE = "mm_session";

/** Matches backend `modelmate.jwt.ttl-minutes` (see backend/src/main/resources/application.yml). */
export const SESSION_MAX_AGE_SECONDS = 60 * 60 * 24; // 24h

export const sessionCookieOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === "production",
  sameSite: "lax" as const,
  path: "/",
  maxAge: SESSION_MAX_AGE_SECONDS,
};
