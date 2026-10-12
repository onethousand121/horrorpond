"use client";

import Link from "next/link";
import { useActionState } from "react";
import { applyTrendingResearchAction } from "./actions";

const ACTION_STYLE: Record<string, string> = {
  고정: "text-accent",
  "고정 유지": "text-accent",
  "신규 추가": "text-accent-2",
  "고정 해제": "text-muted",
  "제외 유지": "text-muted",
  "확인 필요": "text-amber-300",
  오류: "text-red-300",
};

export function ImportForm() {
  const [state, action, pending] = useActionState(applyTrendingResearchAction, null);
  return (
    <div className="space-y-6">
      <form action={action} className="space-y-3">
        <textarea
          name="json"
          rows={12}
          required
          placeholder='{"researched_at": "...", "games": [...]}'
          className="w-full rounded border border-border bg-background px-3 py-2 font-mono text-xs"
        />
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="submit"
            disabled={pending}
            className="rounded bg-accent px-4 py-1.5 text-sm font-medium text-accent-ink disabled:opacity-50"
          >
            {pending ? "반영 중… (1분 정도 걸릴 수 있어요)" : "반영하기"}
          </button>
          {state && <span className={`text-sm ${state.ok ? "text-accent" : "text-red-300"}`}>{state.message}</span>}
        </div>
      </form>

      {state && state.rows.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-xs text-muted">
              <tr>
                <th className="py-1 pr-3">게임</th>
                <th className="py-1 pr-3">처리</th>
                <th className="py-1 pr-3">참고 영상</th>
                <th className="py-1">메모</th>
              </tr>
            </thead>
            <tbody>
              {state.rows.map((row, i) => (
                <tr key={`${row.title}-${i}`} className="border-t border-border align-top">
                  <td className="py-1.5 pr-3">
                    {row.gameId ? (
                      <Link href={`/admin/games/${row.gameId}`} className="hover:text-accent">
                        {row.title}
                      </Link>
                    ) : (
                      row.title
                    )}
                  </td>
                  <td className={`py-1.5 pr-3 whitespace-nowrap ${ACTION_STYLE[row.action] ?? ""}`}>{row.action}</td>
                  <td className="py-1.5 pr-3 whitespace-nowrap text-muted">{row.video}</td>
                  <td className="py-1.5 text-muted">{row.note}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {state && state.reviewQueue.length > 0 && (
        <section className="space-y-1">
          <h2 className="text-sm font-semibold">확인 필요 (반영 안 함)</h2>
          <ul className="list-disc pl-5 text-sm text-muted">
            {state.reviewQueue.map((item, i) => (
              <li key={`${item.title}-${i}`}>
                {item.title} — {item.reason}
              </li>
            ))}
          </ul>
        </section>
      )}
      {state && state.warnings.length > 0 && (
        <ul className="list-disc pl-5 text-xs text-muted">
          {state.warnings.map((w, i) => (
            <li key={i}>{w}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
