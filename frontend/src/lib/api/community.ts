import "server-only";
import { backendFetch } from "@/lib/api/backend-fetch";
import type {
  DiscussionDto,
  DiscussionStats,
  PageResponse,
  ReplyDto,
  TagCountDto,
} from "@/lib/api/types";

/**
 * Community reads are rendered dynamically (no ISR): the list carries a
 * per-user `myVote` and changes often. Public per ADR-012 — no auth required,
 * but we forward the session when present so vote state is accurate.
 */
const DYNAMIC = { cache: "no-store" } as const;

export function getDiscussions(params: { tags?: string[]; sort?: string }) {
  const q = new URLSearchParams();
  if (params.tags?.length) q.set("tags", params.tags.join(","));
  if (params.sort) q.set("sort", params.sort);
  q.set("size", "30");
  return backendFetch<PageResponse<DiscussionDto>>(`/discussions?${q}`, DYNAMIC);
}

export function getDiscussion(id: string) {
  return backendFetch<DiscussionDto>(`/discussions/${encodeURIComponent(id)}`, DYNAMIC);
}

export function getReplies(id: string) {
  return backendFetch<ReplyDto[]>(`/discussions/${encodeURIComponent(id)}/replies`, DYNAMIC);
}

export function getDiscussionTags() {
  return backendFetch<TagCountDto[]>("/discussions/tags", {
    authenticated: false,
    next: { revalidate: 600 },
  });
}

export function getCommunityStats() {
  return backendFetch<DiscussionStats>("/discussions/stats", {
    authenticated: false,
    next: { revalidate: 600 },
  });
}
