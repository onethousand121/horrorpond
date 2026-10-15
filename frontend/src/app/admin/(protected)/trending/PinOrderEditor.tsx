"use client";

import { useActionState, useState } from "react";
import type { TrendingPin } from "@/lib/admin/api";
import { ResultMessage } from "../components/ActionButton";
import { reorderTrendingPinsAction } from "./actions";

/** 메인 "지금 뜨는" 쇼케이스에 나오는 개수 (그 아래는 목록에서만 보인다) */
const SHOWCASE = 5;

/**
 * "지금 뜨는"에 고정한 게임의 순서. 위아래로 옮긴 뒤 저장하면 메인 쇼케이스와 목록 맨 앞이 그 순서가 된다.
 */
export function PinOrderEditor({ pins }: { pins: TrendingPin[] }) {
  const [items, setItems] = useState(pins);
  const [state, action, pending] = useActionState(reorderTrendingPinsAction, null);

  const move = (index: number, delta: number) => {
    const target = index + delta;
    if (target < 0 || target >= items.length) return;
    const next = [...items];
    [next[index], next[target]] = [next[target], next[index]];
    setItems(next);
  };

  if (items.length === 0) {
    return <p className="text-sm text-muted">고정한 게임이 없어요. 조사 결과를 반영하거나 게임 화면에서 고정하세요.</p>;
  }

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="ids" value={items.map((pin) => pin.id).join(",")} />
      <ol className="divide-y divide-border rounded border border-border">
        {items.map((pin, index) => (
          <li key={pin.id} className="flex items-center gap-3 px-3 py-2 text-sm">
            <span className="w-6 text-right font-mono text-muted">{index + 1}</span>
            <span className="min-w-0 flex-1 truncate">
              {pin.title}
              {index < SHOWCASE && <span className="ml-2 text-xs text-accent">메인</span>}
              {pin.auto && <span className="ml-2 text-xs text-muted">자동 고정</span>}
            </span>
            <button
              type="button"
              onClick={() => move(index, -1)}
              disabled={index === 0}
              aria-label={`${pin.title} 위로`}
              className="rounded border border-border px-2 py-0.5 hover:border-foreground/40 disabled:opacity-30"
            >
              ↑
            </button>
            <button
              type="button"
              onClick={() => move(index, 1)}
              disabled={index === items.length - 1}
              aria-label={`${pin.title} 아래로`}
              className="rounded border border-border px-2 py-0.5 hover:border-foreground/40 disabled:opacity-30"
            >
              ↓
            </button>
          </li>
        ))}
      </ol>
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded bg-accent px-4 py-1.5 text-sm text-white hover:bg-accent/90 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "순서 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
      <p className="text-xs text-muted">
        위에서 {SHOWCASE}개가 메인 &quot;지금 뜨는&quot;에 나와요. 새로 고정한 게임은 순서를 저장하기 전까지 맨 아래에 붙어요.
      </p>
    </form>
  );
}
