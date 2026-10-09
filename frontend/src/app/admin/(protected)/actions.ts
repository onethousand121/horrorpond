"use server";

import { revalidatePath, updateTag } from "next/cache";
import {
  addSteamSeed,
  AdminApiError,
  hideGame,
  publishGame,
  replaceAchievements,
  replacePlayVideos,
  unhideGame,
  updateCuration,
  upsertArticle,
} from "@/lib/admin/api";
import { CACHE_TAGS } from "@/lib/api";
import { ARTICLE_LIMITS, type ActionResult } from "@/lib/admin/types";

/**
 * 공개 사이트의 게임 데이터는 모두 "games" 태그로 캐시된다(lib/api). 공개 상태나 글이 바뀌면 이 태그를 바로 만료시켜
 * 다음 방문에서 새 데이터로 다시 그리게 한다. 공개 전에 404로 캐시된 상세 페이지도 같은 태그라 함께 풀린다.
 */
function refreshPages() {
  updateTag(CACHE_TAGS.games);
  // 관리자 화면(no-store 조회)도 바뀐 상태로 다시 그린다
  revalidatePath("/admin", "layout");
}

/** 백엔드 검증 오류는 메시지로 돌려주고, 그 외(리다이렉트 등)는 그대로 던진다. */
async function run(task: () => Promise<unknown>, success: string): Promise<ActionResult> {
  try {
    await task();
  } catch (e) {
    if (e instanceof AdminApiError) return { ok: false, message: e.message };
    throw e;
  }
  refreshPages();
  return { ok: true, message: success };
}

function gameId(formData: FormData): number {
  const id = Number(formData.get("gameId"));
  if (!Number.isInteger(id) || id <= 0) throw new Error("invalid gameId");
  return id;
}

export async function saveCurationAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  const slug = String(formData.get("slug") ?? "").trim();
  const genreSlugs = formData.getAll("genre").map(String);
  const coop = formData.has("coopEditable") ? formData.get("coop") === "on" : undefined;
  return run(() => updateCuration(id, { slug, genreSlugs, coop }), "slug와 장르를 저장했습니다.");
}

export async function saveArticleAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  const highlights = formData
    .getAll("highlight")
    .map((value) => String(value).trim())
    .filter(Boolean)
    .slice(0, ARTICLE_LIMITS.highlights);
  const sponsored = formData.get("sponsored") === "on";
  const disclosure = String(formData.get("sponsorDisclosure") ?? "").trim();
  return run(
    () =>
      upsertArticle(id, {
        title: String(formData.get("title") ?? ""),
        oneLiner: String(formData.get("oneLiner") ?? ""),
        body: String(formData.get("body") ?? ""),
        highlights,
        sponsored,
        sponsorDisclosure: sponsored ? disclosure : null,
      }),
    "글을 저장했습니다. 사이트에 '주인장 추천'으로 올리려면 위 '노출' 칸의 버튼을 누르세요(장점 포인트 1개 이상 필요).",
  );
}

/**
 * 플레이 영상 목록 전체 저장. 제목을 비워 두면 유튜브 oEmbed(키 불필요)로 영상 제목을 채운다.
 * 폼은 행마다 videoUrl / videoTitle을 같은 순서로 보낸다. 주소가 빈 행은 버린다.
 */
export async function savePlayVideosAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  const urls = formData.getAll("videoUrl").map((v) => String(v).trim());
  const titles = formData.getAll("videoTitle").map((v) => String(v).trim());
  const rows = urls.map((url, i) => ({ url, title: titles[i] ?? "" })).filter((row) => row.url);
  const videos = await Promise.all(
    rows.map(async (row) => ({ url: row.url, title: row.title || (await youtubeTitle(row.url)) })),
  );
  return run(() => replacePlayVideos(id, videos), `플레이 영상 ${videos.length}개를 저장했습니다.`);
}

/**
 * 업적 공략 목록 전체 저장. 이름이 빈 행은 버린다.
 */
export async function saveAchievementsAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  const names = formData.getAll("achievementName").map((v) => String(v).trim());
  const descriptions = formData.getAll("achievementDescription").map((v) => String(v).trim());
  const videoUrls = formData.getAll("achievementVideoUrl").map((v) => String(v).trim());
  const achievements = names
    .map((name, i) => ({ name, description: descriptions[i] ?? "", videoUrl: videoUrls[i] ?? "" }))
    .filter((row) => row.name);
  return run(() => replaceAchievements(id, achievements), `업적 ${achievements.length}개를 저장했습니다.`);
}

/** 유튜브 영상 제목. 실패하면 빈 문자열(제목 없이 저장) */
async function youtubeTitle(url: string): Promise<string> {
  try {
    // 영상 ID만 넣은 경우도 oEmbed가 알아보도록 주소로 바꾼다
    const target = /^[A-Za-z0-9_-]{11}$/.test(url) ? `https://www.youtube.com/watch?v=${url}` : url;
    const res = await fetch(`https://www.youtube.com/oembed?format=json&url=${encodeURIComponent(target)}`, {
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (!res.ok) return "";
    const data: unknown = await res.json();
    return typeof data === "object" && data !== null && "title" in data && typeof data.title === "string"
      ? data.title.slice(0, 200)
      : "";
  } catch {
    return "";
  }
}

export async function publishAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  return run(() => publishGame(id), "고정 노출했습니다. 사이트에 바로 반영됩니다.");
}

export async function hideAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  return run(() => hideGame(id), "숨겼습니다.");
}

export async function unhideAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const id = gameId(formData);
  return run(() => unhideGame(id), "숨김을 해제했습니다. 자동 노출 기준을 만족하면 다시 보입니다.");
}

/** Steam 상점 주소(…/app/123/…) 또는 숫자 appid. 못 읽으면 null */
function parseSteamAppId(line: string): number | null {
  const match = line.match(/store\.steampowered\.com\/app\/(\d+)/) ?? line.match(/^(\d{1,10})$/);
  const appid = match ? Number(match[1]) : NaN;
  return Number.isInteger(appid) && appid > 0 ? appid : null;
}

const MAX_SEEDS_PER_SUBMIT = 30;

export async function addSteamGamesAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const lines = String(formData.get("steam") ?? "")
    .split(/\s+/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, MAX_SEEDS_PER_SUBMIT);
  if (lines.length === 0) return { ok: false, message: "Steam 상점 주소나 appid를 넣어 주세요." };

  const added: number[] = [];
  const collected: number[] = [];
  const invalid: string[] = [];
  for (const line of lines) {
    const appid = parseSteamAppId(line);
    if (appid === null) {
      invalid.push(line);
      continue;
    }
    try {
      await addSteamSeed(appid);
      added.push(appid);
    } catch (e) {
      if (e instanceof AdminApiError && e.status === 409) collected.push(appid);
      else if (e instanceof AdminApiError) invalid.push(`${appid} (${e.message})`);
      else throw e;
    }
  }
  const parts = [
    added.length > 0 && `${added.length}개 추가 (다음 수집 때 가장 먼저 들어와요)`,
    collected.length > 0 && `이미 수집됨 ${collected.join(", ")} (검색해서 고정 노출하세요)`,
    invalid.length > 0 && `읽지 못함 ${invalid.join(", ")}`,
  ].filter(Boolean);
  return { ok: invalid.length === 0, message: parts.join(" · ") };
}
