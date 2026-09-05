import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { VoteButtons } from "@/components/vote/vote-buttons";
import { HideReviewButton } from "@/components/admin/hide-review-button";
import { relativeTime } from "@/lib/format";
import { cn } from "@/lib/utils";
import type { ReviewDto, Severity } from "@/lib/api/types";

const SEVERITY_ORDER: Severity[] = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

const SEVERITY_STYLE: Record<Severity, string> = {
  CRITICAL: "bg-destructive/15 text-destructive",
  HIGH: "bg-orange-500/15 text-orange-400",
  MEDIUM: "bg-amber-500/15 text-amber-400",
  LOW: "bg-muted text-muted-foreground",
};

function initials(name: string) {
  return name.split(" ").map((p) => p[0]).filter(Boolean).slice(0, 2).join("").toUpperCase();
}

export function ProblemList({ problems }: { problems: ReviewDto[] }) {
  if (problems.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">No problems reported for this model.</p>
    );
  }

  const grouped = SEVERITY_ORDER.map((severity) => ({
    severity,
    items: problems.filter((p) => p.severity === severity),
  })).filter((g) => g.items.length > 0);

  const ungraded = problems.filter((p) => !p.severity || !SEVERITY_ORDER.includes(p.severity));
  if (ungraded.length > 0) grouped.push({ severity: "LOW", items: ungraded });

  return (
    <div className="space-y-3">
      {grouped.map(({ severity, items }, idx) => (
        <details key={`${severity}-${idx}`} open={idx === 0} className="group rounded-lg border border-border">
          <summary className="flex cursor-pointer items-center gap-2 px-4 py-3 text-sm font-medium">
            <Badge className={cn("border-0", SEVERITY_STYLE[severity])}>{severity}</Badge>
            <span className="text-muted-foreground">
              {items.length} {items.length === 1 ? "report" : "reports"}
            </span>
          </summary>
          <div className="border-t border-border px-4">
            {items.map((problem) => (
              <article key={problem.id} className="flex gap-3 border-b border-border py-4 last:border-b-0">
                <VoteButtons
                  targetType="REVIEW"
                  targetId={problem.id}
                  upvoteCount={problem.upvoteCount}
                  downvoteCount={problem.downvoteCount}
                  myVote={problem.myVote}
                />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                    <Avatar className="size-6">
                      <AvatarFallback className="text-[10px]">
                        {initials(problem.reviewer.name)}
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-sm font-medium">{problem.reviewer.name}</span>
                    <span className="text-xs text-muted-foreground">
                      {relativeTime(problem.createdAt)}
                    </span>
                  </div>
                  {problem.title && <h3 className="mt-2 font-medium">{problem.title}</h3>}
                  <p className="mt-1 text-sm whitespace-pre-line text-muted-foreground">
                    {problem.content}
                  </p>
                  <HideReviewButton reviewId={problem.id} />
                </div>
              </article>
            ))}
          </div>
        </details>
      ))}
    </div>
  );
}
