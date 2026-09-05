import { backendFetch } from "@/lib/api/backend-fetch";
import { SITE_URL, SITE_NAME, SITE_DESCRIPTION } from "@/lib/site";
import type { CategoryDto } from "@/lib/api/types";

export const revalidate = 3600;

export async function GET() {
  let categories: CategoryDto[] = [];
  try {
    categories = await backendFetch<CategoryDto[]>("/categories", { authenticated: false });
  } catch {
    // fall through with an empty list
  }

  const body = `# ${SITE_NAME}

> ${SITE_DESCRIPTION}

${SITE_NAME} collects first-hand reviews, five-criteria ratings (accuracy, speed,
cost, ease of use, reliability), problem reports, and community discussion for AI
models. All public data is server-rendered and also available unauthenticated
via the JSON API at ${SITE_URL}/api/v1.

## Key pages

- [Categories](${SITE_URL}/categories): all model categories
- [Leaderboard](${SITE_URL}/leaderboard): highest-rated models
- [Compare](${SITE_URL}/compare): side-by-side model comparison
- [Community](${SITE_URL}/community): discussions

## Categories

${categories.map((c) => `- [${c.name}](${SITE_URL}/categories/${c.slug}): ${c.description ?? ""}`).join("\n")}

## API (read-only, no auth)

- ${SITE_URL}/api/v1/categories
- ${SITE_URL}/api/v1/models?category={slug}&sort=rating
- ${SITE_URL}/api/v1/models/{slug}
- ${SITE_URL}/api/v1/leaderboard
- ${SITE_URL}/api/v1/reviews/recent
- OpenAPI: ${SITE_URL}/api/v1/openapi

## Full index

- [llms-full.txt](${SITE_URL}/llms-full.txt): every category and model with current ratings
`;

  return new Response(body, {
    headers: { "content-type": "text/plain; charset=utf-8" },
  });
}
