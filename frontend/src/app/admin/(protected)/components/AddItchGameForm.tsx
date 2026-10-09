"use client";

import { useActionState } from "react";
import { addItchGameAction } from "../actions";
import { ResultMessage } from "./ActionButton";

/**
 * itch.io 게임을 주소로 바로 받는다. 자동 수집은 평가 많은 인기작만 받으므로, 그 밖의 게임은 여기서 골라 넣는다.
 */
export function AddItchGameForm() {
  const [state, action, pending] = useActionState(addItchGameAction, null);
  return (
    <details className="rounded border border-border px-4 py-3">
      <summary className="cursor-pointer text-sm text-muted hover:text-foreground">itch.io 게임 직접 추가</summary>
      <form action={action} className="mt-3 space-y-2">
        <input
          name="url"
          type="url"
          placeholder="https://작성자.itch.io/게임"
          className="w-full rounded border border-border bg-background px-3 py-2 text-sm"
        />
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="submit"
            disabled={pending}
            className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
          >
            {pending ? "받아 오는 중…" : "추가"}
          </button>
          {state && <ResultMessage result={state} />}
        </div>
        <p className="text-xs text-muted">
          바로 받아 와서 그 게임 관리 화면으로 이동해요. 평가가 적은 게임은 거기서 고정 노출해야 사이트에 보여요.
          자동 수집은 공포 태그 평점순 목록에서 평가 500개 이상인 게임만 받아요.
        </p>
      </form>
    </details>
  );
}
