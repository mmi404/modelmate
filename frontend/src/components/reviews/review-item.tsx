import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { StarRating } from "@/components/models/star-rating";
import { VoteButtons } from "@/components/vote/vote-buttons";
import { relativeTime } from "@/lib/format";
import type { ReviewDto } from "@/lib/api/types";

function initials(name: string) {
  return name
    .split(" ")
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

export function ReviewItem({ review }: { review: ReviewDto }) {
  return (
    <article className="flex gap-3 border-b border-border py-5 last:border-b-0">
      <VoteButtons
        targetType="REVIEW"
        targetId={review.id}
        upvoteCount={review.upvoteCount}
        downvoteCount={review.downvoteCount}
        myVote={review.myVote}
      />
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <Avatar className="size-6">
            <AvatarFallback className="text-[10px]">
              {initials(review.reviewer.name)}
            </AvatarFallback>
          </Avatar>
          <span className="text-sm font-medium">{review.reviewer.name}</span>
          <span className="text-xs text-muted-foreground">
            {relativeTime(review.createdAt)}
          </span>
          {review.overallRating && (
            <StarRating value={review.overallRating} showValue className="ml-auto" />
          )}
        </div>
        {review.title && <h3 className="mt-2 font-medium">{review.title}</h3>}
        <p className="mt-1 text-sm whitespace-pre-line text-muted-foreground">
          {review.content}
        </p>
      </div>
    </article>
  );
}
