import type { MetadataRoute } from "next";
import { getGames, getGenres, MAX_PAGE_SIZE } from "@/lib/api";
import type { GameSummary } from "@/lib/types";

export const revalidate = 3600;

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
  const [genres, games] = await Promise.all([getGenres(), allPublishedGames()]);

  return [
    ...["/", "/games", "/coop", "/about"].map((path) => ({ url: `${SITE_URL}${path}` })),
    ...genres.map((genre) => ({ url: `${SITE_URL}/genres/${genre.slug}` })),
    ...games.map((game) => ({ url: `${SITE_URL}/games/${game.slug}` })),
  ];
}
