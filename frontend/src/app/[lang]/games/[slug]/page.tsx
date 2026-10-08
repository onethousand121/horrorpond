import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AchievementGuides } from "@/components/AchievementGuides";
import { AdultBadge, CoopBadge, GenreBadge } from "@/components/Badges";
import { HighlightList } from "@/components/HighlightList";
import { Markdown } from "@/components/Markdown";
import { MediaGallery } from "@/components/MediaGallery";
import { PlayVideos } from "@/components/PlayVideos";
import { SponsorBadge } from "@/components/SponsorBadge";
import { TrailerPlayer } from "@/components/TrailerPlayer";
import { getGame } from "@/lib/api";
import { formatCount, formatReleaseDate, groupDevelopers } from "@/lib/format";
import { alternatesFor, getDictionary, isLocale, localePath, type Locale } from "@/lib/i18n";
import { SITE_URL } from "@/lib/site";
import type { GameDetail } from "@/lib/types";

// ISR: 빌드 때는 만들지 않고(빈 배열) 첫 방문 때 정적 생성 후 1시간마다 갱신.
// 없는 slug는 notFound()로 실제 404 상태 코드를 돌려준다.
export const revalidate = 3600;

/** schema.org VideoGame: 검색 결과에 게임 정보(장르, 출시일, 개발사)를 알려준다 */
function videoGameJsonLd(game: GameDetail, lang: Locale) {
  return {
    "@context": "https://schema.org",
    "@type": "VideoGame",
    name: game.title,
    url: `${SITE_URL}${localePath(lang, `/games/${game.slug}`)}`,
    inLanguage: lang,
    ...(game.headerImageUrl && { image: game.headerImageUrl }),
    ...(game.shortDescription && { description: game.shortDescription }),
    ...(game.releaseDate && { datePublished: game.releaseDate }),
    genre: ["Horror", ...game.genres.map((genre) => genre.name)],
    gamePlatform: "PC",
    playMode: game.coop ? ["SinglePlayer", "CoOp"] : "SinglePlayer",
    author: game.developers
      .filter((d) => d.role === "DEVELOPER")
      .map((d) => ({ "@type": "Organization", name: d.name })),
    publisher: game.developers
      .filter((d) => d.role === "PUBLISHER")
      .map((d) => ({ "@type": "Organization", name: d.name })),
    ...(game.playVideos.length > 0 && {
      subjectOf: game.playVideos.map((video) => ({
        "@type": "VideoObject",
        name: video.title ?? game.title,
        embedUrl: `https://www.youtube.com/embed/${video.youtubeId}`,
        thumbnailUrl: `https://i.ytimg.com/vi/${video.youtubeId}/hqdefault.jpg`,
      })),
    }),
  };
}

/** 상세 페이지에는 업적 공략을 앞에서 몇 개만 보여주고 전용 페이지로 잇는다 */
const PREVIEW_ACHIEVEMENTS = 5;

export async function generateStaticParams() {
  return [];
}

export async function generateMetadata({ params }: PageProps<"/[lang]/games/[slug]">): Promise<Metadata> {
  const { lang, slug } = await params;
  if (!isLocale(lang)) return {};
  const game = await getGame(slug, lang);
  if (!game) return {};
  // 성인 게임은 소개·이미지를 싣지 않고 검색엔진에도 내보내지 않는다
  if (game.adult) {
    return { title: game.title, description: getDictionary(lang).adult.notice, robots: { index: false, follow: false } };
  }
  const description = game.article?.oneLiner ?? game.shortDescription ?? undefined;
  return {
    title: game.title,
    description,
    alternates: alternatesFor(lang, `/games/${game.slug}`),
    openGraph: {
      title: game.title,
      description,
      images: game.headerImageUrl ? [game.headerImageUrl] : [],
      type: "article",
    },
  };
}

export default async function GameDetailPage({ params }: PageProps<"/[lang]/games/[slug]">) {
  const { lang, slug } = await params;
  if (!isLocale(lang)) notFound();
  const game = await getGame(slug, lang);
  if (!game) notFound();
  const dict = getDictionary(lang);
  const t = dict.detail;

  const { article } = game;
  const trailers = game.media.filter((m) => m.type === "TRAILER");
  const screenshots = game.media.filter((m) => m.type === "SCREENSHOT");
  const developers = groupDevelopers(game.developers, lang);
  const releaseDate = formatReleaseDate(game, lang);
  const steamLink = game.storeLinks.find((link) => link.store === "STEAM");

  // 같은 그림이 두 번 보이지 않게: 헤더 이미지(상단 히어로)와 첫 스크린샷(갤러리 큰 화면)을 피해 포스터를 고른다
  const trailerPoster = (screenshots[1] ?? screenshots[0])?.url ?? game.headerImageUrl;

  return (
    <article className="space-y-10">
      {!game.adult && (
        <script
          type="application/ld+json"
          // 검색엔진용 구조화 데이터. "<"를 이스케이프해 스크립트 태그가 닫히지 않게 한다
          dangerouslySetInnerHTML={{ __html: JSON.stringify(videoGameJsonLd(game, lang)).replace(/</g, "\\u003c") }}
        />
      )}
      <header className="relative -mx-4 overflow-hidden sm:mx-0 sm:rounded-2xl">
        <div className="relative aspect-[460/215] max-h-[420px] w-full bg-surface sm:aspect-[21/8]">
          {game.headerImageUrl && (
            <Image
              src={game.headerImageUrl}
              alt=""
              fill
              priority
              sizes="(min-width: 1152px) 1152px, 100vw"
              className={game.adult ? "scale-110 object-cover blur-2xl" : "object-cover"}
            />
          )}
          {game.adult && (
            <div className="absolute inset-0 flex items-center justify-center">
              <AdultBadge locale={lang} large />
            </div>
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" />
        </div>
        <div className="relative -mt-10 space-y-3 px-4 sm:-mt-28 sm:px-8">
          <div className="flex flex-wrap gap-1.5">
            {game.coop && <CoopBadge locale={lang} />}
            {game.genres.map((genre) => (
              <GenreBadge key={genre.slug} genre={genre} locale={lang} linked />
            ))}
          </div>
          <h1 className="text-3xl font-bold drop-shadow sm:text-5xl">{game.title}</h1>
          {!game.adult && article?.oneLiner && <p className="max-w-3xl text-lg text-foreground/90">{article.oneLiner}</p>}
        </div>
      </header>

      <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="min-w-0 space-y-10">
          {game.adult ? (
            <section aria-label={dict.adult.title} className="space-y-2 rounded-xl border border-red-400/40 bg-red-500/10 p-5">
              <h2 className="font-pixel text-[11px] text-red-300">{dict.adult.title}</h2>
              <p className="leading-relaxed text-foreground/90">{dict.adult.notice}</p>
            </section>
          ) : (
            <>
              {article ? (
                <section aria-label={t.pickLabel} className="space-y-6">
                  <p className="inline-block rounded bg-accent-2/90 px-2 py-0.5 font-pixel text-[11px] text-accent-ink">
                    {t.pickLabel}
                  </p>
                  {t.articleKoreanOnly && <p className="text-sm text-muted">{t.articleKoreanOnly}</p>}
                  {article.sponsored && (
                    <aside
                      aria-label={t.sponsorNotice}
                      className="flex gap-3 rounded-xl border border-amber-500/50 bg-amber-500/10 p-4 text-sm text-amber-100"
                    >
                      <SponsorBadge locale={lang} />
                      <p className="whitespace-pre-line">{article.sponsorDisclosure}</p>
                    </aside>
                  )}
                  <HighlightList items={article.highlights} locale={lang} />
                  <div className="space-y-4">
                    <h2 id="article-title" className="font-pixel text-[22px] leading-snug">
                      {article.title}
                    </h2>
                    <Markdown>{article.body}</Markdown>
                  </div>
                  {game.shortDescription && (
                    <p className="border-l-2 border-border pl-4 text-sm text-muted">{game.shortDescription}</p>
                  )}
                </section>
              ) : (
                game.shortDescription && (
                  <section aria-label={t.about} className="space-y-2">
                    <h2 className="font-pixel text-[11px] text-muted">{t.aboutSteam}</h2>
                    <p className="leading-relaxed text-foreground/90">{game.shortDescription}</p>
                  </section>
                )
              )}

              {/* 플레이 영상·업적 공략은 있을 때만 보인다 */}
              {game.playVideos.length > 0 && (
                <section aria-label={dict.guide.playVideos} className="space-y-4">
                  <hr className="divider" />
                  <h2 className="font-pixel text-[22px]">{dict.guide.playVideos}</h2>
                  <PlayVideos videos={game.playVideos} locale={lang} />
                </section>
              )}

              {game.achievements.length > 0 && (
                <section aria-label={dict.guide.achievements} className="space-y-4">
                  <hr className="divider" />
                  <h2 className="flex items-baseline gap-3 font-pixel text-[22px]">
                    {dict.guide.achievements}
                    <span className="font-sans text-sm font-normal text-muted">
                      {dict.guide.achievementCount(game.achievements.length)}
                    </span>
                  </h2>
                  <AchievementGuides achievements={game.achievements.slice(0, PREVIEW_ACHIEVEMENTS)} locale={lang} />
                  <Link
                    href={localePath(lang, `/games/${game.slug}/achievements`)}
                    className="inline-block text-sm text-accent hover:underline"
                  >
                    {dict.guide.seeAllAchievements(game.achievements.length)}
                  </Link>
                </section>
              )}

              {(trailers.length > 0 || screenshots.length > 0) && (
                <section aria-label={t.media} className="space-y-4">
                  <hr className="divider" />
                  <h2 className="font-pixel text-[22px]">{t.media}</h2>
                  <TrailerPlayer trailers={trailers} poster={trailerPoster} title={game.title} locale={lang} />
                  <MediaGallery screenshots={screenshots} title={game.title} locale={lang} />
                </section>
              )}
            </>
          )}
        </div>

        <aside className="space-y-4 lg:sticky lg:top-20 lg:self-start">
          <section className="space-y-4 rounded-2xl border border-border bg-surface p-5 text-sm">
            <h2 className="font-pixel text-[11px] text-muted">{t.info}</h2>
            <dl className="space-y-2">
              {releaseDate && (
                <div className="flex justify-between gap-4">
                  <dt className="text-muted">{t.releaseDate}</dt>
                  <dd>{releaseDate}</dd>
                </div>
              )}
              {game.reviewCount != null && game.reviewCount > 0 && (
                <div className="flex justify-between gap-4">
                  <dt className="text-muted">{t.steamReviews}</dt>
                  <dd>{t.reviewCount(formatCount(game.reviewCount, lang))}</dd>
                </div>
              )}
              {developers.map((dev) => (
                <div key={dev.name} className="flex justify-between gap-4">
                  <dt className="shrink-0 text-muted">{dev.label}</dt>
                  <dd className="text-right">{dev.name}</dd>
                </div>
              ))}
            </dl>
            {steamLink && (
              <a
                href={steamLink.url}
                target="_blank"
                rel="noopener noreferrer"
                className="glow-hover flex items-center justify-center gap-2 rounded-lg bg-accent px-4 py-3 font-bold text-accent-ink"
              >
                {t.viewOnSteam}
              </a>
            )}
          </section>
        </aside>
      </div>
    </article>
  );
}
