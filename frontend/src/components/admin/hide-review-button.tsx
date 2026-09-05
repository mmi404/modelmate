"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import { useAuth } from "@/components/providers/auth-provider";

/**
 * Admin-only moderation control shown next to a review or problem report.
 * Self-gating: renders nothing unless the current user is an admin, so the
 * server components that embed it stay statically renderable.
 */
export function HideReviewButton({ reviewId }: { reviewId: number }) {
  const user = useAuth();
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [done, setDone] = useState(false);

  if (user?.role !== "ADMIN") return null;

  function hide() {
    if (!window.confirm("Hide this contribution from public view?")) return;
    startTransition(async () => {
      try {
        await apiFetch(`/admin/reviews/${reviewId}/hidden`, {
          method: "PATCH",
          body: { hidden: true },
        });
        setDone(true);
        toast.success("Hidden");
        router.refresh();
      } catch (err) {
        toast.error(err instanceof ApiClientError ? err.message : "Could not hide it");
      }
    });
  }

  return (
    <button
      type="button"
      onClick={hide}
      disabled={pending || done}
      className="mt-2 text-xs font-medium text-muted-foreground hover:text-destructive disabled:opacity-50"
    >
      {done ? "Hidden" : pending ? "Hiding…" : "Hide (admin)"}
    </button>
  );
}
