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
    <article className="flex flex-col overflow-hidden rounded-lg border border-border bg-surface/60">
      <Link href={`/games/${game.slug}`} className="relative block aspect-[460/215] bg-surface">
        {game.headerImageUrl && (
          <Image
            src={game.headerImageUrl}
            alt=""
            fill
            loading={eager ? "eager" : undefined}
            sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
            className="object-cover"
          />
        )}
        {game.sponsored && (
          <span className="absolute top-2 left-2">
            <SponsorBadge />
          </span>
        )}
      </Link>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <h3 className="font-bold">
          <Link href={`/games/${game.slug}`} className="hover:text-accent">
            {game.title}
          </Link>
        </h3>
        <p className="text-sm text-muted">{game.oneLiner}</p>
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
