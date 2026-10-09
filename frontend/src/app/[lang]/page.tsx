import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { GameRow } from "@/components/GameRow";
import { PopularShowcase } from "@/components/PopularShowcase";
import { VideoRow } from "@/components/VideoRow";
import { getGames, getGenres, getStats } from "@/lib/api";
import { alternatesFor, getDictionary, isLocale, localePath } from "@/lib/i18n";
import { CURATOR } from "@/lib/site";
import { getFreshVideos } from "@/lib/youtube";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드·유튜브 호출은 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

export async function generateMetadata({ params }: PageProps<"/[lang]">): Promise<Metadata> {
  const { lang } = await params;
  return isLocale(lang) ? { alternates: alternatesFor(lang, "/") } : {};
}

const SHOWCASE = 5;
const ROW = 8;
const VIDEOS = 4;
/** 마지막 영상이 이보다 오래되면 영상 줄을 숨긴다 */
const VIDEO_FRESH_DAYS = 60;

function SectionHeader({ title, href, more }: { title: string; href: string; more: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <h2 className="font-pixel text-[22px]">{title}</h2>
      <Link href={href} className="shrink-0 text-sm text-muted hover:text-accent">
        {more} →
      </Link>
    </div>
  );
}

const CHIP = "rounded-full border px-3 py-1 text-sm";

export default async function HomePage({ params }: PageProps<"/[lang]">) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  await connection();
  const dict = getDictionary(lang);
  const t = dict.home;
  const [stats, genres, trending, recent, upcoming, steady, videos] = await Promise.all([
    getStats(),
    getGenres(lang),
    // 지금 뜨는: 최근 90일 출시작을 하루 평균 리뷰 수로. 누적 리뷰 수(스테디셀러)는 바로 아래 줄로 따로 보여준다
    // 배포 순서상 백엔드가 TRENDING을 모르는 몇 분 동안은 누적 인기순으로 대신 보여준다
    getGames({ sort: "TRENDING", size: SHOWCASE, lang }).catch(() => getGames({ sort: "POPULAR", size: SHOWCASE, lang })),
    getGames({ release: "RECENT", sort: "RELEASE", size: ROW, lang }),
    getGames({ release: "UPCOMING", size: ROW, lang }),
    getGames({ sort: "POPULAR", size: ROW, lang }),
    getFreshVideos(VIDEOS, VIDEO_FRESH_DAYS),
  ]);

  return (
    <div className="space-y-12">
      <section className="space-y-4 pt-2">
        <h1 className="font-pixel text-[22px] leading-snug sm:text-[30px]">
          {t.titleLead} <span className="whitespace-nowrap text-accent">{t.titleAccent}</span>
        </h1>
        <p className="max-w-2xl text-muted">
          {t.intro1}
          <br className="hidden sm:block" /> {t.intro2}
        </p>
        <ul aria-label={t.statsLabel} className="flex flex-wrap gap-x-5 gap-y-1 font-pixel text-[11px] text-muted">
          {[
            // 오늘 출시는 최근 출시순 맨 앞, 내일 출시는 출시 예정(가까운 순) 맨 앞에 나온다
            { label: t.statToday, value: stats.releasedToday, href: "/games?view=recent" },
            { label: t.statTomorrow, value: stats.releasingTomorrow, href: "/games?view=upcoming" },
          ]
            // 백엔드보다 프론트가 먼저 배포되는 동안 예전 응답이면 숫자를 숨긴다
            .filter(({ value }) => typeof value === "number")
            .map(({ label: [before, after], value, href }) => (
              <li key={href}>
                <Link href={localePath(lang, href)} className="hover:text-foreground">
                  {before}
                  <strong className="text-[13px] font-normal text-accent">{value.toLocaleString(dict.htmlLang)}</strong>
                  {after}
                </Link>
              </li>
            ))}
        </ul>
        <nav aria-label={t.quickFilters} className="flex flex-wrap gap-2">
          <Link
            href={localePath(lang, "/coop")}
            className={`${CHIP} border-accent-2/50 bg-accent-2/15 font-medium text-accent-2 hover:bg-accent-2/25`}
          >
            {t.coopOnly}
          </Link>
          {lang === "ko" && (
            <Link
              href={localePath(lang, "/games?korean=1")}
              className={`${CHIP} border-accent/40 bg-accent/10 font-medium text-accent hover:bg-accent/20`}
            >
              {t.koreanOnly}
            </Link>
          )}
          {genres.map((genre) => (
            <Link
              key={genre.slug}
              href={localePath(lang, `/genres/${genre.slug}`)}
              className={`${CHIP} border-border bg-surface/60 text-muted hover:border-accent/50 hover:text-foreground`}
            >
              {genre.name}
            </Link>
          ))}
        </nav>
      </section>

      {trending.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={t.trending} href={localePath(lang, "/games")} more={t.more} />
          <PopularShowcase games={trending.content} locale={lang} />
        </section>
      )}

      {steady.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={t.steady} href={localePath(lang, "/games?view=popular")} more={t.more} />
          <GameRow games={steady.content} locale={lang} />
        </section>
      )}

      {recent.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={t.recent} href={localePath(lang, "/games?view=recent")} more={t.more} />
          <GameRow games={recent.content} locale={lang} />
        </section>
      )}

      {upcoming.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={t.upcoming} href={localePath(lang, "/games?view=upcoming")} more={t.more} />
          <GameRow games={upcoming.content} locale={lang} />
        </section>
      )}

      {videos.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={t.videos(dict.site.curatorName)} href={CURATOR.youtubeUrl} more={t.channel} />
          <VideoRow videos={videos} locale={lang} />
        </section>
      )}
    </div>
  );
}
