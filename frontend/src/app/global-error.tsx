"use client";

import { useEffect } from "react";

/**
 * Catches failures in the root layout itself, where `error.tsx` cannot run.
 * Must render its own <html>/<body>.
 */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Root layout error:", error);
  }, [error]);

  return (
    <html lang="en">
      <body
        style={{
          background: "#0d0d0d",
          color: "#f9fafb",
          fontFamily: "system-ui, sans-serif",
          display: "flex",
          minHeight: "100vh",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: "1rem",
          textAlign: "center",
          padding: "2rem",
        }}
      >
        <h1 style={{ fontSize: "1.5rem", fontWeight: 600 }}>ModelMate is temporarily unavailable</h1>
        <p style={{ color: "#9ca3af", maxWidth: "32rem" }}>
          Something failed while starting the page. Please try again shortly.
        </p>
        <button
          onClick={reset}
          style={{
            background: "#4f46e5",
            color: "#f9fafb",
            border: 0,
            borderRadius: "0.5rem",
            padding: "0.6rem 1.2rem",
            cursor: "pointer",
          }}
        >
          Try again
        </button>
      </body>
    </html>
  );
}
