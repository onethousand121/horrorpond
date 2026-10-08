import type { MetadataRoute } from "next";
import { SITE_INDEXING, SITE_URL } from "@/lib/site";

export default function robots(): MetadataRoute.Robots {
  if (!SITE_INDEXING) {
    // 비공개 운영: 전체 차단
    return { rules: { userAgent: "*", disallow: "/" } };
  }
  return {
    rules: { userAgent: "*", allow: "/", disallow: "/admin" },
    sitemap: `${SITE_URL}/sitemap.xml`,
  };
}
