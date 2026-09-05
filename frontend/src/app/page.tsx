import Link from "next/link";
import { Button } from "@/components/ui/button";
import { backendFetch } from "@/lib/api/backend-fetch";
import { ModelCard } from "@/components/models/model-card";
import type { CategoryDto, ModelCardDto } from "@/lib/api/types";

// Public page (ADR-012): server-rendered and revalidated hourly for SEO/GEO.
export const revalidate = 3600;

function getCategories() {
  return backendFetch<CategoryDto[]>("/categories", {
    authenticated: false,
    next: { revalidate: 3600 },
  });
}

function getTrending() {
  return backendFetch<ModelCardDto[]>("/models/trending?limit=6", {
    authenticated: false,
    next: { revalidate: 900 },
  });
}

export default async function HomePage() {
  const [categories, trending] = await Promise.all([getCategories(), getTrending()]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <section className="mb-12">
        <h1 className="max-w-3xl text-3xl font-bold tracking-tight sm:text-4xl">
          Find the right AI model, backed by real experience.
        </h1>
        <p className="mt-3 max-w-2xl text-muted-foreground">
          ModelMate is a community-driven platform to review, rate, and compare AI models
          across every category — before you build on them.
        </p>
        <div className="mt-6 flex flex-wrap gap-3">
          <Button asChild>
            <Link href="/categories">Browse AI Models</Link>
          </Button>
          <Button variant="outline" asChild>
            <Link href="/submit-review">Write a Review</Link>
          </Button>
        </div>
      </section>

      {trending.length > 0 && (
        <section className="mb-12">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-xl font-semibold">Trending this month</h2>
            <Link href="/leaderboard" className="text-sm text-primary hover:underline">
              See leaderboard
            </Link>
          </div>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {trending.map((model) => (
              <ModelCard key={model.id} model={model} />
            ))}
          </div>
        </section>
      )}

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold">Popular Categories</h2>
          <Link href="/categories" className="text-sm text-primary hover:underline">
            View all
          </Link>
        </div>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {categories.map((category) => (
            <Link
              key={category.slug}
              href={`/categories/${category.slug}`}
              className="card-shadow rounded-lg border border-border bg-card p-5 transition-colors hover:border-primary"
            >
              <h3 className="font-semibold">{category.name}</h3>
              <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{category.description}</p>
              <p className="mt-3 text-xs text-muted-foreground">{category.modelCount} models</p>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}
