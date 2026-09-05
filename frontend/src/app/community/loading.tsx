import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <Skeleton className="h-9 w-48" />
      <Skeleton className="mt-2 h-5 w-72" />
      <div className="mt-8 grid gap-8 md:grid-cols-[1fr_16rem]">
        <div className="flex flex-col gap-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-28 rounded-xl" />
          ))}
        </div>
        <Skeleton className="h-40 rounded-lg" />
      </div>
    </div>
  );
}
