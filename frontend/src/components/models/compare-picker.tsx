"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

interface Option {
  name: string;
  slug: string;
}

export function ComparePicker({
  options,
  selected,
}: {
  options: Option[];
  selected: string[];
}) {
  const router = useRouter();
  const initial = [selected[0] ?? "", selected[1] ?? "", selected[2] ?? ""];
  const [slots, setSlots] = useState<string[]>(initial);

  function update(index: number, value: string) {
    const next = [...slots];
    next[index] = value;
    setSlots(next);
    const slugs = next.filter(Boolean);
    if (slugs.length >= 2) {
      router.replace(`/compare?slugs=${slugs.join(",")}`, { scroll: false });
    } else {
      router.replace("/compare", { scroll: false });
    }
  }

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {slots.map((value, i) => (
        <label key={i} className="text-sm">
          <span className="mb-1 block text-muted-foreground">
            Model {i + 1}
            {i === 2 && " (optional)"}
          </span>
          <select
            value={value}
            onChange={(e) => update(i, e.target.value)}
            className="h-9 w-full rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <option value="">Select a model…</option>
            {options
              .filter((o) => o.slug === value || !slots.includes(o.slug))
              .map((o) => (
                <option key={o.slug} value={o.slug}>
                  {o.name}
                </option>
              ))}
          </select>
        </label>
      ))}
    </div>
  );
}
