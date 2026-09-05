import { backendFetch } from "@/lib/api/backend-fetch";
import { SITE_URL, SITE_NAME, SITE_DESCRIPTION } from "@/lib/site";
import type { CategoryDto, ModelCardDto, PageResponse } from "@/lib/api/types";

export const revalidate = 3600;

export async function GET() {
  let categories: CategoryDto[] = [];
  try {
    categories = await backendFetch<CategoryDto[]>("/categories", { authenticated: false });
  } catch {
    return new Response(`# ${SITE_NAME}\n\n${SITE_DESCRIPTION}\n\n(model index temporarily unavailable)\n`, {
      headers: { "content-type": "text/plain; charset=utf-8" },
    });
  }

  const sections = await Promise.all(
    categories.map(async (c) => {
      let models: ModelCardDto[] = [];
      try {
        const page = await backendFetch<PageResponse<ModelCardDto>>(
          `/categories/${c.slug}/models?sort=rating`,
          { authenticated: false },
        );
        models = page.content;
      } catch {
        // leave this category's list empty
      }
      const rows = models
        .map((m) => {
          const rating = m.ratings.overall
            ? `${m.ratings.overall}/5 (${m.ratings.reviewCount} reviews)`
            : "not yet rated";
          return `- [${m.name}](${SITE_URL}/models/${m.slug})${m.creator ? ` by ${m.creator}` : ""} — ${rating}${
            m.description ? `. ${m.description}` : ""
          }`;
        })
        .join("\n");
      return `## ${c.name}\n\n${c.description ?? ""}\n\n${rows || "_No models yet._"}\n`;
    }),
  );

  const body = `# ${SITE_NAME} — full model index

> ${SITE_DESCRIPTION}

Generated from live data. Ratings are the mean of community reviews across five
criteria. Canonical pages and the JSON API live at ${SITE_URL}.

${sections.join("\n")}`;

  return new Response(body, {
    headers: { "content-type": "text/plain; charset=utf-8" },
  });
}
