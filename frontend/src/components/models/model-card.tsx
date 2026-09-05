import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { StarRating } from "@/components/models/star-rating";
import type { ModelCardDto } from "@/lib/api/types";

export function ModelCard({ model }: { model: ModelCardDto }) {
  return (
    <Link
      href={`/models/${model.slug}`}
      className="flex flex-col gap-2 rounded-xl bg-card p-4 text-sm ring-1 ring-foreground/10 transition-colors hover:ring-primary/40"
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="truncate font-heading font-medium">{model.name}</h3>
          {model.creator && (
            <p className="truncate text-xs text-muted-foreground">by {model.creator}</p>
          )}
        </div>
        <Badge variant="outline" className="shrink-0">
          {model.category.name}
        </Badge>
      </div>
      {model.description && (
        <p className="line-clamp-2 text-muted-foreground">{model.description}</p>
      )}
      <StarRating value={model.ratings.overall} reviewCount={model.ratings.reviewCount} />
    </Link>
  );
}
