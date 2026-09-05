import { cn } from "@/lib/utils";
import type { RatingSummary } from "@/lib/api/types";

const DIMENSIONS: { key: keyof RatingSummary; label: string }[] = [
  { key: "accuracy", label: "Accuracy" },
  { key: "speed", label: "Speed" },
  { key: "cost", label: "Cost" },
  { key: "easeOfUse", label: "Ease of use" },
  { key: "reliability", label: "Reliability" },
];

export function RatingBars({
  ratings,
  className,
}: {
  ratings: RatingSummary;
  className?: string;
}) {
  return (
    <dl className={cn("grid gap-3", className)}>
      {DIMENSIONS.map(({ key, label }) => {
        const raw = ratings[key];
        const value = typeof raw === "number" ? raw : null;
        const pct = value === null ? 0 : (value / 5) * 100;
        return (
          <div key={key} className="grid grid-cols-[7rem_1fr_2.5rem] items-center gap-3">
            <dt className="text-sm text-muted-foreground">{label}</dt>
            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary transition-[width]"
                style={{ width: `${pct}%` }}
              />
            </div>
            <dd className="text-right text-sm font-medium tabular-nums">
              {value === null ? "—" : value.toFixed(1)}
            </dd>
          </div>
        );
      })}
    </dl>
  );
}
