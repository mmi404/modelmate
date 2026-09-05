"use client";

import { createContext, useContext } from "react";
import type { UserDto } from "@/lib/api/types";

const AuthContext = createContext<UserDto | null>(null);

/**
 * Value comes straight from the server (see `app/layout.tsx`, which awaits
 * `getCurrentUser()`); there's no client-side fetch or local state to sync.
 * After login/logout, callers do `router.refresh()` so the root layout
 * re-runs on the server and this provider receives the new value as a prop.
 */
export function AuthProvider({ user, children }: { user: UserDto | null; children: React.ReactNode }) {
  return <AuthContext.Provider value={user}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
