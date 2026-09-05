import { SITE_URL, SITE_NAME, SITE_DESCRIPTION } from "@/lib/site";
import type { ModelDetailDto, ReviewDto } from "@/lib/api/types";
import { toNumber } from "@/lib/format";

export function organizationLd(): Record<string, unknown> {
  return {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: SITE_NAME,
    url: SITE_URL,
    description: SITE_DESCRIPTION,
  };
}

export function websiteLd(): Record<string, unknown> {
  return {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: SITE_NAME,
    url: SITE_URL,
    description: SITE_DESCRIPTION,
    potentialAction: {
      "@type": "SearchAction",
      target: {
        "@type": "EntryPoint",
        urlTemplate: `${SITE_URL}/categories?q={search_term_string}`,
      },
      "query-input": "required name=search_term_string",
    },
  };
}

export function breadcrumbLd(items: { name: string; path: string }[]): Record<string, unknown> {
  return {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, i) => ({
      "@type": "ListItem",
      position: i + 1,
      name: item.name,
      item: `${SITE_URL}${item.path}`,
    })),
  };
}

export function modelLd(model: ModelDetailDto, reviews: ReviewDto[]): Record<string, unknown> {
  const rating = toNumber(model.ratings.overall);
  const data: Record<string, unknown> = {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: model.name,
    applicationCategory: model.category.name,
    url: `${SITE_URL}/models/${model.slug}`,
    ...(model.description ? { description: model.description } : {}),
    ...(model.creator ? { author: { "@type": "Organization", name: model.creator } } : {}),
    offers: { "@type": "Offer", price: "0", priceCurrency: "USD" },
  };

  if (rating !== null && model.ratings.reviewCount > 0) {
    data.aggregateRating = {
      "@type": "AggregateRating",
      ratingValue: rating.toFixed(2),
      reviewCount: model.ratings.reviewCount,
      bestRating: 5,
      worstRating: 1,
    };
  }

  const published = reviews
    .filter((r) => r.type === "REVIEW")
    .slice(0, 10)
    .map((r) => ({
      "@type": "Review",
      reviewBody: r.content,
      ...(r.title ? { name: r.title } : {}),
      datePublished: r.createdAt,
      author: { "@type": "Person", name: r.reviewer.name },
      ...(toNumber(r.overallRating) !== null
        ? {
            reviewRating: {
              "@type": "Rating",
              ratingValue: toNumber(r.overallRating),
              bestRating: 5,
              worstRating: 1,
            },
          }
        : {}),
    }));
  if (published.length > 0) {
    data.review = published;
  }

  return data;
}
