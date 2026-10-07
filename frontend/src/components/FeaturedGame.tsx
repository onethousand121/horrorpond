import Image from "next/image";
import Link from "next/link";
import { CoopBadge, GenreBadge } from "@/components/Badges";
import { SponsorBadge } from "@/components/SponsorBadge";
import type { GameSummary } from "@/lib/types";

/** 홈 맨 위의 큰 카드: 가장 최근에 소개한 게임 */
export function FeaturedGame({ game, label }: { game: GameSummary; label: string }) {
  return (
    <article className="glow-hover group relative grid overflow-hidden rounded-2xl border border-border bg-surface md:grid-cols-[1.35fr_1fr]">
      <div className="relative aspect-[460/215] md:aspect-auto md:min-h-72">
        {game.headerImageUrl && (
          <Image
            src={game.headerImageUrl}
            alt=""
            fill
            priority
            sizes="(min-width: 768px) 60vw, 100vw"
            className="object-cover transition duration-700 group-hover:scale-[1.02]"
          />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-surface via-transparent to-transparent md:bg-gradient-to-l" />
      </div>
      <div className="flex flex-col gap-4 p-6 md:p-8">
        <div className="flex items-center gap-2">
          <span className="font-pixel text-[11px] text-accent">{label}</span>
          {game.sponsored && <SponsorBadge />}
        </div>
        <h2 className="text-2xl font-bold md:text-3xl">
          <Link href={`/games/${game.slug}`} className="after:absolute after:inset-0 group-hover:text-accent">
            {game.title}
          </Link>
        </h2>
        <p className="text-foreground/85">{game.oneLiner}</p>
        <ul className="space-y-2">
          {game.highlights.map((item, i) => (
            <li key={item} className="flex items-baseline gap-3 text-sm">
              <span className="font-pixel text-[11px] text-accent" aria-hidden>
                {String(i + 1).padStart(2, "0")}
              </span>
              {item}
            </li>
          ))}
        </ul>
        <div className="mt-auto flex flex-wrap items-center gap-1.5 pt-2">
          {game.coop && <CoopBadge />}
          {game.genres.map((genre) => (
            <GenreBadge key={genre.slug} genre={genre} />
          ))}
          <span className="ml-auto text-sm font-medium text-accent">추천 이유 보기 →</span>
        </div>
      </div>
    </article>
  );
}
