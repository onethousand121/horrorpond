import Image from "next/image";
import Link from "next/link";
import { ContentBadges, FreeBadge, GenreBadge, KoreanBadge, KoreanPatchBadge } from "@/components/Badges";
import { formatReleaseDate } from "@/lib/format";
import { getDictionary, localePath, type Locale } from "@/lib/i18n";
import { textLang } from "@/lib/textLang";
import type { DailyPick } from "@/lib/types";

/**
 * 오늘의 게임: 하루 한 게임을 큰 카드로. 큐레이터의 한 줄 소개가 중심이고, 개발사 제공 게임은 협찬 뱃지를 붙인다.
 * 성인 게임은 백엔드가 고르지 않는다.
 */
export function DailyPickSpotlight({ pick, locale }: { pick: DailyPick; locale: Locale }) {
  const dict = getDictionary(locale);
  const t = dict.home;
  const { game } = pick;
  const release = formatReleaseDate(game, locale);
  return (
    <section aria-label={t.dailyPick} className="space-y-4">
      <div className="flex items-baseline gap-3">
        <h2 className="font-pixel text-[22px]">{t.dailyPick}</h2>
        <span className="text-sm text-muted">{t.dailyPickDate(pick.date)}</span>
      </div>
      <article className="glow-hover group relative grid overflow-hidden rounded-xl border border-accent/40 bg-surface md:grid-cols-[1fr_1fr]">
        <div className="relative aspect-[460/215] bg-surface-2">
          {game.headerImageUrl && (
            <Image
              src={game.headerImageUrl}
              alt={game.title}
              fill
              sizes="(min-width: 768px) 560px, 100vw"
              className="object-cover"
            />
          )}
          <div className="absolute top-3 right-3 flex flex-wrap justify-end gap-1.5">
            <ContentBadges
              locale={locale}
              sponsored={pick.sponsored || game.sponsored}
              picked={game.picked}
              reviewed={game.reviewed}
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
          {pick.intro && (
            <p lang={textLang(pick.intro)} className="border-l-2 border-accent-2/60 pl-3 text-base text-foreground/90">
              {pick.intro}
            </p>
          )}
          {release && <p className="text-xs text-muted">{release}</p>}
          <div className="flex flex-wrap gap-1.5">
            <KoreanBadge locale={locale} languages={game.languages} />
            <KoreanPatchBadge locale={locale} koreanPatch={game.koreanPatch} languages={game.languages} />
            <FreeBadge locale={locale} game={game} />
            {game.genres.map((genre) => (
              <GenreBadge key={genre.slug} genre={genre} locale={locale} />
            ))}
          </div>
          {game.shortDescription && (
            <p className="line-clamp-3 text-sm leading-relaxed text-muted">{game.shortDescription}</p>
          )}
          <span aria-hidden className="mt-auto text-sm text-accent">{t.dailyPickMore} →</span>
        </div>
      </article>
    </section>
  );
}
