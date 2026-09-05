import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { relativeTime } from "@/lib/format";
import type { ContributionDto } from "@/lib/api/types";

const TYPE_LABEL: Record<ContributionDto["type"], string> = {
  REVIEW: "Review",
  PROBLEM: "Problem",
  DISCUSSION: "Discussion",
  REPLY: "Reply",
};

function href(c: ContributionDto): string {
  if (c.type === "DISCUSSION") return `/community/${c.id}`;
  if (c.type === "REPLY" && c.discussionId) return `/community/${c.discussionId}`;
  if (c.modelSlug) return `/models/${c.modelSlug}`;
  return "#";
}

export function ContributionList({ contributions }: { contributions: ContributionDto[] }) {
  if (contributions.length === 0) {
    return <p className="text-sm text-muted-foreground">No contributions yet.</p>;
  }

  return (
    <ul className="flex flex-col divide-y divide-border">
      {contributions.map((c) => (
        <li key={`${c.type}-${c.id}`} className="py-3">
          <Link href={href(c)} className="group flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <Badge variant="outline">{TYPE_LABEL[c.type]}</Badge>
              {c.severity && <Badge variant="destructive">{c.severity}</Badge>}
              <span className="text-xs text-muted-foreground">{relativeTime(c.createdAt)}</span>
            </div>
            {c.title && (
              <span className="text-sm font-medium group-hover:underline">{c.title}</span>
            )}
            {c.snippet && (
              <span className="line-clamp-2 text-sm text-muted-foreground">{c.snippet}</span>
            )}
          </Link>
        </li>
      ))}
    </ul>
  );
}
