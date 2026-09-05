import type { Metadata } from "next";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { getDiscussions, getDiscussionTags, getCommunityStats } from "@/lib/api/community";
import { safe } from "@/lib/api/safe";
import { DiscussionCard } from "@/components/community/discussion-card";
import { TagFilter } from "@/components/community/tag-filter";
import type { DiscussionDto, DiscussionStats, PageResponse, TagCountDto } from "@/lib/api/types";

const EMPTY_PAGE: PageResponse<DiscussionDto> = {
  content: [], page: 0, size: 0, totalElements: 0, totalPages: 0,
};
const EMPTY_STATS: DiscussionStats = { activeMembers: 0, totalDiscussions: 0, totalReplies: 0 };

export const metadata: Metadata = {
  title: "Community",
  alternates: { canonical: "/community" },
  description: "Discuss AI models with the ModelMate community — ask questions, share findings, compare notes.",
};

const SORTS = new Set(["newest", "active", "top"]);

type Props = { searchParams: Promise<{ tags?: string; sort?: string }> };

export default async function CommunityPage({ searchParams }: Props) {
  const { tags: tagsParam, sort: sortParam } = await searchParams;
  const tags = (tagsParam ?? "").split(",").filter(Boolean);
  const sort = sortParam && SORTS.has(sortParam) ? sortParam : "newest";

  const [discussions, tagCounts, stats] = await Promise.all([
    safe(() => getDiscussions({ tags, sort }), EMPTY_PAGE),
    safe(getDiscussionTags, [] as TagCountDto[]),
    safe(getCommunityStats, EMPTY_STATS),
  ]);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Community</h1>
          <p className="mt-2 text-muted-foreground">
            {stats.totalDiscussions} discussions · {stats.totalReplies} replies · {stats.activeMembers} members
          </p>
        </div>
        <Button asChild>
          <Link href="/community/new">Start a discussion</Link>
        </Button>
      </div>

      <div className="mt-8 grid gap-8 md:grid-cols-[1fr_16rem]">
        <div>
          <div className="mb-4 flex gap-2 text-sm">
            {(["newest", "active", "top"] as const).map((s) => (
              <Link
                key={s}
                href={`/community?sort=${s}${tags.length ? `&tags=${tags.join(",")}` : ""}`}
                className={
                  "rounded-md px-2.5 py-1 capitalize " +
                  (sort === s ? "bg-accent text-foreground" : "text-muted-foreground hover:text-foreground")
                }
              >
                {s}
              </Link>
            ))}
          </div>

          {discussions.content.length === 0 ? (
            <div className="rounded-lg border border-dashed border-border p-10 text-center text-muted-foreground">
              No discussions{tags.length ? " with those tags" : " yet"}.{" "}
              <Link href="/community/new" className="text-primary hover:underline">Start one</Link>.
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {discussions.content.map((d) => (
                <DiscussionCard key={d.id} discussion={d} />
              ))}
            </div>
          )}
        </div>

        <aside className="space-y-4">
          <div className="rounded-lg border border-border p-4">
            <h2 className="mb-3 text-sm font-semibold">Filter by tag</h2>
            <TagFilter tags={tagCounts} />
          </div>
        </aside>
      </div>
    </div>
  );
}
