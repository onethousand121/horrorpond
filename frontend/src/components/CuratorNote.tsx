import Image from "next/image";
import { CURATOR } from "@/lib/site";

/** 큐레이터 소개. 사람이 직접 고른다는 신뢰의 근거라 홈과 상세에 함께 둔다. */
export function CuratorNote({ compact = false }: { compact?: boolean }) {
  return (
    <aside
      className={`flex flex-col gap-4 rounded-2xl border border-border bg-surface ${compact ? "p-4" : "p-6 sm:flex-row sm:items-center"}`}
    >
      <div className="flex items-center gap-3">
        <div className="grid size-14 shrink-0 place-items-end overflow-hidden rounded-full border border-accent/40 bg-surface-2">
          <Image src="/brand/frog-cup.webp" alt="" width={36} height={48} className="h-12 w-auto translate-y-1" />
        </div>
        <div>
          <p className="font-pixel text-[11px] text-muted">큐레이터</p>
          <p className="font-bold">{CURATOR.name}</p>
        </div>
      </div>
      <p className={`text-sm leading-relaxed text-foreground/85 ${compact ? "" : "sm:flex-1"}`}>
        공포게임 유튜버. 직접 플레이한 게임 중 끝까지 붙잡아 둔 것만 건져 올립니다. 점수 대신 이 게임을 해야 하는
        이유를 적습니다.
      </p>
      <a
        href={CURATOR.youtubeUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="glow-hover inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-danger/50 bg-danger/10 px-4 py-2 text-sm font-medium text-red-200"
      >
        ▶ YouTube 채널
      </a>
    </aside>
  );
}
