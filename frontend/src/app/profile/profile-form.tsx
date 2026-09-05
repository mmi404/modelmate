"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { apiFetch, ApiClientError } from "@/lib/api/client";
import type { UserDto } from "@/lib/api/types";

export function ProfileForm({ user }: { user: UserDto }) {
  const router = useRouter();
  const [firstName, setFirstName] = useState(user.firstName);
  const [lastName, setLastName] = useState(user.lastName);
  const [bio, setBio] = useState(user.bio ?? "");
  const [avatarUrl, setAvatarUrl] = useState(user.avatarUrl ?? "");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await apiFetch("/me", {
        method: "PUT",
        body: {
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          bio: bio.trim() || undefined,
          avatarUrl: avatarUrl.trim() || undefined,
        },
      });
      toast.success("Profile updated");
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiClientError ? err.message : "Could not update your profile");
    } finally {
      setPending(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-4">
        <div className="grid gap-2">
          <Label htmlFor="p-first">First name</Label>
          <Input id="p-first" required maxLength={100} value={firstName} onChange={(e) => setFirstName(e.target.value)} />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="p-last">Last name</Label>
          <Input id="p-last" required maxLength={100} value={lastName} onChange={(e) => setLastName(e.target.value)} />
        </div>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="p-email">Email</Label>
        <Input id="p-email" value={user.email ?? ""} disabled readOnly />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="p-avatar">Avatar URL <span className="text-muted-foreground">(optional)</span></Label>
        <Input id="p-avatar" type="url" maxLength={500} value={avatarUrl} onChange={(e) => setAvatarUrl(e.target.value)} />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="p-bio">Bio <span className="text-muted-foreground">(optional)</span></Label>
        <Textarea id="p-bio" rows={3} maxLength={500} value={bio} onChange={(e) => setBio(e.target.value)} />
      </div>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <Button type="submit" disabled={pending} className="self-start">
        {pending ? "Saving…" : "Save changes"}
      </Button>
    </form>
  );
}
