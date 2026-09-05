"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { cn } from "@/lib/utils";
import type { TagCountDto } from "@/lib/api/types";

export function TagFilter({ tags }: { tags: TagCountDto[] }) {
  const router = useRouter();
  const params = useSearchParams();
  const active = new Set((params.get("tags") ?? "").split(",").filter(Boolean));

  function toggle(tag: string) {
    const next = new Set(active);
    if (next.has(tag)) next.delete(tag);
    else next.add(tag);
    const q = new URLSearchParams(params);
    if (next.size) q.set("tags", [...next].join(","));
    else q.delete("tags");
    router.replace(`/community?${q}`, { scroll: false });
  }

  if (tags.length === 0) return null;

  return (
    <div className="flex flex-wrap gap-1.5">
      {tags.map(({ tag, count }) => (
        <button
          key={tag}
          type="button"
          onClick={() => toggle(tag)}
          className={cn(
            "rounded-full border px-2.5 py-1 text-xs transition-colors",
            active.has(tag)
              ? "border-primary bg-primary/10 text-foreground"
              : "border-border text-muted-foreground hover:text-foreground",
          )}
        >
          {tag} <span className="text-muted-foreground">{count}</span>
        </button>
      ))}
    </div>
  );
}
