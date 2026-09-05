import { Star } from "lucide-react";
import { cn } from "@/lib/utils";
import { toNumber } from "@/lib/format";

interface StarRatingProps {
  value: string | number | null;
  /** Show the numeric value next to the stars. */
  showValue?: boolean;
  reviewCount?: number;
  size?: "sm" | "md";
  className?: string;
}

export function StarRating({
  value,
  showValue = true,
  reviewCount,
  size = "sm",
  className,
}: StarRatingProps) {
  const rating = toNumber(value);
  const px = size === "sm" ? "size-3.5" : "size-4";

  if (rating === null) {
    return (
      <span className={cn("text-xs text-muted-foreground", className)}>Not yet rated</span>
    );
  }

  return (
    <span className={cn("inline-flex items-center gap-1", className)}>
      <span className="inline-flex" aria-hidden>
        {[0, 1, 2, 3, 4].map((i) => (
          <Star
            key={i}
            className={cn(
              px,
              i + 1 <= Math.round(rating)
                ? "fill-amber-400 text-amber-400"
                : "fill-muted text-muted",
            )}
          />
        ))}
      </span>
      {showValue && (
        <span className="text-xs font-medium">
          {rating.toFixed(1)}
          {typeof reviewCount === "number" && (
            <span className="ml-1 font-normal text-muted-foreground">
              ({reviewCount})
            </span>
          )}
        </span>
      )}
      <span className="sr-only">{rating.toFixed(1)} out of 5</span>
    </span>
  );
}
