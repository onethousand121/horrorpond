"use server";

// 관리 액션은 모두 맨 앞에서 로그인을 확인한다 (쿠키 없는 호출이 캐시 만료·외부 호출을 하지 않게)
import { requireAdminKey } from "@/lib/admin/session";

import { revalidatePath, updateTag } from "next/cache";
import {
  addItchGame,
  addSteamSeed,
  AdminApiError,
  changeTrendingOverride,
  getAdminGame,
  getTrendingPins,
  lookupGame,
  publishGame,
  reorderTrendingPins,
  replaceReferenceVideos,
} from "@/lib/admin/api";
import type { ActionResult, AdminGameDetail } from "@/lib/admin/types";
import { CACHE_TAGS } from "@/lib/api";
import { parseResearch, youtubeIdOf, type ResearchGame } from "@/lib/admin/trendingResearch";
import { youtubeInfo } from "@/lib/admin/youtubeInfo";

/** 이번 결과가 이보다 적으면 조사가 덜 된 회차로 보고 지난주 자동 고정을 풀지 않는다 */
const MIN_GAMES_TO_UNPIN = 5;
const MAX_REFERENCE_VIDEOS = 10;
/** 한 번에 반영할 게임 수. 서버 액션 시간 제한(60초) 안에 끝내기 위해 */
const MAX_GAMES = 20;

export interface ImportRow {
  title: string;
  gameId: number | null;
  /** 고정 · 고정 유지 · 신규 추가 · 제외 유지 · 확인 필요 · 고정 해제 · 오류 */
  action: string;
  video: string;
  note: string;
}

export interface ImportState {
  ok: boolean;
  message: string;
  rows: ImportRow[];
  reviewQueue: { title: string; reason: string }[];
  warnings: string[];
}

/**
 * 매주 조사 결과를 "지금 뜨는"과 참고 영상에 반영한다. 게임마다 따로 처리해 하나가 실패해도 나머지는 계속한다.
 */
export async function applyTrendingResearchAction(_prev: ImportState | null, formData: FormData): Promise<ImportState> {
  await requireAdminKey();
  let research;
  try {
    research = parseResearch(String(formData.get("json") ?? ""));
  } catch (e) {
    return { ok: false, message: (e as Error).message, rows: [], reviewQueue: [], warnings: [] };
  }

  if (research.games.length > MAX_GAMES) {
    return { ok: false, message: `게임은 한 번에 ${MAX_GAMES}개까지 반영할 수 있어요 (${research.games.length}개).`, rows: [], reviewQueue: [], warnings: [] };
  }

  const rows: ImportRow[] = [];
  const progress: Progress = { pinnedNow: new Set<number>(), changed: false };
  try {
    return await applyAll(research, rows, progress);
  } finally {
    // 중간에 실패해도 이미 반영된 변경이 사이트에 보이게 캐시를 비운다.
    // 백엔드 변경이 하나도 성공하지 않았으면(예: 첫 호출에서 인증 실패 → 로그인으로 redirect) 캐시를 건드리지 않는다.
    // finally는 예외(redirect 포함)를 그대로 다시 던진다
    if (progress.changed) {
      updateTag(CACHE_TAGS.games);
      revalidatePath("/admin", "layout");
    }
  }
}

/** 이번 반영에서 자동 고정한 게임과, 백엔드 변경이 하나라도 성공했는지 */
interface Progress {
  pinnedNow: Set<number>;
  changed: boolean;
}

async function applyAll(research: ReturnType<typeof parseResearch>, rows: ImportRow[], progress: Progress): Promise<ImportState> {
  const { pinnedNow } = progress;
  for (const game of research.games) {
    try {
      rows.push(await applyGame(game, progress));
    } catch (e) {
      if (!(e instanceof AdminApiError)) throw e;
      rows.push({ title: game.title, gameId: null, action: "오류", video: "-", note: e.message });
    }
  }

  // 지난주 자동 고정 중 이번 결과에 없는 것만 푼다. 관리자가 직접 고정한 건 그대로 둔다
  let unpinNote = "";
  if (research.games.length >= MIN_GAMES_TO_UNPIN) {
    for (const pin of await getTrendingPins()) {
      if (pin.auto && !pinnedNow.has(pin.id)) {
        await changeTrendingOverride(pin.id, null);
        progress.changed = true;
        rows.push({ title: pin.title, gameId: pin.id, action: "고정 해제", video: "-", note: "이번 주 결과에 없음" });
      }
    }
  } else {
    unpinNote = ` 결과가 ${research.games.length}개뿐이라 지난주 고정은 그대로 뒀어요.`;
  }

  return {
    ok: true,
    message: `${research.games.length}개 처리했어요.${unpinNote}`,
    rows,
    reviewQueue: research.reviewQueue,
    warnings: research.warnings,
  };
}

async function applyGame(game: ResearchGame, progress: Progress): Promise<ImportRow> {
  const row: ImportRow = { title: game.title, gameId: null, action: "", video: "-", note: game.why ?? "" };
  if (game.confidence === "low") {
    return { ...row, action: "확인 필요", note: "신뢰도 낮음 — 직접 확인 후 관리 화면에서 고정해 주세요" };
  }
  if (game.steamAppId === null && game.itchUrl === null) {
    return { ...row, action: "확인 필요", note: "상점 주소가 없거나 알아볼 수 없음" };
  }

  let detail: AdminGameDetail | null = game.steamAppId !== null
    ? await lookupGame({ steamAppId: game.steamAppId })
    : await lookupGame({ itchUrl: game.itchUrl! });
  if (detail === null && game.steamAppId !== null && game.itchUrl !== null) {
    detail = await lookupGame({ itchUrl: game.itchUrl });
  }
  if (detail === null) {
    if (game.steamAppId !== null) {
      try {
        await addSteamSeed(game.steamAppId);
        progress.changed = true;
        return { ...row, action: "신규 추가", note: "다음 새벽 수집 후 들어와요. 고정은 다음 주 반영 때" };
      } catch (e) {
        if (e instanceof AdminApiError && e.status === 409) {
          return { ...row, action: "확인 필요", note: "수집은 됐지만 게임으로 등록되지 않음 (DLC·공포 아님 판정 등)" };
        }
        throw e;
      }
    }
    const { gameId } = await addItchGame(game.itchUrl!);
    progress.changed = true;
    detail = await getAdminGame(gameId);
    if (detail === null) return { ...row, action: "오류", note: "itch.io 게임을 받았지만 찾지 못함" };
    row.note = `itch.io에서 새로 받음. ${row.note}`;
  }
  row.gameId = detail.id;

  if (detail.status === "HIDDEN" || detail.trendingOverride === "EXCLUDED") {
    return { ...row, action: "제외 유지", note: detail.status === "HIDDEN" ? "숨긴 게임" : "지금 뜨는에서 뺀 게임" };
  }
  if (detail.adult) {
    return { ...row, action: "확인 필요", note: "성인 게임이라 건너뜀" };
  }
  if (!detail.publiclyVisible) {
    await publishGame(detail.id);
    progress.changed = true;
    row.note = `사이트에 안 보이던 게임이라 고정 노출함. ${row.note}`;
  }

  if (detail.trendingOverride === "PINNED" && !detail.trendingAuto) {
    row.action = "고정 유지"; // 관리자가 직접 고정한 게임은 자동 고정으로 바꾸지 않는다
  } else {
    if (detail.trendingOverride !== "PINNED") {
      await changeTrendingOverride(detail.id, "PINNED", true);
      progress.changed = true;
    }
    row.action = "고정";
    progress.pinnedNow.add(detail.id);
  }

  row.video = await applyReferenceVideo(detail, game);
  if (row.video === "추가") progress.changed = true;
  return row;
}

async function applyReferenceVideo(detail: AdminGameDetail, game: ResearchGame): Promise<string> {
  const video = game.referenceVideo;
  if (detail.playVideos.length > 0) return "내 영상 있음";
  if (!video) return "-";
  if (video.originalChannel !== "verified") return "확인 필요 (원 채널 미확인)";
  const youtubeId = youtubeIdOf(video.url);
  if (!youtubeId) return "확인 필요 (유튜브 주소 아님)";
  const existing = detail.referenceVideos ?? [];
  if (existing.some((v) => v.youtubeId === youtubeId)) return "이미 있음";
  if (existing.length >= MAX_REFERENCE_VIDEOS) return "생략 (참고 영상 가득 참)";
  const info = await youtubeInfo(video.url);
  if (!info.ok) return "생략 (퍼가기 불가·비공개)";
  const channelName = video.channel ?? info.author;
  if (!channelName) return "확인 필요 (채널 이름 없음)";
  await replaceReferenceVideos(detail.id, [
    ...existing.map((v) => ({
      url: `https://www.youtube.com/watch?v=${v.youtubeId}`,
      title: v.title ?? "",
      channelName: v.channelName ?? "",
    })),
    { url: video.url, title: video.title ?? info.title, channelName },
  ]);
  return "추가";
}

/** 고정 게임 순서 저장. ids는 쉼표로 이은 게임 id (화면 위에서부터) */
export async function reorderTrendingPinsAction(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  await requireAdminKey();
  const ids = String(formData.get("ids") ?? "")
    .split(",")
    .map(Number)
    .filter((id) => Number.isInteger(id) && id > 0);
  try {
    await reorderTrendingPins(ids);
  } catch (e) {
    if (e instanceof AdminApiError) return { ok: false, message: e.message };
    throw e;
  }
  updateTag(CACHE_TAGS.games);
  revalidatePath("/admin", "layout");
  return { ok: true, message: "순서를 저장했어요. 메인 \"지금 뜨는\"에 바로 반영돼요." };
}
