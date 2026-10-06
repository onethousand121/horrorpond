import "server-only";

import type { GameDetail, GameSort, GameSummary, Genre, PageResponse } from "./types";

/**
 * 데이터 캐시 수명(초). 페이지의 `export const revalidate`와 같은 값으로 맞춘다
 * (세그먼트 설정은 정적 분석되어야 해서 상수를 import할 수 없다).
 */
export const REVALIDATE_SECONDS = 3600;

/** on-demand 갱신(revalidateTag)용 캐시 태그 */
export const CACHE_TAGS = {
  games: "games",
  genres: "genres",
  game: (slug: string) => `game:${slug}`,
} as const;

export const DEFAULT_PAGE_SIZE = 24;
export const MAX_PAGE_SIZE = 48;

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly path: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function apiBaseUrl(): string {
  const base = process.env.API_BASE_URL;
  if (!base) {
    throw new Error("API_BASE_URL is not set (see frontend/.env.example)");
  }
  return base.replace(/\/+$/, "");
}

async function apiFetch(path: string, tags: string[]): Promise<Response> {
  return fetch(`${apiBaseUrl()}${path}`, {
    headers: { Accept: "application/json" },
    next: { revalidate: REVALIDATE_SECONDS, tags },
  });
}

async function failure(res: Response, path: string): Promise<ApiError> {
  const body = await res.text().catch(() => "");
  return new ApiError(res.status, path, `API ${res.status} ${path}: ${body.slice(0, 200)}`);
}

export interface GetGamesParams {
  genre?: string;
  coop?: boolean;
  sort?: GameSort;
  page?: number;
  size?: number;
}

export async function getGames(params: GetGamesParams = {}): Promise<PageResponse<GameSummary>> {
  const query = new URLSearchParams();
  if (params.genre) query.set("genre", params.genre);
  if (params.coop !== undefined) query.set("coop", String(params.coop));
  if (params.sort) query.set("sort", params.sort);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? DEFAULT_PAGE_SIZE));

  const path = `/api/games?${query}`;
  const res = await apiFetch(path, [CACHE_TAGS.games]);
  if (!res.ok) throw await failure(res, path);
  return res.json();
}

/** 공개되지 않았거나 없는 게임이면 null. 그 외 오류(5xx 등)는 throw. */
export async function getGame(slug: string): Promise<GameDetail | null> {
  const path = `/api/games/${encodeURIComponent(slug)}`;
  const res = await apiFetch(path, [CACHE_TAGS.games, CACHE_TAGS.game(slug)]);
  if (res.status === 404) return null;
  if (!res.ok) throw await failure(res, path);
  return res.json();
}

export async function getGenres(): Promise<Genre[]> {
  const path = "/api/genres";
  const res = await apiFetch(path, [CACHE_TAGS.genres]);
  if (!res.ok) throw await failure(res, path);
  return res.json();
}
