import type { ArticleStatus, GameStatus } from "@/lib/admin/types";

const GAME_STATUS: Record<GameStatus, { label: string; className: string }> = {
  CANDIDATE: { label: "자동", className: "border-border text-muted" },
  PUBLISHED: { label: "고정 노출", className: "border-emerald-500/40 text-emerald-300" },
  HIDDEN: { label: "숨김", className: "border-zinc-500/40 text-zinc-400" },
};

/** 지금 공개 사이트에 보이는지 (자동 노출 포함) */
export function VisibilityBadge({ visible }: { visible: boolean }) {
  return visible ? (
    <span className="rounded bg-emerald-500/15 px-2 py-0.5 text-xs text-emerald-300">사이트 노출 중</span>
  ) : (
    <span className="rounded bg-zinc-500/15 px-2 py-0.5 text-xs text-zinc-400">비노출</span>
  );
}

export function AdultBadge() {
  return <span className="rounded border border-red-500/40 px-2 py-0.5 text-xs text-red-300">성인</span>;
}

export function GameStatusBadge({ status }: { status: GameStatus }) {
  const { label, className } = GAME_STATUS[status];
  return <span className={`rounded border px-2 py-0.5 text-xs ${className}`}>{label}</span>;
}

export function ArticleStatusBadge({ status }: { status: ArticleStatus | null }) {
  if (status === null) return null;
  return (
    <span className={`text-xs ${status === "PUBLISHED" ? "text-emerald-300" : "text-amber-300"}`}>
      {status === "PUBLISHED" ? "글 공개" : "글 초안"}
    </span>
  );
}
