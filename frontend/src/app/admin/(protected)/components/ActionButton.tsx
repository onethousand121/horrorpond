"use client";

import { useActionState } from "react";
import type { ActionResult } from "@/lib/admin/types";

type GameAction = (prev: ActionResult | null, formData: FormData) => Promise<ActionResult>;

/** 게임 하나에 대한 버튼형 액션(공개, 숨김 등). 결과 메시지를 버튼 옆에 보여준다. */
export function ActionButton({ action, gameId, label, pendingLabel, variant = "default", confirmMessage }: {
  action: GameAction;
  gameId: number;
  label: string;
  pendingLabel?: string;
  variant?: "default" | "primary" | "danger";
  confirmMessage?: string;
}) {
  const [state, formAction, pending] = useActionState(action, null);
  const styles = {
    default: "border border-border hover:border-foreground/40",
    primary: "bg-accent text-white hover:bg-accent/90",
    danger: "border border-red-500/40 text-red-300 hover:bg-red-500/10",
  }[variant];

  return (
    <form
      action={formAction}
      onSubmit={(event) => {
        if (confirmMessage && !window.confirm(confirmMessage)) event.preventDefault();
      }}
      className="inline-flex items-center gap-2"
    >
      <input type="hidden" name="gameId" value={gameId} />
      <button type="submit" disabled={pending} className={`rounded px-3 py-1.5 text-sm disabled:opacity-50 ${styles}`}>
        {pending ? (pendingLabel ?? "처리 중…") : label}
      </button>
      {state && <ResultMessage result={state} />}
    </form>
  );
}

export function ResultMessage({ result }: { result: ActionResult }) {
  return (
    <span role="status" className={`text-sm ${result.ok ? "text-emerald-400" : "text-red-400"}`}>
      {result.message}
    </span>
  );
}
