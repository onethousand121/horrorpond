"use client";

import { useActionState } from "react";
import { testAlertAction } from "../actions";
import { ResultMessage } from "./ActionButton";

/** 수집 실패 알림 메일이 실제로 도착하는지 확인한다 */
export function TestAlertButton() {
  const [state, action, pending] = useActionState(testAlertAction, null);
  return (
    <details className="rounded border border-border px-4 py-3">
      <summary className="cursor-pointer text-sm text-muted hover:text-foreground">알림 메일 테스트</summary>
      <form action={action} className="mt-3 flex flex-wrap items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "보내는 중…" : "테스트 메일 보내기"}
        </button>
        {state && <ResultMessage result={state} />}
      </form>
      <p className="mt-2 text-xs text-muted">수집에서 실패한 단계가 있을 때 오는 메일과 같은 경로로 한 통을 보내요.</p>
    </details>
  );
}
