"use client";

import { useState, useTransition } from "react";
import { usePathname, useRouter } from "next/navigation";
import { ChevronDown, ChevronUp } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/lib/utils";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import { useAuth } from "@/components/providers/auth-provider";
import type { VoteResult, VoteTargetType } from "@/lib/api/types";

interface VoteButtonsProps {
  targetType: VoteTargetType;
  targetId: number;
  upvoteCount: number;
  downvoteCount: number;
  myVote: number | null;
  orientation?: "vertical" | "horizontal";
}

export function VoteButtons({
  targetType,
  targetId,
  upvoteCount,
  downvoteCount,
  myVote,
  orientation = "vertical",
}: VoteButtonsProps) {
  const user = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [pending, startTransition] = useTransition();
  const [state, setState] = useState({ up: upvoteCount, down: downvoteCount, mine: myVote });

  const score = state.up - state.down;

  function vote(value: 1 | -1) {
    if (!user) {
      toast.error("Log in to vote", {
        action: { label: "Log in", onClick: () => router.push(`/login?next=${pathname}`) },
      });
      return;
    }
    if (pending) return;

    const next = state.mine === value ? null : value;
    startTransition(async () => {
      try {
        const result = next === null
          ? await apiFetch<VoteResult>("/votes", {
              method: "DELETE",
              body: { targetType, targetId },
            })
          : await apiFetch<VoteResult>("/votes", {
              method: "PUT",
              body: { targetType, targetId, value: next },
            });
        setState({ up: result.upvoteCount, down: result.downvoteCount, mine: result.myVote });
      } catch (err) {
        const msg = err instanceof ApiClientError ? err.message : "Could not record your vote";
        toast.error(msg);
      }
    });
  }

  return (
    <div
      className={cn(
        "flex items-center gap-1",
        orientation === "vertical" ? "flex-col" : "flex-row",
      )}
    >
      <button
        type="button"
        onClick={() => vote(1)}
        disabled={pending}
        aria-pressed={state.mine === 1}
        aria-label="Upvote"
        className={cn(
          "rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50",
          state.mine === 1 && "text-primary",
        )}
      >
        <ChevronUp className="size-5" />
      </button>
      <span className="text-sm font-medium tabular-nums" aria-live="polite">
        {score}
      </span>
      <button
        type="button"
        onClick={() => vote(-1)}
        disabled={pending}
        aria-pressed={state.mine === -1}
        aria-label="Downvote"
        className={cn(
          "rounded-md p-1 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:opacity-50",
          state.mine === -1 && "text-destructive",
        )}
      >
        <ChevronDown className="size-5" />
      </button>
    </div>
  );
}
