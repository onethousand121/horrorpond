"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { AdultBadge, CoopBadge, GenreBadge, GuideBadges, KoreanBadge } from "@/components/Badges";
import { formatCount, formatReleaseDate } from "@/lib/format";
import { getDictionary, localePath, type Locale } from "@/lib/i18n";
import type { GameSummary } from "@/lib/types";

/** 자동으로 다음 게임으로 넘기는 간격 */
const ROTATE_MS = 6000;
const STEAM_TAGS = 4;

/**
 * 인기 게임 쇼케이스: 큰 카드 1개 + 아래 썸네일 줄. 썸네일에 마우스를 올리거나 누르면 큰 카드가 바뀐다.
 * 마우스나 포커스가 안에 있는 동안, 그리고 움직임 줄이기 설정이면 자동으로 넘기지 않는다.
 */
export function PopularShowcase({ games, locale }: { games: GameSummary[]; locale: Locale }) {
  const dict = getDictionary(locale);
  const [active, setActive] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (paused || games.length < 2 || window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const timer = setInterval(() => setActive((i) => (i + 1) % games.length), ROTATE_MS);
    return () => clearInterval(timer);
  }, [paused, games.length]);

  const game = games[active];
  if (!game) return null;
  const release = formatReleaseDate(game, locale);

  return (
    <div
      className="space-y-3"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      <article className="glow-hover group relative grid overflow-hidden rounded-xl border border-border bg-surface lg:grid-cols-[3fr_2fr]">
        <div className="relative aspect-[460/215] bg-surface-2">
          {game.headerImageUrl && (
            <Image
              key={game.slug}
              src={game.headerImageUrl}
              alt=""
              fill
              priority={active === 0}
              sizes="(min-width: 1024px) 690px, 100vw"
              className={`animate-[fade-in_0.4s_ease] object-cover ${game.adult ? "scale-110 blur-xl" : ""}`}
            />
          )}
          {game.adult && (
            <div className="absolute inset-0 flex items-center justify-center">
              <AdultBadge locale={locale} large />
            </div>
          )}
          <span className="absolute top-3 left-3 rounded bg-background/85 px-2 py-0.5 font-pixel text-[11px] text-accent">
            {dict.showcase.rank(active + 1)}
          </span>
          <div className="absolute top-3 right-3 flex gap-1.5">
            <GuideBadges
              locale={locale}
              hasPlayVideo={game.hasPlayVideo}
              hasAchievementGuide={game.hasAchievementGuide}
            />
          </div>
        </div>
        <div className="flex min-w-0 flex-col gap-3 p-5">
          <h3 className="text-xl leading-snug font-bold">
            {/* 카드 전체를 클릭 영역으로 */}
            <Link href={localePath(locale, `/games/${game.slug}`)} className="after:absolute after:inset-0 group-hover:text-accent">
              {game.title}
            </Link>
          </h3>
          <p className="flex flex-wrap gap-x-3 text-xs text-muted">
            {release && <span>{release}</span>}
            {game.reviewCount != null && game.reviewCount > 0 && <span>
                {dict.card.steamReviews} {formatCount(game.reviewCount, locale)}
              </span>}
          </p>
          <div className="flex flex-wrap gap-1.5">
            <KoreanBadge locale={locale} languages={game.languages} audioLanguages={game.audioLanguages} />
            {game.coop && <CoopBadge locale={locale} />}
            {game.genres.map((genre) => (
              <GenreBadge key={genre.slug} genre={genre} locale={locale} />
            ))}
            {game.tags.slice(0, STEAM_TAGS).map((tag) => (
              <span key={tag} className="rounded-full px-1.5 py-0.5 text-xs text-muted/80">
                #{tag}
              </span>
            ))}
          </div>
          {game.picked && game.oneLiner ? (
            <p className="text-sm text-foreground/90">
              <span className="mr-1.5 rounded bg-accent-2/90 px-1.5 py-0.5 font-pixel text-[11px] text-accent-ink">
                {dict.site.pick}
              </span>
              {game.oneLiner}
            </p>
          ) : null}
          {!game.adult && game.shortDescription && (
            <p className="line-clamp-4 text-sm leading-relaxed text-muted">{game.shortDescription}</p>
          )}
        </div>
      </article>

      <div className="grid grid-cols-5 gap-2 sm:gap-3" role="group" aria-label={dict.showcase.choose}>
        {games.map((item, index) => (
          <button
            key={item.slug}
            type="button"
            aria-pressed={index === active}
            aria-label={item.title}
            onClick={() => setActive(index)}
            onMouseEnter={() => setActive(index)}
            className={`relative aspect-[460/215] overflow-hidden rounded-md border bg-surface-2 transition ${
              index === active
                ? "border-accent shadow-[0_0_12px_rgba(168,230,201,0.35)]"
                : "border-border opacity-60 hover:opacity-100"
            }`}
          >
            {item.headerImageUrl && (
              <Image
                src={item.headerImageUrl}
                alt=""
                fill
                sizes="(min-width: 1024px) 220px, 20vw"
                className={item.adult ? "scale-110 object-cover blur-md" : "object-cover"}
              />
            )}
          </button>
        ))}
      </div>
    </div>
  );
}
