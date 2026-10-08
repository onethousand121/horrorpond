import "server-only";

import { redirect } from "next/navigation";
import type { ErrorResponse, PageResponse } from "../types";
import { requireAdminKey } from "./session";
import type { AdminArticle, AdminGame, AdminGameDetail, GameStatus } from "./types";

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

export function upsertArticle(
  id: number,
  body: Pick<AdminArticle, "title" | "oneLiner" | "body" | "highlights" | "sponsored" | "sponsorDisclosure">,
) {
  return adminFetch<AdminArticle>(`/api/admin/games/${id}/article`, { method: "PUT", body });
}

export function publishGame(id: number) {
  return adminFetch<AdminGame>(`/api/admin/games/${id}/publish`, { method: "POST" });
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

export function replaceAchievements(
  id: number,
  achievements: { name: string; description: string; videoUrl: string }[],
) {
  return adminFetch<unknown>(`/api/admin/games/${id}/achievements`, { method: "PUT", body: { achievements } });
}
