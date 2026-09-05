/** Canonical origin for absolute URLs (metadata, sitemap, JSON-LD, OG images). */
export const SITE_URL = (
  process.env.NEXT_PUBLIC_SITE_URL ?? "https://modelmate.mmi404.com"
).replace(/\/$/, "");

export const SITE_NAME = "ModelMate";
export const SITE_DESCRIPTION =
  "Community-driven reviews, ratings, and comparisons of AI models across every category.";

export function absoluteUrl(path = "/"): string {
  return `${SITE_URL}${path.startsWith("/") ? path : `/${path}`}`;
}
