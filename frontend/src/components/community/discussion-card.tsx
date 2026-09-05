import Link from "next/link";
import { MessageSquare } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { relativeTime } from "@/lib/format";
import type { DiscussionDto } from "@/lib/api/types";

export function DiscussionCard({ discussion }: { discussion: DiscussionDto }) {
  const score = discussion.upvoteCount - discussion.downvoteCount;
  return (
    <Link
      href={`/community/${discussion.id}`}
      className="flex flex-col gap-2 rounded-xl bg-card p-4 ring-1 ring-foreground/10 transition-colors hover:ring-primary/40"
    >
      <h3 className="font-heading font-medium">{discussion.title}</h3>
      <p className="line-clamp-2 text-sm text-muted-foreground">{discussion.content}</p>
      <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
        {discussion.tags.map((tag) => (
          <Badge key={tag} variant="outline">{tag}</Badge>
        ))}
        <span className="ml-auto flex items-center gap-3">
          <span>{score} points</span>
          <span className="flex items-center gap-1">
            <MessageSquare className="size-3.5" />
            {discussion.replyCount}
          </span>
          <span>{discussion.author.name}</span>
          <span>{relativeTime(discussion.createdAt)}</span>
        </span>
      </div>
    </Link>
  );
}
