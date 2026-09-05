import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/get-current-user";
import { getMyContributions } from "@/lib/api/profile";
import { ContributionList } from "@/components/profile/contribution-list";
import { ProfileForm } from "./profile-form";

export const metadata: Metadata = { title: "Your profile", robots: { index: false } };

export default async function ProfilePage() {
  const user = await getCurrentUser();
  if (!user) redirect("/login?next=/profile");
  const contributions = await getMyContributions();

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-3xl font-bold tracking-tight">Your profile</h1>
      <div className="mt-8 rounded-lg border border-border bg-card p-6">
        <ProfileForm user={user} />
      </div>
      <section className="mt-8">
        <h2 className="mb-2 text-lg font-semibold">Your contributions</h2>
        <ContributionList contributions={contributions.content} />
      </section>
    </div>
  );
}
