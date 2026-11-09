"use client";

import { useActionState } from "react";
import type { AdminDailyPick } from "@/lib/admin/api";
import { removeDailyPickAction, saveDailyPickAction } from "../actions";
import { ActionButton, ResultMessage } from "./ActionButton";

const inputClass = "w-full rounded border border-border bg-background px-3 py-1.5 text-sm";

/** 백엔드 DailyPickCandidate 제약 (서버가 최종 검증한다) */
const LIMITS = { intro: 120, introEn: 200 };

/**
 * 오늘의 게임 후보. 메인 "지금 뜨는" 아래에 하루씩 돌아가며 크게 보인다 (날짜를 고르면 그날 고정).
 */
export function DailyPickForm({ gameId, pick, sponsoredDefault }: {
  gameId: number;
  pick: AdminDailyPick | null;
  /** 처음 등록할 때 협찬 체크 기본값 (추천 글이 협찬이면 켠다) */
  sponsoredDefault: boolean;
}) {
  const [state, action, pending] = useActionState(saveDailyPickAction, null);
  return (
    <div className="space-y-3">
      <form action={action} className="space-y-2">
        <input type="hidden" name="gameId" value={gameId} />
        <input
          name="intro"
          defaultValue={pick?.intro ?? ""}
          maxLength={LIMITS.intro}
          required
          placeholder="한 줄 소개 (예: 13명 중 이상한 사람을 찾아내는 감옥 추리 호러)"
          className={inputClass}
        />
        <input
          name="introEn"
          defaultValue={pick?.introEn ?? ""}
          maxLength={LIMITS.introEn}
          lang="en"
          placeholder="영어 소개 (선택, 없으면 영어 화면도 한국어)"
          className={inputClass}
        />
        <div className="flex flex-wrap items-center gap-4 text-sm">
          <label className="flex items-center gap-1.5">
            <input type="checkbox" name="sponsored" defaultChecked={pick?.sponsored ?? sponsoredDefault} />
            개발사 제공 (키·자료를 받음)
          </label>
          <label className="flex items-center gap-1.5">
            이 날짜에 고정
            <input type="date" name="featuredOn" defaultValue={pick?.featuredOn ?? ""} className="rounded border border-border bg-background px-2 py-1" />
          </label>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="submit"
            disabled={pending}
            className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
          >
            {pending ? "저장 중…" : pick ? "후보 수정" : "후보로 등록"}
          </button>
          {state && <ResultMessage result={state} />}
        </div>
      </form>
      {pick && (
        <ActionButton
          action={removeDailyPickAction}
          gameId={gameId}
          label="후보에서 빼기"
          pendingLabel="빼는 중…"
          confirmMessage="오늘의 게임 후보에서 뺄까요?"
        />
      )}
      {pick && !pick.showable && (
        <p className="text-sm text-amber-400">
          지금은 사이트에 보이지 않는 게임이라 메인에 나오지 않아요. 위의 공개 버튼으로 고정 노출해 주세요.
        </p>
      )}
    </div>
  );
}
