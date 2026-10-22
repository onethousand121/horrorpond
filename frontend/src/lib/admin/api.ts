import "server-only";

import { redirect } from "next/navigation";
import type { EndingType, ErrorResponse, PageResponse } from "../types";
import { requireAdminKey } from "./session";
import type { AdminArticle, AdminGame, AdminGameDetail, GameStatus, TrendingOverride } from "./types";

export class AdminApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "AdminApiError";
  }
}

function apiBaseUrl(): string {
  const base = process.env.API_BASE_URL;
  if (!base) {
    throw new Error("API_BASE_URL is not set (see frontend/.env.example)");
  }
  return base.replace(/\/+$/, "");
}

/** 키 확인용. 로그인 액션에서만 쓴다. */
export async function isValidAdminKey(key: string): Promise<boolean> {
  const res = await fetch(`${apiBaseUrl()}/api/admin/games?size=1`, {
    headers: { "X-Admin-Key": key, Accept: "application/json" },
    cache: "no-store",
  });
  return res.ok;
}

/**
 * 관리자 API 호출. 캐시하지 않는다. 키가 틀리면(403) 로그인 화면으로 보낸다.
 * 그 외 오류는 백엔드 ErrorResponse.message를 담아 throw한다.
 */
async function adminFetch<T>(path: string, init: { method?: string; body?: unknown } = {}): Promise<T> {
  const key = await requireAdminKey();
  const res = await fetch(`${apiBaseUrl()}${path}`, {
    method: init.method ?? "GET",
    headers: {
      "X-Admin-Key": key,
      Accept: "application/json",
      ...(init.body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: init.body === undefined ? undefined : JSON.stringify(init.body),
    cache: "no-store",
  });
  if (res.status === 401 || res.status === 403) {
    redirect("/admin/login?expired=1");
  }
  if (!res.ok) {
    const error = (await res.json().catch(() => null)) as ErrorResponse | null;
    throw new AdminApiError(res.status, error?.message ?? `API ${res.status} ${path}`);
  }
  return res.json() as Promise<T>;
}

export interface SearchAdminGamesParams {
  status?: GameStatus;
  q?: string;
  page?: number;
  size?: number;
}

export function searchAdminGames(params: SearchAdminGamesParams): Promise<PageResponse<AdminGame>> {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.q) query.set("q", params.q);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 30));
  return adminFetch(`/api/admin/games?${query}`);
}

/** 없는 게임이면 null */
export async function getAdminGame(id: number): Promise<AdminGameDetail | null> {
  try {
    return await adminFetch<AdminGameDetail>(`/api/admin/games/${id}`);
  } catch (e) {
    if (e instanceof AdminApiError && e.status === 404) return null;
    throw e;
  }
}

export function updateCuration(id: number, body: { slug: string; genreSlugs: string[]; coop?: boolean }) {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/curation`, { method: "PUT", body });
}

/** 추천 내리기: 글은 초안으로 남는다 */
export function unpublishArticle(id: number): Promise<AdminGame> {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/article/unpublish`, { method: "POST" });
}

/** 추천 글 삭제 */
export function deleteArticle(id: number): Promise<AdminGame> {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/article`, { method: "DELETE" });
}

export function upsertArticle(
  id: number,
  body: Pick<AdminArticle, "kind" | "title" | "oneLiner" | "body" | "highlights" | "sponsored" | "sponsorDisclosure">,
) {
  return adminFetch<AdminArticle>(`/api/admin/games/${id}/article`, { method: "PUT", body });
}

/** 큐레이터 글 영어 번역 저장 (한국어 글이 있어야 한다) */
export function translateArticle(
  id: number,
  body: { title: string; oneLiner: string; body: string; highlights: string[] },
): Promise<AdminArticle> {
  return adminFetch<AdminArticle>(`/api/admin/games/${id}/article/translation`, { method: "PUT", body });
}

/** 영어 번역 삭제 (영어 화면에는 다시 한국어 글) */
export function clearArticleTranslation(id: number): Promise<AdminArticle> {
  return adminFetch<AdminArticle>(`/api/admin/games/${id}/article/translation`, { method: "DELETE" });
}

export function publishGame(id: number) {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/publish`, { method: "POST" });
}

/**
 * "지금 뜨는" 고정·제외 (null이면 자동 기준으로 되돌림).
 * @param auto 매주 조사 반영이 고정하면 true (다음 반영 때 결과에서 빠지면 풀린다)
 */
export function changeTrendingOverride(id: number, override: TrendingOverride | null, auto = false) {
  return adminFetch<unknown>(`/api/admin/games/${id}/trending`, { method: "PUT", body: { override, auto } });
}

/** "지금 뜨는"에 고정된 게임. auto는 매주 조사 반영이 고정한 것 */
export interface TrendingPin {
  id: number;
  title: string;
  /** 매주 조사 반영이 고정했으면 true */
  auto: boolean;
  /** 고정 게임끼리의 순서 (작을수록 앞). 정하지 않았으면 null. 백엔드 배포 전 응답에는 없다 */
  order?: number | null;
}

/** 사이트 "지금 뜨는"과 같은 순서 (정한 순서 → 순서 없는 게임) */
export function getTrendingPins(): Promise<TrendingPin[]> {
  return adminFetch(`/api/admin/games/trending-pins`);
}

/** 고정 게임 순서를 목록 순서대로 저장 */
export function reorderTrendingPins(gameIds: number[]): Promise<TrendingPin[]> {
  return adminFetch(`/api/admin/games/trending-pins/order`, { method: "PUT", body: { gameIds } });
}

/** 상점 정보로 게임 찾기. 없으면 null */
export async function lookupGame(by: { steamAppId: number } | { itchUrl: string }): Promise<AdminGameDetail | null> {
  const query = "steamAppId" in by ? `steamAppId=${by.steamAppId}` : `itchUrl=${encodeURIComponent(by.itchUrl)}`;
  try {
    return await adminFetch<AdminGameDetail>(`/api/admin/games/lookup?${query}`);
  } catch (e) {
    if (e instanceof AdminApiError && e.status === 404) return null;
    throw e;
  }
}

export function hideGame(id: number) {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/hide`, { method: "POST" });
}

export function unhideGame(id: number) {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/unhide`, { method: "POST" });
}

export function replacePlayVideos(id: number, videos: { url: string; title: string }[]) {
  return adminFetch<unknown>(`/api/admin/games/${id}/videos`, { method: "PUT", body: { videos } });
}

/** 다른 채널의 참고 영상 목록 전체 저장 (주인장 영상은 그대로) */
export function replaceReferenceVideos(id: number, videos: { url: string; title: string; channelName: string }[]) {
  return adminFetch<unknown>(`/api/admin/games/${id}/reference-videos`, { method: "PUT", body: { videos } });
}

export function replaceAchievements(
  id: number,
  achievements: { name: string; description: string; videoUrl: string }[],
) {
  return adminFetch<unknown>(`/api/admin/games/${id}/achievements`, { method: "PUT", body: { achievements } });
}

/** 엔딩 구분과 엔딩 목록을 한 번에 저장 (목록은 멀티 엔딩일 때만) */
export function replaceEndings(
  id: number,
  endingType: EndingType | null,
  endings: { name: string; description: string; videoUrl: string }[],
) {
  return adminFetch<unknown>(`/api/admin/games/${id}/endings`, { method: "PUT", body: { endingType, endings } });
}

/** 유저 한글패치 목록 전체 저장 */
export function replaceKoreanPatches(id: number, patches: { url: string; author: string; note: string }[]) {
  return adminFetch<unknown>(`/api/admin/games/${id}/korean-patches`, { method: "PUT", body: { patches } });
}

/**
 * Steam 게임 수동 추가(다음 수집에서 가장 먼저 받는다). 이미 수집된 게임이면 409(AdminApiError).
 */
export function addSteamSeed(appid: number): Promise<{ appid: number }> {
  return adminFetch(`/api/admin/ingestion/seeds`, { method: "POST", body: { appid } });
}

/**
 * itch.io 게임을 주소로 바로 받는다 (인기 기준 없음). 400: itch.io 게임 주소가 아님, 404: 없는 게임.
 */
/** 수집 실패 알림 메일 테스트. sent=false면 서버에 키·받는 주소가 없거나 Resend가 거절한 것 */
export function sendTestAlert(): Promise<{ sent: boolean }> {
  return adminFetch(`/api/admin/ingestion/alert-test`, { method: "POST" });
}

export function addItchGame(url: string): Promise<{ gameId: number }> {
  return adminFetch(`/api/admin/ingestion/itch`, { method: "POST", body: { url } });
}
