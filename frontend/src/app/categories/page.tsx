import type { Metadata } from "next";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { getCategories } from "@/lib/api/catalog";
import { safe } from "@/lib/api/safe";
import type { CategoryDto } from "@/lib/api/types";

export const revalidate = 3600;

export const metadata: Metadata = {
  title: "Categories",
  alternates: { canonical: "/categories" },
  description:
    "Browse AI models by category — LLMs, image generation, speech, embeddings and more, each rated by the ModelMate community.",
};

export default async function CategoriesPage() {
  const categories = await safe(getCategories, [] as CategoryDto[]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <header className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight">Categories</h1>
        <p className="mt-2 text-muted-foreground">
          {categories.length} categories of AI models, reviewed and rated by people who build with them.
        </p>
      </header>

      {categories.length === 0 ? (
        <p className="text-muted-foreground">No categories yet.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {categories.map((category) => (
            <Link
              key={category.slug}
              href={`/categories/${category.slug}`}
              className="flex flex-col gap-3 rounded-xl bg-card p-5 ring-1 ring-foreground/10 transition-colors hover:ring-primary/40"
            >
                <div className="flex items-center justify-between gap-2">
                  <h2 className="font-heading text-lg font-medium">{category.name}</h2>
                  <Badge variant="secondary">{category.modelCount}</Badge>
                </div>
                {category.description && (
                  <p className="line-clamp-2 text-sm text-muted-foreground">
                    {category.description}
                  </p>
                )}
                {category.applications.length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {category.applications.slice(0, 3).map((app) => (
                      <Badge key={app} variant="outline">
                        {app}
                      </Badge>
                    ))}
                  </div>
                )}
              </Link>
          ))}
        </div>
      )}
    </div>
  );
}
