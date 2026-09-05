"use client";

import { useRouter, usePathname, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function CategoryFilter({
  categories,
  value,
  paramName = "category",
  allLabel = "All categories",
}: {
  categories: { slug: string; name: string }[];
  value: string;
  paramName?: string;
  allLabel?: string;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  function onChange(next: string) {
    const q = new URLSearchParams(params);
    if (next === "__all") q.delete(paramName);
    else q.set(paramName, next);
    router.replace(`${pathname}?${q}`, { scroll: false });
  }

  return (
    <Select value={value || "__all"} onValueChange={onChange}>
      <SelectTrigger size="sm" aria-label="Filter by category">
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="__all">{allLabel}</SelectItem>
        {categories.map((c) => (
          <SelectItem key={c.slug} value={c.slug}>{c.name}</SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
