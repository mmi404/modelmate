import type { Metadata } from "next";
import { getModelNames } from "@/lib/api/catalog";
import { SubmitReviewForm } from "./submit-review-form";

export const metadata: Metadata = {
  title: "Write a review",
  robots: { index: false },
  description: "Share your experience with an AI model — rate it or report a problem.",
};

type Props = { searchParams: Promise<{ model?: string; type?: string }> };

export default async function SubmitReviewPage({ searchParams }: Props) {
  const { model, type } = await searchParams;
  const names = await getModelNames();

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="text-3xl font-bold tracking-tight">Write a review</h1>
      <p className="mt-2 text-muted-foreground">
        Rate a model across five criteria, or report a specific problem.
      </p>
      <div className="mt-8 rounded-lg border border-border bg-card p-6">
        <SubmitReviewForm
          models={names.map((m) => ({ id: m.id, name: m.name, slug: m.slug }))}
          initialSlug={model ?? ""}
          initialType={type === "problem" ? "PROBLEM" : "REVIEW"}
        />
      </div>
    </div>
  );
}
