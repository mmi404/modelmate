/**
 * Run a data fetch that a public page needs to render, but that should not hard-fail
 * the page (or a `next build` with no backend reachable) if the API is briefly down.
 * Logs and returns `fallback`; the page shows its empty state and ISR retries later.
 */
export async function safe<T>(fetcher: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fetcher();
  } catch (err) {
    console.error("[safe] public fetch failed, using fallback:", err);
    return fallback;
  }
}
