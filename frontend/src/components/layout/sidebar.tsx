"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/components/providers/auth-provider";
import { ADMIN_NAV, SIDEBAR_NAV } from "@/components/layout/nav-links";
import { cn } from "@/lib/utils";

/** Desktop-only, authenticated-only (see AppShell). Design spec: 250px, bg #1A1A1A. */
export function Sidebar() {
  const user = useAuth();
  const pathname = usePathname();
  if (!user) return null;

  const items = user.role === "ADMIN" ? [...SIDEBAR_NAV, ...ADMIN_NAV] : SIDEBAR_NAV;

  return (
    <aside className="hidden w-(--spacing-sidebar) shrink-0 border-r border-sidebar-border bg-sidebar md:block">
      <nav className="flex flex-col gap-1 p-4">
        {items.map((item) => {
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "rounded-md px-3 py-2 text-sm font-medium text-sidebar-foreground/80 transition-colors hover:bg-sidebar-accent hover:text-sidebar-foreground",
                active && "bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground"
              )}
            >
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
