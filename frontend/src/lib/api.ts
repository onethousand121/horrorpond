import "server-only";

import { DEFAULT_LOCALE, type Locale } from "./i18n";
import type { GameDetail, GameSort, GameSummary, Genre, HubStats, PageResponse, ReleaseWindow } from "./types";

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
  videos: "videos",
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

/** 백엔드 기본 언어는 한국어라, 영어일 때만 lang을 붙인다 (한국어 캐시 키는 그대로) */
function withLang(path: string, lang: Locale | undefined): string {
  if (!lang || lang === DEFAULT_LOCALE) return path;
  return `${path}?lang=${lang}`;
}

async function failure(res: Response, path: string): Promise<ApiError> {
  const body = await res.text().catch(() => "");
  return new ApiError(res.status, path, `API ${res.status} ${path}: ${body.slice(0, 200)}`);
}

export interface GetGamesParams {
  /** 제목·소개·장르 이름 언어 (기본 한국어) */
  lang?: Locale;
  /** 제목 검색 (최대 100자) */
  q?: string;
  genre?: string;
  coop?: boolean;
  release?: ReleaseWindow;
  /** 재일 추천만 */
  picked?: boolean;
  sort?: GameSort;
  page?: number;
  size?: number;
}

export async function getGames(params: GetGamesParams = {}): Promise<PageResponse<GameSummary>> {
  const query = new URLSearchParams();
  if (params.q) query.set("q", params.q);
  if (params.genre) query.set("genre", params.genre);
  if (params.coop !== undefined) query.set("coop", String(params.coop));
  if (params.release) query.set("release", params.release);
  if (params.picked) query.set("picked", "true");
  if (params.sort) query.set("sort", params.sort);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? DEFAULT_PAGE_SIZE));

  if (params.lang && params.lang !== DEFAULT_LOCALE) query.set("lang", params.lang);

  const path = `/api/games?${query}`;
  const res = await apiFetch(path, [CACHE_TAGS.games]);
  if (!res.ok) throw await failure(res, path);
  return res.json();
}

/** 공개되지 않았거나 없는 게임이면 null. 그 외 오류(5xx 등)는 throw. */
export async function getGame(slug: string, lang?: Locale): Promise<GameDetail | null> {
  const path = withLang(`/api/games/${encodeURIComponent(slug)}`, lang);
  const res = await apiFetch(path, [CACHE_TAGS.games, CACHE_TAGS.game(slug)]);
  if (res.status === 404) return null;
  if (!res.ok) throw await failure(res, path);
  return res.json();
}

export async function getGenres(lang?: Locale): Promise<Genre[]> {
  const path = withLang("/api/genres", lang);
  const res = await apiFetch(path, [CACHE_TAGS.genres]);
  if (!res.ok) throw await failure(res, path);
  return res.json();
}

export async function getStats(): Promise<HubStats> {
  const path = "/api/stats";
  const res = await apiFetch(path, [CACHE_TAGS.games]);
  if (!res.ok) throw await failure(res, path);
  return res.json();
}
