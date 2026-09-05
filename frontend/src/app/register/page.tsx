import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/get-current-user";
import { RegisterForm } from "./register-form";

export const metadata: Metadata = { title: "Register", alternates: { canonical: "/register" } };

export default async function RegisterPage() {
  const user = await getCurrentUser();
  if (user) redirect("/");

  return (
    <div className="mx-auto grid min-h-[calc(100vh-var(--spacing-navbar)-var(--spacing-footer))] max-w-5xl grid-cols-1 items-center gap-10 px-4 py-12 md:grid-cols-2">
      <div className="hidden md:block">
        <h1 className="text-3xl font-bold">Join ModelMate</h1>
        <p className="mt-3 text-muted-foreground">
          Create an account to post reviews, report problems, and join the discussion.
        </p>
      </div>
      <div className="card-shadow rounded-lg border border-border bg-card p-6">
        <h2 className="mb-6 text-xl font-semibold">Create Account</h2>
        <RegisterForm />
      </div>
    </div>
  );
}
