import Image from "next/image";
import Link from "next/link";
import { CoopBadge, GenreBadge } from "@/components/Badges";
import { HighlightList } from "@/components/HighlightList";
import { SponsorBadge } from "@/components/SponsorBadge";
import { formatCount, formatReleaseDate } from "@/lib/format";
import type { GameSummary } from "@/lib/types";

const CARD_HIGHLIGHTS = 3;
const CARD_GENRES = 2;

/**
 * 허브 카드: 출시일·리뷰 수·장르가 기본, 재일 추천 게임이면 한 줄 소개와 장점을 더 보여준다.
 * @param eager 화면 첫 줄 카드(LCP 후보)는 지연 로딩하지 않는다.
 */
export function GameCard({ game, eager = false }: { game: GameSummary; eager?: boolean }) {
  const release = formatReleaseDate(game);
  return (
    <article className="glow-hover group relative flex flex-col overflow-hidden rounded-xl border border-border bg-surface hover:-translate-y-0.5">
      <div className="relative aspect-[460/215] bg-surface-2">
        {game.headerImageUrl && (
          <Image
            src={game.headerImageUrl}
            alt=""
            fill
            loading={eager ? "eager" : undefined}
            sizes="(min-width: 1024px) 25vw, (min-width: 640px) 50vw, 100vw"
            className="object-cover transition duration-500 group-hover:scale-[1.03]"
          />
        )}
        <div className="absolute top-2 left-2 flex gap-1.5">
          {game.comingSoon && (
            <span className="rounded bg-background/85 px-1.5 py-0.5 font-pixel text-[11px] text-accent">출시 예정</span>
          )}
          {game.picked && (
            <span className="rounded bg-accent-2/90 px-1.5 py-0.5 font-pixel text-[11px] text-accent-ink">재일 추천</span>
          )}
          {game.sponsored && <SponsorBadge />}
        </div>
      </div>
      <div className="flex flex-1 flex-col gap-2 p-3.5">
        <h3 className="leading-snug font-bold">
          {/* 카드 전체를 클릭 영역으로 */}
          <Link href={`/games/${game.slug}`} className="after:absolute after:inset-0 group-hover:text-accent">
            {game.title}
          </Link>
        </h3>
        <p className="flex flex-wrap gap-x-3 text-xs text-muted">
          {release && <span>{release}</span>}
          {game.reviewCount != null && game.reviewCount > 0 && <span>리뷰 {formatCount(game.reviewCount)}</span>}
        </p>
        {game.picked && game.oneLiner && <p className="text-sm text-foreground/85">{game.oneLiner}</p>}
        {game.picked && <HighlightList items={game.highlights.slice(0, CARD_HIGHLIGHTS)} compact />}
        <div className="mt-auto flex flex-wrap gap-1.5 pt-1">
          {game.coop && <CoopBadge />}
          {game.genres.slice(0, CARD_GENRES).map((genre) => (
            <GenreBadge key={genre.slug} genre={genre} />
          ))}
        </div>
      </div>
    </article>
  );
}
