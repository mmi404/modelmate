import "server-only";
import { backendFetch } from "@/lib/api/backend-fetch";
import type { AdminStats, PageResponse, PendingModelDto } from "@/lib/api/types";

export function getAdminStats() {
  return backendFetch<AdminStats>("/admin/stats", { cache: "no-store" });
}

export function getPendingModels() {
  return backendFetch<PageResponse<PendingModelDto>>("/admin/models/pending?size=50", {
    cache: "no-store",
  });
}
