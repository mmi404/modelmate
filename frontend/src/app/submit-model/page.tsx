import type { Metadata } from "next";
import { getCategories } from "@/lib/api/catalog";
import { SubmitModelForm } from "./submit-model-form";

export const metadata: Metadata = {
  title: "Submit a model",
  robots: { index: false },
  description: "Add an AI model to ModelMate for the community to review and rate.",
};

export default async function SubmitModelPage() {
  const categories = await getCategories();

  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="text-3xl font-bold tracking-tight">Submit a model</h1>
      <p className="mt-2 text-muted-foreground">
        New submissions are reviewed by a moderator before they appear in the catalog.
      </p>
      <div className="mt-8 rounded-lg border border-border bg-card p-6">
        <SubmitModelForm
          categories={categories.map((c) => ({ id: c.id, name: c.name }))}
        />
      </div>
    </div>
  );
}
