import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <Skeleton className="h-4 w-40" />
      <Skeleton className="mt-4 h-8 w-3/4" />
      <Skeleton className="mt-2 h-4 w-40" />
      <Skeleton className="mt-6 h-32 rounded-lg" />
      <div className="mt-8 space-y-4">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-20 rounded-lg" />
        ))}
      </div>
    </div>
  );
}
