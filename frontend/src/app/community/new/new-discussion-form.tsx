"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import type { DiscussionDto } from "@/lib/api/types";

export function NewDiscussionForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [tagInput, setTagInput] = useState("");
  const [tags, setTags] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  function addTag() {
    const t = tagInput.trim().toLowerCase();
    if (t && !tags.includes(t) && tags.length < 5) {
      setTags([...tags, t]);
    }
    setTagInput("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      const created = await apiFetch<DiscussionDto>("/discussions", {
        method: "POST",
        body: { title: title.trim(), content: content.trim(), tags },
      });
      toast.success("Discussion posted");
      router.push(`/community/${created.id}`);
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : "Could not post the discussion");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div className="grid gap-2">
        <Label htmlFor="d-title">Title</Label>
        <Input id="d-title" required maxLength={500} value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="d-content">Body</Label>
        <Textarea id="d-content" required rows={8} maxLength={20000} value={content} onChange={(e) => setContent(e.target.value)} />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="d-tags">Tags <span className="text-muted-foreground">(up to 5)</span></Label>
        <Input
          id="d-tags"
          value={tagInput}
          onChange={(e) => setTagInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === ",") {
              e.preventDefault();
              addTag();
            }
          }}
          onBlur={addTag}
          placeholder="Press Enter to add"
          disabled={tags.length >= 5}
        />
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {tags.map((tag) => (
              <Badge key={tag} variant="secondary" asChild>
                <button type="button" onClick={() => setTags(tags.filter((t) => t !== tag))}>
                  {tag} ✕
                </button>
              </Badge>
            ))}
          </div>
        )}
      </div>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <Button type="submit" disabled={pending} className="mt-2">
        {pending ? "Posting…" : "Post discussion"}
      </Button>
    </form>
  );
}
