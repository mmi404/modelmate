import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Badge } from "@/components/ui/badge";
import { BackendError } from "@/lib/api/backend-fetch";
import { getDiscussion, getReplies } from "@/lib/api/community";
import { VoteButtons } from "@/components/vote/vote-buttons";
import { RepliesSection } from "@/components/community/replies-section";
import { relativeTime } from "@/lib/format";

type Props = { params: Promise<{ id: string }> };

async function loadDiscussion(id: string) {
  try {
    return await getDiscussion(id);
  } catch (err) {
    if (err instanceof BackendError && err.status === 404) notFound();
    throw err;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const discussion = await loadDiscussion(id);
  return {
    title: discussion.title,
    description: discussion.content.slice(0, 160),
  };
}

export default async function DiscussionPage({ params }: Props) {
  const { id } = await params;
  const discussion = await loadDiscussion(id);
  const replies = await getReplies(id);

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <nav className="mb-4 text-sm text-muted-foreground">
        <Link href="/community" className="hover:text-foreground">Community</Link>
        <span className="mx-2">/</span>
        <span className="text-foreground">Discussion</span>
      </nav>

      <article className="flex gap-3">
        <VoteButtons
          targetType="DISCUSSION"
          targetId={discussion.id}
          upvoteCount={discussion.upvoteCount}
          downvoteCount={discussion.downvoteCount}
          myVote={discussion.myVote}
        />
        <div className="min-w-0 flex-1">
          <h1 className="text-2xl font-bold tracking-tight">{discussion.title}</h1>
          <p className="mt-1 text-xs text-muted-foreground">
            {discussion.author.name} · {relativeTime(discussion.createdAt)}
          </p>
          <div className="mt-3 flex flex-wrap gap-1.5">
            {discussion.tags.map((tag) => (
              <Badge key={tag} variant="outline">{tag}</Badge>
            ))}
          </div>
          <p className="mt-4 whitespace-pre-line text-sm text-muted-foreground">{discussion.content}</p>
        </div>
      </article>

      <RepliesSection discussionId={discussion.id} initialReplies={replies} />
    </div>
  );
}
