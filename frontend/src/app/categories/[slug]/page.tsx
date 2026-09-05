import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { BackendError } from "@/lib/api/backend-fetch";
import { getCategory, getCategoryModels } from "@/lib/api/catalog";
import { ModelCard } from "@/components/models/model-card";
import { SortSelect } from "@/components/models/sort-select";

export const revalidate = 300;

const SORTS = new Set(["rating", "reviews", "newest", "name"]);

type Props = {
  params: Promise<{ slug: string }>;
  searchParams: Promise<{ sort?: string }>;
};

async function loadCategory(slug: string) {
  try {
    return await getCategory(slug);
  } catch (err) {
    if (err instanceof BackendError && err.status === 404) notFound();
    throw err;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const category = await loadCategory(slug);
  return {
    title: category.name,
    description:
      category.description ??
      `Community reviews and ratings for ${category.name} AI models on ModelMate.`,
  };
}

export default async function CategoryPage({ params, searchParams }: Props) {
  const { slug } = await params;
  const { sort: sortParam } = await searchParams;
  const sort = sortParam && SORTS.has(sortParam) ? sortParam : "rating";

  const [category, models] = await Promise.all([
    loadCategory(slug),
    getCategoryModels(slug, sort),
  ]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <nav className="mb-4 text-sm text-muted-foreground">
        <Link href="/categories" className="hover:text-foreground">
          Categories
        </Link>
        <span className="mx-2">/</span>
        <span className="text-foreground">{category.name}</span>
      </nav>

      <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{category.name}</h1>
          {category.description && (
            <p className="mt-2 max-w-2xl text-muted-foreground">{category.description}</p>
          )}
          <p className="mt-2 text-sm text-muted-foreground">
            {models.totalElements} {models.totalElements === 1 ? "model" : "models"}
          </p>
        </div>
        {models.content.length > 0 && <SortSelect value={sort} />}
      </header>

      {models.content.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border p-10 text-center">
          <p className="text-muted-foreground">
            No models in this category yet.{" "}
            <Link href="/submit-model" className="text-primary hover:underline">
              Submit one
            </Link>
            .
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {models.content.map((model) => (
            <ModelCard key={model.id} model={model} />
          ))}
        </div>
      )}
    </div>
  );
}
