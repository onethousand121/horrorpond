import Image from "next/image";
import Link from "next/link";
import { CoopBadge, GenreBadge } from "@/components/Badges";
import { HighlightList } from "@/components/HighlightList";
import { SponsorBadge } from "@/components/SponsorBadge";
import type { GameSummary } from "@/lib/types";

const CARD_HIGHLIGHTS = 3;

/**
 * @param eager 화면 첫 줄 카드(LCP 후보)는 지연 로딩하지 않는다.
 */
export function GameCard({ game, eager = false }: { game: GameSummary; eager?: boolean }) {
  return (
    <article className="group relative flex flex-col overflow-hidden rounded-xl border border-border bg-surface transition hover:-translate-y-0.5 hover:border-accent/40">
      <div className="relative aspect-[460/215] bg-surface-2">
        {game.headerImageUrl && (
          <Image
            src={game.headerImageUrl}
            alt=""
            fill
            loading={eager ? "eager" : undefined}
            sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
            className="object-cover transition duration-500 group-hover:scale-[1.03]"
          />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-surface via-transparent to-transparent" />
        {game.sponsored && (
          <span className="absolute top-2 left-2">
            <SponsorBadge />
          </span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-2.5 p-4 pt-1">
        <h3 className="text-[17px] font-bold">
          {/* 카드 전체를 클릭 영역으로 */}
          <Link href={`/games/${game.slug}`} className="after:absolute after:inset-0 group-hover:text-accent">
            {game.title}
          </Link>
        </h3>
        <p className="text-sm leading-relaxed text-muted">{game.oneLiner}</p>
        <HighlightList items={game.highlights.slice(0, CARD_HIGHLIGHTS)} compact />
        <div className="mt-auto flex flex-wrap gap-1.5 pt-2">
          {game.coop && <CoopBadge />}
          {game.genres.map((genre) => (
            <GenreBadge key={genre.slug} genre={genre} />
          ))}
        </div>
      </div>
    </article>
  );
}
