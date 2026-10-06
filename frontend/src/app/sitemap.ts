import type { MetadataRoute } from "next";
import { connection } from "next/server";
import { getGames, getGenres, MAX_PAGE_SIZE } from "@/lib/api";
import type { GameSummary } from "@/lib/types";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

const SITE_URL = (process.env.SITE_URL ?? "http://localhost:3000").replace(/\/+$/, "");

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

  return [
    ...["/", "/games", "/coop", "/about"].map((path) => ({ url: `${SITE_URL}${path}` })),
    ...genres.map((genre) => ({ url: `${SITE_URL}/genres/${genre.slug}` })),
    ...games.map((game) => ({ url: `${SITE_URL}/games/${game.slug}` })),
  ];
}
