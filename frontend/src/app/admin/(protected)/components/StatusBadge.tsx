import type { ArticleStatus, GameStatus } from "@/lib/admin/types";

const GAME_STATUS: Record<GameStatus, { label: string; className: string }> = {
  CANDIDATE: { label: "후보", className: "border-border text-muted" },
  PUBLISHED: { label: "공개", className: "border-emerald-500/40 text-emerald-300" },
  HIDDEN: { label: "숨김", className: "border-zinc-500/40 text-zinc-400" },
};

export function GameStatusBadge({ status }: { status: GameStatus }) {
  const { label, className } = GAME_STATUS[status];
  return <span className={`rounded border px-2 py-0.5 text-xs ${className}`}>{label}</span>;
}

export function ArticleStatusBadge({ status }: { status: ArticleStatus | null }) {
  if (status === null) return <span className="text-xs text-muted">글 없음</span>;
  return (
    <span className={`text-xs ${status === "PUBLISHED" ? "text-emerald-300" : "text-amber-300"}`}>
      {status === "PUBLISHED" ? "글 공개" : "글 초안"}
    </span>
  );
}
