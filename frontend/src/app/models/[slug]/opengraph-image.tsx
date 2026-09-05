import { ImageResponse } from "next/og";
import { getModel } from "@/lib/api/catalog";
import { toNumber } from "@/lib/format";
import { SITE_NAME } from "@/lib/site";

export const alt = "Model rating summary";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export const revalidate = 3600;

export default async function OgImage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;

  let name = slug;
  let creator: string | null = null;
  let category = "";
  let rating: number | null = null;
  let reviewCount = 0;
  try {
    const model = await getModel(slug);
    name = model.name;
    creator = model.creator;
    category = model.category.name;
    rating = toNumber(model.ratings.overall);
    reviewCount = model.ratings.reviewCount;
  } catch {
    // render with just the slug
  }

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: "linear-gradient(135deg, #0b0b0f 0%, #16161d 100%)",
          color: "#fafafa",
          padding: 72,
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ fontSize: 30, color: "#a1a1aa", display: "flex" }}>
          {SITE_NAME} · {category}
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <div style={{ fontSize: 88, fontWeight: 700, lineHeight: 1.05 }}>{name}</div>
          {creator ? (
            <div style={{ fontSize: 34, color: "#a1a1aa" }}>by {creator}</div>
          ) : null}
        </div>
        <div style={{ fontSize: 40, display: "flex", alignItems: "center", gap: 16 }}>
          {rating !== null ? (
            <>
              <span style={{ color: "#fbbf24" }}>{"★".repeat(Math.round(rating))}</span>
              <span style={{ fontWeight: 700 }}>{rating.toFixed(1)}/5</span>
              <span style={{ color: "#a1a1aa" }}>
                from {reviewCount} review{reviewCount === 1 ? "" : "s"}
              </span>
            </>
          ) : (
            <span style={{ color: "#a1a1aa" }}>Not yet rated — be the first</span>
          )}
        </div>
      </div>
    ),
    size,
  );
}
