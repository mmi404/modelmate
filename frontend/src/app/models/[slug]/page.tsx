import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ExternalLink } from "lucide-react";
import { BackendError } from "@/lib/api/backend-fetch";
import { getModel, getModelReviews, getModelProblems } from "@/lib/api/catalog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { StarRating } from "@/components/models/star-rating";
import { RatingBars } from "@/components/models/rating-bars";
import { ReviewItem } from "@/components/reviews/review-item";
import { ProblemList } from "@/components/reviews/problem-list";
import { formatDate } from "@/lib/format";

export const revalidate = 300;

type Props = { params: Promise<{ slug: string }> };

async function loadModel(slug: string) {
  try {
    return await getModel(slug);
  } catch (err) {
    if (err instanceof BackendError && err.status === 404) notFound();
    throw err;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const model = await loadModel(slug);
  const rating = model.ratings.overall ? `${model.ratings.overall}/5 from ${model.ratings.reviewCount} reviews — ` : "";
  return {
    title: `${model.name} reviews`,
    description: `${rating}${model.description ?? `Community reviews and ratings for ${model.name}.`}`.slice(0, 160),
  };
}

export default async function ModelPage({ params }: Props) {
  const { slug } = await params;
  const model = await loadModel(slug);
  const [reviews, problems] = await Promise.all([
    getModelReviews(model.id),
    getModelProblems(model.id),
  ]);

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <nav className="mb-4 text-sm text-muted-foreground">
        <Link href="/categories" className="hover:text-foreground">Categories</Link>
        <span className="mx-2">/</span>
        <Link href={`/categories/${model.category.slug}`} className="hover:text-foreground">
          {model.category.name}
        </Link>
        <span className="mx-2">/</span>
        <span className="text-foreground">{model.name}</span>
      </nav>

      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{model.name}</h1>
          {model.creator && (
            <p className="mt-1 text-muted-foreground">by {model.creator}</p>
          )}
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <StarRating
              value={model.ratings.overall}
              reviewCount={model.ratings.reviewCount}
              size="md"
            />
            {model.problemCount > 0 && (
              <Badge variant="destructive">{model.problemCount} problems reported</Badge>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          <Button asChild>
            <Link href={`/submit-review?model=${model.slug}`}>Write a review</Link>
          </Button>
          {model.websiteUrl && (
            <Button variant="outline" asChild>
              <a href={model.websiteUrl} target="_blank" rel="noopener noreferrer nofollow">
                Website <ExternalLink className="size-4" />
              </a>
            </Button>
          )}
        </div>
      </header>

      {model.description && (
        <p className="mt-6 max-w-2xl whitespace-pre-line text-muted-foreground">
          {model.description}
        </p>
      )}

      <div className="mt-8 grid gap-6 md:grid-cols-[2fr_1fr]">
        <div className="space-y-8">
          <section>
            <h2 className="mb-4 text-xl font-semibold">
              Reviews{" "}
              <span className="text-base font-normal text-muted-foreground">
                ({reviews.totalElements})
              </span>
            </h2>
            {reviews.content.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No reviews yet.{" "}
                <Link href={`/submit-review?model=${model.slug}`} className="text-primary hover:underline">
                  Be the first
                </Link>
                .
              </p>
            ) : (
              <div>
                {reviews.content.map((review) => (
                  <ReviewItem key={review.id} review={review} />
                ))}
              </div>
            )}
          </section>

          <section>
            <h2 className="mb-4 text-xl font-semibold">
              Problems{" "}
              <span className="text-base font-normal text-muted-foreground">
                ({problems.totalElements})
              </span>
            </h2>
            <ProblemList problems={problems.content} />
          </section>
        </div>

        <aside className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Rating breakdown</CardTitle>
            </CardHeader>
            <CardContent>
              {model.ratings.reviewCount === 0 ? (
                <p className="text-sm text-muted-foreground">No ratings yet.</p>
              ) : (
                <RatingBars ratings={model.ratings} />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Category</span>
                <Link href={`/categories/${model.category.slug}`} className="hover:underline">
                  {model.category.name}
                </Link>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Added</span>
                <span>{formatDate(model.createdAt)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Submitted by</span>
                <span>{model.submitter.name}</span>
              </div>
            </CardContent>
          </Card>

          <Button variant="outline" className="w-full" asChild>
            <Link href={`/compare?slugs=${model.slug}`}>Compare with another model</Link>
          </Button>
        </aside>
      </div>
    </div>
  );
}
