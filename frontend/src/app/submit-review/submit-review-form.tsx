"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import { StarInput } from "@/components/reviews/star-input";
import type { ReviewType, Severity } from "@/lib/api/types";

interface ModelOption {
  id: number;
  name: string;
  slug: string;
}

const DIMENSIONS = ["accuracy", "speed", "cost", "easeOfUse", "reliability"] as const;
const DIMENSION_LABELS: Record<(typeof DIMENSIONS)[number], string> = {
  accuracy: "Accuracy",
  speed: "Speed",
  cost: "Cost",
  easeOfUse: "Ease of use",
  reliability: "Reliability",
};
const SEVERITIES: Severity[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];

export function SubmitReviewForm({
  models,
  initialSlug,
  initialType,
}: {
  models: ModelOption[];
  initialSlug: string;
  initialType: ReviewType;
}) {
  const router = useRouter();
  const initialModel = useMemo(
    () => models.find((m) => m.slug === initialSlug),
    [models, initialSlug],
  );

  const [modelId, setModelId] = useState<string>(initialModel ? String(initialModel.id) : "");
  const [type, setType] = useState<ReviewType>(initialType);
  const [ratings, setRatings] = useState<Record<string, number>>({});
  const [severity, setSeverity] = useState<Severity>("MEDIUM");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!modelId) {
      setError("Choose a model");
      return;
    }
    if (type === "REVIEW" && DIMENSIONS.some((d) => !ratings[d])) {
      setError("Rate all five criteria");
      return;
    }
    if (content.trim().length === 0) {
      setError("Add some detail in the description");
      return;
    }

    setPending(true);
    try {
      await apiFetch(`/models/${modelId}/reviews`, {
        method: "POST",
        body: {
          type,
          title: title.trim() || undefined,
          content: content.trim(),
          ratings:
            type === "REVIEW"
              ? {
                  accuracy: ratings.accuracy,
                  speed: ratings.speed,
                  cost: ratings.cost,
                  easeOfUse: ratings.easeOfUse,
                  reliability: ratings.reliability,
                }
              : undefined,
          severity: type === "PROBLEM" ? severity : undefined,
        },
      });
      const slug = models.find((m) => String(m.id) === modelId)?.slug;
      toast.success(type === "REVIEW" ? "Review posted" : "Problem reported");
      router.push(slug ? `/models/${slug}` : "/categories");
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : "Could not post your contribution");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5">
      <div className="grid gap-2">
        <Label htmlFor="r-model">Model</Label>
        <select
          id="r-model"
          required
          value={modelId}
          onChange={(e) => setModelId(e.target.value)}
          className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <option value="">Select a model…</option>
          {models.map((m) => (
            <option key={m.id} value={m.id}>{m.name}</option>
          ))}
        </select>
      </div>

      <div className="grid gap-2">
        <span className="text-sm font-medium">Type</span>
        <div className="flex gap-2">
          {(["REVIEW", "PROBLEM"] as ReviewType[]).map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setType(t)}
              className={cn(
                "flex-1 rounded-lg border px-3 py-2 text-sm transition-colors",
                type === t
                  ? "border-primary bg-primary/10 text-foreground"
                  : "border-border text-muted-foreground hover:text-foreground",
              )}
            >
              {t === "REVIEW" ? "Review & rate" : "Report a problem"}
            </button>
          ))}
        </div>
      </div>

      {type === "REVIEW" ? (
        <fieldset className="grid gap-2 rounded-lg border border-border p-4">
          <legend className="px-1 text-sm font-medium">Ratings</legend>
          {DIMENSIONS.map((d) => (
            <StarInput
              key={d}
              label={DIMENSION_LABELS[d]}
              value={ratings[d] ?? 0}
              onChange={(v) => setRatings((prev) => ({ ...prev, [d]: v }))}
            />
          ))}
        </fieldset>
      ) : (
        <div className="grid gap-2">
          <Label htmlFor="r-severity">Severity</Label>
          <select
            id="r-severity"
            value={severity}
            onChange={(e) => setSeverity(e.target.value as Severity)}
            className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            {SEVERITIES.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      )}

      <div className="grid gap-2">
        <Label htmlFor="r-title">Title <span className="text-muted-foreground">(optional)</span></Label>
        <Input id="r-title" maxLength={255} value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>

      <div className="grid gap-2">
        <Label htmlFor="r-content">{type === "REVIEW" ? "Your review" : "What went wrong"}</Label>
        <Textarea
          id="r-content"
          required
          rows={5}
          maxLength={5000}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
      </div>

      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <Button type="submit" disabled={pending}>
        {pending ? "Posting…" : type === "REVIEW" ? "Post review" : "Report problem"}
      </Button>
    </form>
  );
}
