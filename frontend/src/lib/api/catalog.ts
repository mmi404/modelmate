import "server-only";
import { backendFetch } from "@/lib/api/backend-fetch";
import type {
  CategoryDto,
  ModelCardDto,
  ModelDetailDto,
  PageResponse,
  ReviewDto,
} from "@/lib/api/types";

const PUBLIC = { authenticated: false } as const;
const HOURLY = { ...PUBLIC, next: { revalidate: 3600 } } as const;

export function getCategories() {
  return backendFetch<CategoryDto[]>("/categories", HOURLY);
}

export function getCategory(slug: string) {
  return backendFetch<CategoryDto>(`/categories/${encodeURIComponent(slug)}`, HOURLY);
}

export function getCategoryModels(slug: string, sort: string) {
  const q = new URLSearchParams({ sort, size: "48" });
  return backendFetch<PageResponse<ModelCardDto>>(
    `/categories/${encodeURIComponent(slug)}/models?${q}`,
    { ...PUBLIC, next: { revalidate: 300 } },
  );
}

export function getModel(slug: string) {
  return backendFetch<ModelDetailDto>(`/models/${encodeURIComponent(slug)}`, {
    ...PUBLIC,
    next: { revalidate: 300 },
  });
}

export function getModelReviews(modelId: number) {
  return backendFetch<PageResponse<ReviewDto>>(`/models/${modelId}/reviews?size=50`, {
    ...PUBLIC,
    next: { revalidate: 120 },
  });
}

export function getModelProblems(modelId: number) {
  return backendFetch<PageResponse<ReviewDto>>(`/models/${modelId}/problems?size=50`, {
    ...PUBLIC,
    next: { revalidate: 120 },
  });
}

export function compareModels(slugs: string[]) {
  const q = new URLSearchParams({ slugs: slugs.join(",") });
  return backendFetch<ModelDetailDto[]>(`/models/compare?${q}`, PUBLIC);
}

export function getModelNames() {
  return backendFetch<{ id: number; name: string; slug: string; creator: string | null }[]>(
    "/models/names",
    HOURLY,
  );
}
