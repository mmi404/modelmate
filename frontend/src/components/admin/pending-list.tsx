"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import { relativeTime } from "@/lib/format";
import type { PendingModelDto } from "@/lib/api/types";

export function PendingList({ initial }: { initial: PendingModelDto[] }) {
  const router = useRouter();
  const [models, setModels] = useState(initial);
  const [busyId, setBusyId] = useState<number | null>(null);

  async function act(model: PendingModelDto, action: "approve" | "reject") {
    let reason = "";
    if (action === "reject") {
      reason = window.prompt(`Reject "${model.name}" — reason:`)?.trim() ?? "";
      if (!reason) return;
    }
    setBusyId(model.id);
    try {
      await apiFetch(`/admin/models/${model.id}/${action}`, {
        method: "POST",
        body: action === "reject" ? { reason } : undefined,
      });
      setModels((prev) => prev.filter((m) => m.id !== model.id));
      toast.success(action === "approve" ? "Model approved" : "Model rejected");
      router.refresh();
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : `Could not ${action} the model`);
    } finally {
      setBusyId(null);
    }
  }

  if (models.length === 0) {
    return <p className="text-sm text-muted-foreground">Nothing waiting for review.</p>;
  }

  return (
    <ul className="flex flex-col gap-3">
      {models.map((model) => (
        <li key={model.id} className="rounded-lg border border-border p-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="font-medium">
                {model.name}
                {model.creator && <span className="text-muted-foreground"> · {model.creator}</span>}
              </p>
              <p className="text-xs text-muted-foreground">
                {model.categoryName} · by {model.submitterName} · {relativeTime(model.submittedAt)}
              </p>
              {model.description && (
                <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">{model.description}</p>
              )}
              {model.websiteUrl && (
                <a
                  href={model.websiteUrl}
                  target="_blank"
                  rel="noopener noreferrer nofollow"
                  className="mt-1 inline-block text-xs text-primary hover:underline"
                >
                  {model.websiteUrl}
                </a>
              )}
            </div>
            <div className="flex gap-2">
              <Button size="sm" disabled={busyId === model.id} onClick={() => act(model, "approve")}>
                Approve
              </Button>
              <Button
                size="sm"
                variant="outline"
                disabled={busyId === model.id}
                onClick={() => act(model, "reject")}
              >
                Reject
              </Button>
            </div>
          </div>
        </li>
      ))}
    </ul>
  );
}
