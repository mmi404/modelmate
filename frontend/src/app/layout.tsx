import type { Metadata } from "next";
import { Inter, Geist_Mono } from "next/font/google";
import { TooltipProvider } from "@/components/ui/tooltip";
import { Toaster } from "@/components/ui/sonner";
import { AuthProvider } from "@/components/providers/auth-provider";
import { QueryProvider } from "@/components/providers/query-provider";
import { AppShell } from "@/components/layout/app-shell";
import { getCurrentUser } from "@/lib/auth/get-current-user";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "ModelMate — Reviews & ratings for AI models",
    template: "%s — ModelMate",
  },
  description:
    "Community-driven reviews, ratings, and comparisons of AI models across every category.",
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  const user = await getCurrentUser();

  return (
    <html lang="en" className={`${inter.variable} ${geistMono.variable} h-full antialiased dark`}>
      <body className="min-h-full">
        <AuthProvider user={user}>
          <QueryProvider>
            <TooltipProvider delayDuration={200}>
              <AppShell>{children}</AppShell>
              <Toaster theme="dark" richColors position="bottom-right" />
            </TooltipProvider>
          </QueryProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
