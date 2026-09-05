import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <Skeleton className="h-9 w-56" />
      <Skeleton className="mt-2 h-5 w-80" />
      <Skeleton className="mt-6 h-24 rounded-lg" />
      <Skeleton className="mt-8 h-72 rounded-lg" />
    </div>
  );
}
