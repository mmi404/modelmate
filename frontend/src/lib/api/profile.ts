import "server-only";
import { backendFetch } from "@/lib/api/backend-fetch";
import type { ContributionDto, PageResponse, UserDto } from "@/lib/api/types";

export function getPublicProfile(id: string) {
  return backendFetch<UserDto>(`/users/${encodeURIComponent(id)}`, {
    authenticated: false,
    next: { revalidate: 120 },
  });
}

export function getPublicContributions(id: string) {
  return backendFetch<PageResponse<ContributionDto>>(
    `/users/${encodeURIComponent(id)}/contributions?size=50`,
    { authenticated: false, next: { revalidate: 120 } },
  );
}

export function getMyContributions() {
  return backendFetch<PageResponse<ContributionDto>>("/me/contributions?size=50", {
    cache: "no-store",
  });
}
