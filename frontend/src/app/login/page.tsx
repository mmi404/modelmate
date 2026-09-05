import type { Metadata } from "next";
import { Suspense } from "react";
import { redirect } from "next/navigation";
import { getCurrentUser } from "@/lib/auth/get-current-user";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Log in", alternates: { canonical: "/login" } };

export default async function LoginPage() {
  const user = await getCurrentUser();
  if (user) redirect("/");

  return (
    <div className="mx-auto grid min-h-[calc(100vh-var(--spacing-navbar)-var(--spacing-footer))] max-w-5xl grid-cols-1 items-center gap-10 px-4 py-12 md:grid-cols-2">
      <div className="hidden md:block">
        <h1 className="text-3xl font-bold">Welcome back</h1>
        <p className="mt-3 text-muted-foreground">
          Sign in to review, rate, and compare AI models with the ModelMate community.
        </p>
      </div>
      <div className="card-shadow rounded-lg border border-border bg-card p-6">
        <h2 className="mb-6 text-xl font-semibold">Welcome Back</h2>
        <Suspense fallback={null}>
          <LoginForm />
        </Suspense>
      </div>
    </div>
  );
}
