import type { GetGamesParams } from "./api";
import type { Locale } from "./i18n";

/**
 * 색인되는 모음 페이지(/collections/{slug}). 필터 목록(/games?free=1 등)은 noindex라
 * 검색어("무료 공포게임" 등)에 맞춘 고정 주소를 따로 둔다. 문구는 lib/i18n의 collections 사전.
 * 백엔드에 이미 있는 공개 게임 조회 필터만 쓴다.
 */
export const COLLECTIONS = [
  { slug: "trending", query: { sort: "TRENDING" }, seeAll: "/games" },
  { slug: "upcoming", query: { release: "UPCOMING" }, seeAll: "/games?view=upcoming" },
  { slug: "free", query: { free: true, sort: "POPULAR" }, seeAll: "/games?view=popular&free=1" },
  // 한국어 지원 표시는 한국어 화면에만 있으므로 영어 페이지는 없다 (404, 사이트맵·hreflang 제외)
  { slug: "korean", query: { korean: true, sort: "POPULAR" }, seeAll: "/games?view=popular&korean=1", locales: ["ko"] },
  { slug: "picks", query: { keeper: true, sort: "POPULAR" }, seeAll: "/games?view=picked" },
] as const satisfies readonly {
  slug: string;
  query: GetGamesParams;
  seeAll: string;
  locales?: readonly Locale[];
}[];

export type Collection = (typeof COLLECTIONS)[number];
export type CollectionSlug = Collection["slug"];

export function collectionLocales(collection: Collection): readonly Locale[] {
  return "locales" in collection ? collection.locales : ["ko", "en"];
}

/** 그 언어에 있는 모음만 */
export function findCollection(slug: string, locale: Locale): Collection | undefined {
  const collection = COLLECTIONS.find((c) => c.slug === slug);
  return collection && collectionLocales(collection).includes(locale) ? collection : undefined;
}

export function collectionsFor(locale: Locale): Collection[] {
  return COLLECTIONS.filter((c) => collectionLocales(c).includes(locale));
}

/**
 * 언어 전환기용: 그 언어에 없는 페이지(한국어 전용 모음)는 대신 갈 주소. path는 언어 접두사 없는 주소
 */
export function pathForLocale(locale: Locale, path: string): string {
  const match = /^\/collections\/([^/]+)$/.exec(path);
  if (!match) return path;
  const collection = COLLECTIONS.find((c) => c.slug === match[1]);
  if (!collection || collectionLocales(collection).includes(locale)) return path;
  return "/games";
}
