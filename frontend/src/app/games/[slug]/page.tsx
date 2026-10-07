import type { Metadata } from "next";
import Image from "next/image";
import { notFound } from "next/navigation";
import { CoopBadge, GenreBadge } from "@/components/Badges";
import { HighlightList } from "@/components/HighlightList";
import { Markdown } from "@/components/Markdown";
import { MediaGallery } from "@/components/MediaGallery";
import { SponsorBadge } from "@/components/SponsorBadge";
import { TrailerPlayer } from "@/components/TrailerPlayer";
import { getGame } from "@/lib/api";
import { formatCount, formatReleaseDate, groupDevelopers } from "@/lib/format";
import { CURATOR } from "@/lib/site";

// ISR: 빌드 때는 만들지 않고(빈 배열) 첫 방문 때 정적 생성 후 1시간마다 갱신.
// 없는 slug는 notFound()로 실제 404 상태 코드를 돌려준다.
export const revalidate = 3600;

export async function generateStaticParams() {
  return [];
}

export async function generateMetadata({ params }: PageProps<"/games/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const game = await getGame(slug);
  if (!game) return {};
  const description = game.article?.oneLiner ?? game.shortDescription ?? undefined;
  return {
    title: game.title,
    description,
    alternates: { canonical: `/games/${game.slug}` },
    openGraph: {
      title: game.title,
      description,
      images: game.headerImageUrl ? [game.headerImageUrl] : [],
      type: "article",
    },
  };
}

export default async function GameDetailPage({ params }: PageProps<"/games/[slug]">) {
  const { slug } = await params;
  const game = await getGame(slug);
  if (!game) notFound();

  const { article } = game;
  const trailers = game.media.filter((m) => m.type === "TRAILER");
  const screenshots = game.media.filter((m) => m.type === "SCREENSHOT");
  const developers = groupDevelopers(game.developers);
  const releaseDate = formatReleaseDate(game);
  const steamLink = game.storeLinks.find((link) => link.store === "STEAM");

  // 같은 그림이 두 번 보이지 않게: 헤더 이미지(상단 히어로)와 첫 스크린샷(갤러리 큰 화면)을 피해 포스터를 고른다
  const trailerPoster = (screenshots[1] ?? screenshots[0])?.url ?? game.headerImageUrl;

  return (
    <article className="space-y-10">
      <header className="relative -mx-4 overflow-hidden sm:mx-0 sm:rounded-2xl">
        <div className="relative aspect-[460/215] max-h-[420px] w-full bg-surface sm:aspect-[21/8]">
          {game.headerImageUrl && (
            <Image
              src={game.headerImageUrl}
              alt=""
              fill
              priority
              sizes="(min-width: 1152px) 1152px, 100vw"
              className="object-cover"
            />
          )}
          <div className="absolute inset-0 bg-gradient-to-t from-background via-background/40 to-transparent" />
        </div>
        <div className="relative -mt-10 space-y-3 px-4 sm:-mt-28 sm:px-8">
          <div className="flex flex-wrap gap-1.5">
            {game.coop && <CoopBadge />}
            {game.genres.map((genre) => (
              <GenreBadge key={genre.slug} genre={genre} linked />
            ))}
          </div>
          <h1 className="text-3xl font-bold drop-shadow sm:text-5xl">{game.title}</h1>
          {article?.oneLiner && <p className="max-w-3xl text-lg text-foreground/90">{article.oneLiner}</p>}
        </div>
      </header>

      <div className="grid gap-10 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="min-w-0 space-y-10">
          {article ? (
            <section aria-label={`${CURATOR.name} 추천`} className="space-y-6">
              <p className="inline-block rounded bg-accent-2/90 px-2 py-0.5 font-pixel text-[11px] text-accent-ink">
                {CURATOR.name} 추천
              </p>
              {article.sponsored && (
                <aside
                  aria-label="협찬 고지"
                  className="flex gap-3 rounded-xl border border-amber-500/50 bg-amber-500/10 p-4 text-sm text-amber-100"
                >
                  <SponsorBadge />
                  <p className="whitespace-pre-line">{article.sponsorDisclosure}</p>
                </aside>
              )}
              <HighlightList items={article.highlights} />
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
              <section aria-label="게임 소개" className="space-y-2">
                <h2 className="font-pixel text-[11px] text-muted">게임 소개 (Steam)</h2>
                <p className="leading-relaxed text-foreground/90">{game.shortDescription}</p>
              </section>
            )
          )}

          {(trailers.length > 0 || screenshots.length > 0) && (
            <section aria-label="영상과 스크린샷" className="space-y-4">
              <hr className="divider" />
              <h2 className="font-pixel text-[22px]">영상과 스크린샷</h2>
              <TrailerPlayer trailers={trailers} poster={trailerPoster} title={game.title} />
              <MediaGallery screenshots={screenshots} title={game.title} />
            </section>
          )}
        </div>

        <aside className="space-y-4 lg:sticky lg:top-20 lg:self-start">
          <section className="space-y-4 rounded-2xl border border-border bg-surface p-5 text-sm">
            <h2 className="font-pixel text-[11px] text-muted">게임 정보</h2>
            <dl className="space-y-2">
              {releaseDate && (
                <div className="flex justify-between gap-4">
                  <dt className="text-muted">출시일</dt>
                  <dd>{releaseDate}</dd>
                </div>
              )}
              {game.reviewCount != null && game.reviewCount > 0 && (
                <div className="flex justify-between gap-4">
                  <dt className="text-muted">Steam 리뷰</dt>
                  <dd>{formatCount(game.reviewCount)}개</dd>
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
                Steam에서 보기 ↗
              </a>
            )}
          </section>
        </aside>
      </div>
    </article>
  );
}
