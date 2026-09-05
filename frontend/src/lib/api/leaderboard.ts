import "server-only";
import { backendFetch } from "@/lib/api/backend-fetch";
import type { LeaderboardEntry } from "@/lib/api/types";

export function getLeaderboard(category?: string, minReviews = 1) {
  const q = new URLSearchParams({ minReviews: String(minReviews) });
  if (category) q.set("category", category);
  return backendFetch<LeaderboardEntry[]>(`/leaderboard?${q}`, {
    authenticated: false,
    next: { revalidate: 300 },
  });
}
