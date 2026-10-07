"use server";

import { revalidatePath, updateTag } from "next/cache";
import {
  AdminApiError,
  hideGame,
  publishGame,
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
    "글을 저장했습니다.",
  );
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
