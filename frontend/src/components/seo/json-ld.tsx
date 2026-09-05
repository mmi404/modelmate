/**
 * Renders a JSON-LD <script>. `data` is trusted, app-generated structured data;
 * we still escape `<` to keep it from breaking out of the script element.
 */
export function JsonLd({ data }: { data: Record<string, unknown> | Record<string, unknown>[] }) {
  const json = JSON.stringify(data).replace(/</g, "\u003c");
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: json }} />;
}
