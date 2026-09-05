"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { apiFetch, ApiClientError } from "@/lib/api/client";

type Step = "email" | "code" | "password";

export function ForgotPasswordForm() {
  const router = useRouter();
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [ticket, setTicket] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  function fail(err: unknown, fallback: string) {
    setError(err instanceof ApiClientError ? err.message : fallback);
  }

  async function submitEmail(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await apiFetch("/auth/forgot-password", { method: "POST", body: { email } });
      toast.success("If that email is registered, a code is on its way.");
      setStep("code");
    } catch (err) {
      fail(err, "Could not send a reset code");
    } finally {
      setPending(false);
    }
  }

  async function submitCode(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!/^\d{6}$/.test(code)) {
      setError("Enter the 6-digit code from your email");
      return;
    }
    setPending(true);
    try {
      const { resetTicket } = await apiFetch<{ resetTicket: string }>("/auth/verify-reset-code", {
        method: "POST",
        body: { email, code },
      });
      setTicket(resetTicket);
      setStep("password");
    } catch (err) {
      fail(err, "That code is invalid or expired");
    } finally {
      setPending(false);
    }
  }

  async function submitPassword(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (password.length < 8) {
      setError("Password must be at least 8 characters");
      return;
    }
    if (password !== confirm) {
      setError("Passwords don't match");
      return;
    }
    setPending(true);
    try {
      await apiFetch("/auth/reset-password", {
        method: "POST",
        body: { resetTicket: ticket, newPassword: password },
      });
      toast.success("Password updated — you can log in now.");
      router.push("/login");
    } catch (err) {
      fail(err, "Could not reset your password");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="mt-6">
      <ol className="mb-5 flex items-center gap-2 text-xs text-muted-foreground">
        {(["email", "code", "password"] as Step[]).map((s, i) => (
          <li key={s} className="flex items-center gap-2">
            <span
              className={
                "flex size-5 items-center justify-center rounded-full border " +
                (step === s
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border")
              }
            >
              {i + 1}
            </span>
            {i < 2 && <span className="h-px w-6 bg-border" />}
          </li>
        ))}
      </ol>

      {step === "email" && (
        <form onSubmit={submitEmail} className="flex flex-col gap-4">
          <div className="grid gap-2">
            <Label htmlFor="fp-email">Email</Label>
            <Input
              id="fp-email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(ev) => setEmail(ev.target.value)}
            />
          </div>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <Button type="submit" disabled={pending}>
            {pending ? "Sending…" : "Send code"}
          </Button>
        </form>
      )}

      {step === "code" && (
        <form onSubmit={submitCode} className="flex flex-col gap-4">
          <div className="grid gap-2">
            <Label htmlFor="fp-code">6-digit code</Label>
            <Input
              id="fp-code"
              inputMode="numeric"
              maxLength={6}
              required
              value={code}
              onChange={(ev) => setCode(ev.target.value.replace(/\D/g, ""))}
            />
          </div>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <Button type="submit" disabled={pending}>
            {pending ? "Verifying…" : "Verify code"}
          </Button>
          <button
            type="button"
            className="text-xs text-muted-foreground hover:text-foreground"
            onClick={() => { setStep("email"); setError(null); }}
          >
            Use a different email
          </button>
        </form>
      )}

      {step === "password" && (
        <form onSubmit={submitPassword} className="flex flex-col gap-4">
          <div className="grid gap-2">
            <Label htmlFor="fp-pw">New password</Label>
            <Input
              id="fp-pw"
              type="password"
              required
              autoComplete="new-password"
              value={password}
              onChange={(ev) => setPassword(ev.target.value)}
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="fp-pw2">Confirm password</Label>
            <Input
              id="fp-pw2"
              type="password"
              required
              autoComplete="new-password"
              value={confirm}
              onChange={(ev) => setConfirm(ev.target.value)}
            />
          </div>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <Button type="submit" disabled={pending}>
            {pending ? "Saving…" : "Set new password"}
          </Button>
        </form>
      )}
    </div>
  );
}
