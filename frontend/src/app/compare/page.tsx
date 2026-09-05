import type { Metadata } from "next";
import Link from "next/link";
import { BackendError } from "@/lib/api/backend-fetch";
import { compareModels, getModelNames } from "@/lib/api/catalog";
import { safe } from "@/lib/api/safe";
import { ComparePicker } from "@/components/models/compare-picker";
import { StarRating } from "@/components/models/star-rating";
import type { ModelDetailDto } from "@/lib/api/types";

export const revalidate = 300;

export const metadata: Metadata = {
  title: "Compare AI models",
  alternates: { canonical: "/compare" },
  description: "Put 2–3 AI models side by side and compare their community ratings across accuracy, speed, cost, ease of use and reliability.",
};

type Props = { searchParams: Promise<{ slugs?: string }> };

function parseSlugs(raw: string | undefined): string[] {
  if (!raw) return [];
  return [...new Set(raw.split(",").map((s) => s.trim()).filter(Boolean))].slice(0, 3);
}

export default async function ComparePage({ searchParams }: Props) {
  const { slugs: rawSlugs } = await searchParams;
  const slugs = parseSlugs(rawSlugs);
  const names = await safe(getModelNames, [] as Awaited<ReturnType<typeof getModelNames>>);

  let models: ModelDetailDto[] = [];
  let error: string | null = null;
  if (slugs.length >= 2) {
    try {
      models = await compareModels(slugs);
    } catch (err) {
      error =
        err instanceof BackendError
          ? err.message
          : "Could not load the comparison.";
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <header className="mb-6">
        <h1 className="text-3xl font-bold tracking-tight">Compare models</h1>
        <p className="mt-2 text-muted-foreground">
          Pick 2 or 3 models to see their ratings side by side.
        </p>
      </header>

      <div className="mb-8 rounded-lg border border-border p-4">
        <ComparePicker options={names} selected={slugs} />
      </div>

      {error && (
        <p className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </p>
      )}

      {!error && slugs.length < 2 && (
        <p className="text-muted-foreground">Select at least two models above to compare them.</p>
      )}

      {!error && models.length >= 2 && (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm">
            <thead>
              <tr>
                <th className="w-40 p-3 text-left font-medium text-muted-foreground">Criteria</th>
                {models.map((m) => (
                  <th key={m.id} className="p-3 text-left align-bottom">
                    <Link href={`/models/${m.slug}`} className="font-heading text-base font-semibold hover:underline">
                      {m.name}
                    </Link>
                    {m.creator && (
                      <span className="block text-xs font-normal text-muted-foreground">
                        {m.creator}
                      </span>
                    )}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              <tr className="border-t border-border">
                <td className="p-3 text-muted-foreground">Overall</td>
                {models.map((m) => (
                  <td key={m.id} className="p-3">
                    <StarRating value={m.ratings.overall} reviewCount={m.ratings.reviewCount} />
                  </td>
                ))}
              </tr>
              {(["accuracy", "speed", "cost", "easeOfUse", "reliability"] as const).map((dim) => {
                const label = { accuracy: "Accuracy", speed: "Speed", cost: "Cost", easeOfUse: "Ease of use", reliability: "Reliability" }[dim];
                const best = Math.max(...models.map((m) => (typeof m.ratings[dim] === "number" ? (m.ratings[dim] as number) : -1)));
                return (
                  <tr key={dim} className="border-t border-border">
                    <td className="p-3 text-muted-foreground">{label}</td>
                    {models.map((m) => {
                      const v = typeof m.ratings[dim] === "number" ? (m.ratings[dim] as number) : null;
                      return (
                        <td key={m.id} className="p-3 tabular-nums">
                          {v === null ? (
                            "—"
                          ) : (
                            <span className={v === best && best > 0 ? "font-semibold text-primary" : ""}>
                              {v.toFixed(1)}
                            </span>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
              <tr className="border-t border-border">
                <td className="p-3 text-muted-foreground">Reviews</td>
                {models.map((m) => (
                  <td key={m.id} className="p-3 tabular-nums">{m.ratings.reviewCount}</td>
                ))}
              </tr>
              <tr className="border-t border-border">
                <td className="p-3 text-muted-foreground">Problems</td>
                {models.map((m) => (
                  <td key={m.id} className="p-3 tabular-nums">{m.problemCount}</td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
