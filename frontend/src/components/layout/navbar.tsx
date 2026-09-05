"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { useAuth } from "@/components/providers/auth-provider";
import { LogoutButton } from "@/components/layout/logout-button";
import { ADMIN_NAV, PRIMARY_NAV, SIDEBAR_NAV } from "@/components/layout/nav-links";
import { cn } from "@/lib/utils";

export function Navbar() {
  const user = useAuth();
  const pathname = usePathname();

  return (
    <header className="sticky top-0 z-40 flex h-(--spacing-navbar) shrink-0 items-center gap-2 border-b border-border bg-background px-4 md:gap-4 md:px-6">
      <Sheet>
        <SheetTrigger asChild>
          <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu">
            <Menu />
          </Button>
        </SheetTrigger>
        <SheetContent side="left" className="w-(--spacing-sidebar) bg-sidebar p-0 text-sidebar-foreground">
          <SheetHeader className="border-b border-sidebar-border">
            <SheetTitle>ModelMate</SheetTitle>
          </SheetHeader>
          <nav className="flex flex-col gap-1 p-3">
            {PRIMARY_NAV.map((item) => (
              <MobileNavLink key={item.href} href={item.href} active={pathname === item.href}>
                {item.label}
              </MobileNavLink>
            ))}
            {user && (
              <>
                <div className="my-2 border-t border-sidebar-border" />
                {SIDEBAR_NAV.map((item) => (
                  <MobileNavLink key={item.href} href={item.href} active={pathname === item.href}>
                    {item.label}
                  </MobileNavLink>
                ))}
                {user.role === "ADMIN" &&
                  ADMIN_NAV.map((item) => (
                    <MobileNavLink key={item.href} href={item.href} active={pathname === item.href}>
                      {item.label}
                    </MobileNavLink>
                  ))}
              </>
            )}
          </nav>
        </SheetContent>
      </Sheet>

      <Link href="/" className="shrink-0 text-lg font-bold tracking-tight">
        Model<span className="text-primary">Mate</span>
      </Link>

      <nav className="hidden items-center gap-1 md:flex">
        {PRIMARY_NAV.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "rounded-md px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground",
              pathname === item.href && "bg-accent text-foreground"
            )}
          >
            {item.label}
          </Link>
        ))}
      </nav>

      <div className="ml-auto flex flex-1 items-center justify-end gap-3">
        <div className="relative hidden max-w-sm flex-1 sm:block">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input placeholder="Search AI models..." className="pl-8" aria-label="Search AI models" />
        </div>

        {user ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="rounded-full" aria-label="Account menu">
                <Avatar className="size-8">
                  <AvatarFallback>{initials(user.firstName, user.lastName)}</AvatarFallback>
                </Avatar>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              <DropdownMenuLabel className="truncate">
                {user.firstName} {user.lastName}
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild>
                <Link href="/profile">Profile</Link>
              </DropdownMenuItem>
              {user.role === "ADMIN" && (
                <DropdownMenuItem asChild>
                  <Link href="/admin">Admin</Link>
                </DropdownMenuItem>
              )}
              <DropdownMenuSeparator />
              <LogoutButton />
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <div className="flex items-center gap-2">
            <Button variant="ghost" asChild>
              <Link href="/login">Log in</Link>
            </Button>
            <Button asChild>
              <Link href="/register">Register</Link>
            </Button>
          </div>
        )}
      </div>
    </header>
  );
}

function initials(firstName: string, lastName: string) {
  return `${firstName?.[0] ?? ""}${lastName?.[0] ?? ""}`.toUpperCase();
}

function MobileNavLink({
  href,
  active,
  children,
}: {
  href: string;
  active: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "rounded-md px-3 py-2 text-sm font-medium text-sidebar-foreground/80 hover:bg-sidebar-accent hover:text-sidebar-foreground",
        active && "bg-sidebar-accent text-sidebar-foreground"
      )}
    >
      {children}
    </Link>
  );
}
