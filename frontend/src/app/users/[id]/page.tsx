import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { BackendError } from "@/lib/api/backend-fetch";
import { getPublicProfile, getPublicContributions } from "@/lib/api/profile";
import { ProfileHeader } from "@/components/profile/profile-header";
import { ContributionList } from "@/components/profile/contribution-list";

type Props = { params: Promise<{ id: string }> };

async function loadProfile(id: string) {
  try {
    return await getPublicProfile(id);
  } catch (err) {
    if (err instanceof BackendError && err.status === 404) notFound();
    throw err;
  }
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  const user = await loadProfile(id);
  return { title: `${user.firstName} ${user.lastName}`.trim() };
}

export default async function PublicProfilePage({ params }: Props) {
  const { id } = await params;
  const user = await loadProfile(id);
  const contributions = await getPublicContributions(id);

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <ProfileHeader user={user} />
      <section className="mt-8">
        <h2 className="mb-2 text-lg font-semibold">Contributions</h2>
        <ContributionList contributions={contributions.content} />
      </section>
    </div>
  );
}
