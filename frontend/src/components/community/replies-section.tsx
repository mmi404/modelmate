"use client";

import { useMemo, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import { toast } from "sonner";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { VoteButtons } from "@/components/vote/vote-buttons";
import { relativeTime } from "@/lib/format";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import { useAuth } from "@/components/providers/auth-provider";
import type { ReplyDto } from "@/lib/api/types";

function initials(name: string) {
  return name.split(" ").map((p) => p[0]).filter(Boolean).slice(0, 2).join("").toUpperCase();
}

export function RepliesSection({
  discussionId,
  initialReplies,
}: {
  discussionId: number;
  initialReplies: ReplyDto[];
}) {
  const user = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [replies, setReplies] = useState(initialReplies);
  const [replyingTo, setReplyingTo] = useState<number | null>(null);

  const { roots, childrenOf } = useMemo(() => {
    const roots = replies.filter((r) => r.parentReplyId === null);
    const childrenOf = new Map<number, ReplyDto[]>();
    for (const r of replies) {
      if (r.parentReplyId !== null) {
        const list = childrenOf.get(r.parentReplyId) ?? [];
        list.push(r);
        childrenOf.set(r.parentReplyId, list);
      }
    }
    return { roots, childrenOf };
  }, [replies]);

  async function submitReply(content: string, parentReplyId: number | null) {
    if (!user) {
      toast.error("Log in to reply", {
        action: { label: "Log in", onClick: () => router.push(`/login?next=${pathname}`) },
      });
      return false;
    }
    try {
      const created = await apiFetch<ReplyDto>(`/discussions/${discussionId}/replies`, {
        method: "POST",
        body: { content, parentReplyId: parentReplyId ?? undefined },
      });
      setReplies((prev) => [...prev, created]);
      setReplyingTo(null);
      toast.success("Reply posted");
      return true;
    } catch (err) {
      toast.error(err instanceof ApiClientError ? err.message : "Could not post your reply");
      return false;
    }
  }

  return (
    <section className="mt-8">
      <h2 className="mb-4 text-lg font-semibold">
        {replies.length} {replies.length === 1 ? "reply" : "replies"}
      </h2>

      <ReplyForm onSubmit={(c) => submitReply(c, null)} placeholder="Add a reply…" />

      <div className="mt-6 space-y-6">
        {roots.map((reply) => (
          <div key={reply.id}>
            <ReplyRow
              reply={reply}
              onReplyClick={() => setReplyingTo(replyingTo === reply.id ? null : reply.id)}
            />
            {replyingTo === reply.id && (
              <div className="mt-3 ml-11">
                <ReplyForm
                  autoFocus
                  placeholder={`Reply to ${reply.author.name}…`}
                  onSubmit={(c) => submitReply(c, reply.id)}
                />
              </div>
            )}
            {(childrenOf.get(reply.id) ?? []).length > 0 && (
              <div className="mt-4 ml-11 space-y-4 border-l border-border pl-4">
                {(childrenOf.get(reply.id) ?? []).map((child) => (
                  <ReplyRow key={child.id} reply={child} />
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

function ReplyRow({ reply, onReplyClick }: { reply: ReplyDto; onReplyClick?: () => void }) {
  return (
    <article className="flex gap-3">
      <VoteButtons
        targetType="REPLY"
        targetId={reply.id}
        upvoteCount={reply.upvoteCount}
        downvoteCount={reply.downvoteCount}
        myVote={reply.myVote}
      />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <Avatar className="size-6">
            <AvatarFallback className="text-[10px]">{initials(reply.author.name)}</AvatarFallback>
          </Avatar>
          <span className="text-sm font-medium">{reply.author.name}</span>
          <span className="text-xs text-muted-foreground">{relativeTime(reply.createdAt)}</span>
        </div>
        <p className="mt-1 text-sm whitespace-pre-line text-muted-foreground">{reply.content}</p>
        {onReplyClick && (
          <button
            type="button"
            onClick={onReplyClick}
            className="mt-1 text-xs font-medium text-muted-foreground hover:text-foreground"
          >
            Reply
          </button>
        )}
      </div>
    </article>
  );
}

function ReplyForm({
  onSubmit,
  placeholder,
  autoFocus,
}: {
  onSubmit: (content: string) => Promise<boolean>;
  placeholder: string;
  autoFocus?: boolean;
}) {
  const [value, setValue] = useState("");
  const [pending, setPending] = useState(false);

  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (!value.trim()) return;
        setPending(true);
        const ok = await onSubmit(value.trim());
        setPending(false);
        if (ok) setValue("");
      }}
      className="flex flex-col gap-2"
    >
      <Textarea
        autoFocus={autoFocus}
        rows={3}
        maxLength={10000}
        placeholder={placeholder}
        value={value}
        onChange={(e) => setValue(e.target.value)}
      />
      <Button type="submit" size="sm" disabled={pending || !value.trim()} className="self-start">
        {pending ? "Posting…" : "Post reply"}
      </Button>
    </form>
  );
}
