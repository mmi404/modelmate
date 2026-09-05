"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { apiFetch, ApiClientError } from "@/lib/api/client";

export function SubmitModelForm({ categories }: { categories: { id: number; name: string }[] }) {
  const router = useRouter();
  const [name, setName] = useState("");
  const [creator, setCreator] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [websiteUrl, setWebsiteUrl] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!categoryId) {
      setError("Pick a category");
      return;
    }
    setPending(true);
    try {
      await apiFetch("/models", {
        method: "POST",
        body: {
          name,
          creator: creator || undefined,
          categoryId: Number(categoryId),
          description: description || undefined,
          websiteUrl,
        },
      });
      toast.success("Submitted — a moderator will review it shortly.");
      router.push("/categories");
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : "Could not submit the model");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div className="grid gap-2">
        <Label htmlFor="m-name">Model name</Label>
        <Input id="m-name" required maxLength={255} value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="m-creator">Creator / organisation <span className="text-muted-foreground">(optional)</span></Label>
        <Input id="m-creator" maxLength={255} value={creator} onChange={(e) => setCreator(e.target.value)} />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="m-category">Category</Label>
        <select
          id="m-category"
          required
          value={categoryId}
          onChange={(e) => setCategoryId(e.target.value)}
          className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <option value="">Select a category…</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="m-url">Website URL</Label>
        <Input
          id="m-url"
          type="url"
          required
          maxLength={500}
          placeholder="https://…"
          value={websiteUrl}
          onChange={(e) => setWebsiteUrl(e.target.value)}
        />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="m-desc">Description <span className="text-muted-foreground">(optional)</span></Label>
        <Textarea
          id="m-desc"
          rows={4}
          maxLength={5000}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <Button type="submit" disabled={pending} className="mt-2">
        {pending ? "Submitting…" : "Submit for review"}
      </Button>
    </form>
  );
}
