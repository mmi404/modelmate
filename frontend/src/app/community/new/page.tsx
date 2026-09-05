import type { Metadata } from "next";
import { NewDiscussionForm } from "./new-discussion-form";

export const metadata: Metadata = { title: "Start a discussion" };

export default function NewDiscussionPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-10">
      <h1 className="text-3xl font-bold tracking-tight">Start a discussion</h1>
      <p className="mt-2 text-muted-foreground">
        Ask a question or share something you&apos;ve learned about an AI model.
      </p>
      <div className="mt-8 rounded-lg border border-border bg-card p-6">
        <NewDiscussionForm />
      </div>
    </div>
  );
}
