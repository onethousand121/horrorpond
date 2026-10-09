"use client";

import { useActionState } from "react";
import { addSteamGamesAction } from "../actions";
import { ResultMessage } from "./ActionButton";

/**
 * 자동 수집에 없는 게임을 Steam 주소로 직접 추가한다. 수동 추가는 공포 판정 없이 다음 수집에서 가장 먼저 받는다.
 */
export function AddSteamGamesForm() {
  const [state, action, pending] = useActionState(addSteamGamesAction, null);
  return (
    <details className="rounded border border-border px-4 py-3">
      <summary className="cursor-pointer text-sm text-muted hover:text-foreground">Steam 게임 직접 추가</summary>
      <form action={action} className="mt-3 space-y-2">
        <textarea
          name="steam"
          rows={3}
          placeholder={"https://store.steampowered.com/app/3272170/Face_of_Another/\n3751730"}
          className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
        />
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="submit"
            disabled={pending}
            className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
          >
            {pending ? "추가 중…" : "추가"}
          </button>
          {state && <ResultMessage result={state} />}
        </div>
        <p className="text-xs text-muted">
          Steam 상점 주소나 appid를 줄마다 하나씩(최대 30개). 매일 04:00 수집에서 가장 먼저 받아 사이트에 올라와요.
        </p>
      </form>
    </details>
  );
}
