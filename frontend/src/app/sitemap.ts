import type { MetadataRoute } from "next";
import { backendFetch } from "@/lib/api/backend-fetch";
import { SITE_URL } from "@/lib/site";
import type { CategoryDto, DiscussionDto, PageResponse } from "@/lib/api/types";

// Rebuilt on the same cadence as the pages it lists.
export const revalidate = 3600;

type ModelSummary = { slug: string };

async function safe<T>(p: Promise<T>, fallback: T): Promise<T> {
  try {
    return await p;
  } catch {
    return fallback;
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const [categories, models, discussions] = await Promise.all([
    safe(backendFetch<CategoryDto[]>("/categories", { authenticated: false }), []),
    safe(backendFetch<ModelSummary[]>("/models/names", { authenticated: false }), []),
    safe(
      backendFetch<PageResponse<DiscussionDto>>("/discussions?size=200", { authenticated: false }),
      { content: [], page: 0, size: 0, totalElements: 0, totalPages: 0 },
    ),
  ]);

  const staticRoutes: MetadataRoute.Sitemap = [
    { url: `${SITE_URL}/`, changeFrequency: "daily", priority: 1 },
    { url: `${SITE_URL}/categories`, changeFrequency: "weekly", priority: 0.8 },
    { url: `${SITE_URL}/leaderboard`, changeFrequency: "daily", priority: 0.7 },
    { url: `${SITE_URL}/compare`, changeFrequency: "monthly", priority: 0.5 },
    { url: `${SITE_URL}/community`, changeFrequency: "hourly", priority: 0.6 },
  ];

  return [
    ...staticRoutes,
    ...categories.map((c) => ({
      url: `${SITE_URL}/categories/${c.slug}`,
      changeFrequency: "weekly" as const,
      priority: 0.7,
    })),
    ...models.map((m) => ({
      url: `${SITE_URL}/models/${m.slug}`,
      changeFrequency: "weekly" as const,
      priority: 0.9,
    })),
    ...discussions.content.map((d) => ({
      url: `${SITE_URL}/community/${d.id}`,
      changeFrequency: "weekly" as const,
      priority: 0.4,
    })),
  ];
}
