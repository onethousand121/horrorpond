import type { MetadataRoute } from "next";
import { connection } from "next/server";
import { getGames, getGenres, MAX_PAGE_SIZE } from "@/lib/api";
import { collectionLocales, COLLECTIONS } from "@/lib/collections";
import { alternateLanguages, LOCALES, localePath, type Locale } from "@/lib/i18n";
import { SITE_URL } from "@/lib/site";
import type { GameSummary } from "@/lib/types";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.


async function allPublishedGames(): Promise<GameSummary[]> {
  const games: GameSummary[] = [];
  for (let page = 0; ; page++) {
    const result = await getGames({ page, size: MAX_PAGE_SIZE });
    games.push(...result.content);
    if (!result.hasNext) return games;
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  await connection();
  const [genres, games] = await Promise.all([getGenres(), allPublishedGames()]);

  const paths = [
    ...["/", "/games", "/coop", "/about", "/privacy"],
    ...genres.map((genre) => `/genres/${genre.slug}`),
    // 성인 게임 페이지는 noindex라 사이트맵에서도 뺀다
    ...games.filter((game) => !game.adult).map((game) => `/games/${game.slug}`),
    ...games
      .filter((game) => !game.adult && game.hasAchievementGuide)
      .map((game) => `/games/${game.slug}/achievements`),
  ];
  const entries: { path: string; locales: readonly Locale[] }[] = [
    ...paths.map((path) => ({ path, locales: LOCALES })),
    // 모음 페이지. 한국어 전용 모음은 한국어 주소만
    ...COLLECTIONS.map((c) => ({ path: `/collections/${c.slug}`, locales: collectionLocales(c) })),
  ];
  // 언어마다 따로 항목을 두고, 서로를 hreflang으로 연결한다 (x-default는 한국어)
  return entries.flatMap(({ path, locales }) => {
    const languages = Object.fromEntries(
      Object.entries(alternateLanguages(path, locales)).map(([key, href]) => [key, `${SITE_URL}${href}`]),
    );
    return locales.map((locale) => ({ url: `${SITE_URL}${localePath(locale, path)}`, alternates: { languages } }));
  });
}
