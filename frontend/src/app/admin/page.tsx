import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/get-current-user";
import { getAdminStats, getPendingModels } from "@/lib/api/admin";
import { PendingList } from "@/components/admin/pending-list";

export const metadata: Metadata = { title: "Admin" };

const STAT_LABELS: Record<string, string> = {
  totalUsers: "Users",
  pendingModels: "Pending",
  approvedModels: "Approved",
  rejectedModels: "Rejected",
  totalReviews: "Reviews",
  totalProblems: "Problems",
  totalDiscussions: "Discussions",
};

export default async function AdminPage() {
  const user = await getCurrentUser();
  if (!user) redirect("/login?next=/admin");
  if (user.role !== "ADMIN") redirect("/");

  const [stats, pending] = await Promise.all([getAdminStats(), getPendingModels()]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold tracking-tight">Admin</h1>

      <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {Object.entries(STAT_LABELS).map(([key, label]) => (
          <div key={key} className="rounded-lg border border-border bg-card p-4">
            <p className="text-2xl font-semibold tabular-nums">
              {(stats as unknown as Record<string, number>)[key]}
            </p>
            <p className="text-xs text-muted-foreground">{label}</p>
          </div>
        ))}
      </div>

      <section className="mt-10">
        <h2 className="mb-3 text-lg font-semibold">
          Pending submissions ({pending.content.length})
        </h2>
        <PendingList initial={pending.content} />
      </section>
    </div>
  );
}
