import type { Metadata } from "next";
import Link from "next/link";
import { getCategories } from "@/lib/api/catalog";
import { getLeaderboard } from "@/lib/api/leaderboard";
import { CategoryFilter } from "@/components/models/category-filter";
import { StarRating } from "@/components/models/star-rating";
import { cn } from "@/lib/utils";

export const revalidate = 300;

export const metadata: Metadata = {
  title: "Leaderboard",
  description: "The highest-rated AI models on ModelMate, ranked by community rating and review volume.",
};

type Props = { searchParams: Promise<{ category?: string }> };

export default async function LeaderboardPage({ searchParams }: Props) {
  const { category } = await searchParams;
  const [entries, categories] = await Promise.all([
    getLeaderboard(category, 1),
    getCategories(),
  ]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Leaderboard</h1>
          <p className="mt-2 text-muted-foreground">
            Ranked by average overall rating, then number of reviews.
          </p>
        </div>
        <CategoryFilter
          categories={categories.map((c) => ({ slug: c.slug, name: c.name }))}
          value={category ?? ""}
        />
      </div>

      {entries.length === 0 ? (
        <p className="mt-10 text-muted-foreground">No rated models yet in this view.</p>
      ) : (
        <ol className="mt-8 flex flex-col gap-2">
          {entries.map((e) => (
            <li
              key={e.modelId}
              className={cn(
                "flex items-center gap-4 rounded-xl bg-card p-4 ring-1 ring-foreground/10",
                e.topThree && "ring-primary/40",
              )}
            >
              <span
                className={cn(
                  "flex size-9 shrink-0 items-center justify-center rounded-full text-sm font-semibold",
                  e.topThree ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground",
                )}
              >
                {e.rank}
              </span>
              <div className="min-w-0 flex-1">
                <Link href={`/models/${e.slug}`} className="font-heading font-medium hover:underline">
                  {e.name}
                </Link>
                <p className="text-xs text-muted-foreground">
                  {e.creator ? `${e.creator} · ` : ""}
                  <Link href={`/categories/${e.categorySlug}`} className="hover:underline">
                    {e.categoryName}
                  </Link>
                </p>
              </div>
              <StarRating value={e.overall} reviewCount={e.reviewCount} />
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
