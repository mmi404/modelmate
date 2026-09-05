"use client";

import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { LogOut } from "lucide-react";
import { DropdownMenuItem } from "@/components/ui/dropdown-menu";

export function LogoutButton() {
  const router = useRouter();
  const [pending, startTransition] = useTransition();
  const [, setError] = useState<string | null>(null);

  function logout() {
    startTransition(async () => {
      try {
        await fetch("/api/logout", { method: "POST" });
        router.push("/");
        router.refresh();
      } catch {
        setError("Could not log out, try again.");
      }
    });
  }

  return (
    <DropdownMenuItem onSelect={(e) => e.preventDefault()} disabled={pending} onClick={logout}>
      <LogOut />
      {pending ? "Logging out…" : "Log out"}
    </DropdownMenuItem>
  );
}
