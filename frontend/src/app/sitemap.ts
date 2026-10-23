import type { MetadataRoute } from "next";
import { connection } from "next/server";
import { getGames, getGenres, getSitemapGames, MAX_PAGE_SIZE, type SitemapGame } from "@/lib/api";
import { collectionLocales, COLLECTIONS } from "@/lib/collections";
import { alternateLanguages, LOCALES, localePath, type Locale } from "@/lib/i18n";
import { SITE_URL } from "@/lib/site";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.


/**
 * 공개 게임(성인 제외) 주소 목록. 사이트맵 전용 API 한 번으로 받고,
 * 그 API가 없는 이전 백엔드면 목록 API를 페이지마다 넘기며 모은다.
 */
async function allPublishedGames(): Promise<SitemapGame[]> {
  const sitemapGames = await getSitemapGames();
  if (sitemapGames) return sitemapGames;
  const games: SitemapGame[] = [];
  for (let page = 0; ; page++) {
    const result = await getGames({ page, size: MAX_PAGE_SIZE });
    games.push(...result.content.filter((game) => !game.adult));
    if (!result.hasNext) return games;
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  await connection();
  const [genres, games] = await Promise.all([getGenres(), allPublishedGames()]);

  const paths = [
    ...["/", "/games", "/coop", "/about", "/privacy"],
    ...genres.map((genre) => `/genres/${genre.slug}`),
    // 성인 게임 페이지는 noindex라 사이트맵에서도 뺀다 (allPublishedGames가 이미 뺐다)
    ...games.map((game) => `/games/${game.slug}`),
    ...games.filter((game) => game.hasAchievementGuide).map((game) => `/games/${game.slug}/achievements`),
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
