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
import { formatReleaseDate, groupDevelopers } from "@/lib/format";

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
  return {
    title: game.title,
    description: game.article.oneLiner,
    alternates: { canonical: `/games/${game.slug}` },
    openGraph: {
      title: game.title,
      description: game.article.oneLiner,
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

  return (
    <article className="mx-auto max-w-4xl space-y-8">
      {game.headerImageUrl && (
        <div className="relative aspect-[460/215] overflow-hidden rounded-lg bg-surface">
          <Image
            src={game.headerImageUrl}
            alt=""
            fill
            priority
            sizes="(min-width: 896px) 896px, 100vw"
            className="object-cover"
          />
        </div>
      )}

      <header className="space-y-4">
        <h1 className="text-3xl font-bold">{game.title}</h1>

        {article.sponsored && (
          <aside
            aria-label="협찬 고지"
            className="flex gap-3 rounded-lg border border-amber-500/50 bg-amber-500/10 p-4 text-sm text-amber-100"
          >
            <SponsorBadge />
            <p className="whitespace-pre-line">{article.sponsorDisclosure}</p>
          </aside>
        )}

        <p className="text-lg text-foreground/90">{article.oneLiner}</p>

        <div className="flex flex-wrap gap-1.5">
          {game.coop && <CoopBadge />}
          {game.genres.map((genre) => (
            <GenreBadge key={genre.slug} genre={genre} linked />
          ))}
        </div>
      </header>

      <HighlightList items={article.highlights} />

      {(trailers.length > 0 || screenshots.length > 0) && (
        <section aria-label="미디어" className="space-y-6">
          <TrailerPlayer trailers={trailers} poster={game.headerImageUrl} title={game.title} />
          <MediaGallery screenshots={screenshots} title={game.title} />
        </section>
      )}

      <section aria-labelledby="article-title" className="space-y-4">
        <h2 id="article-title" className="text-xl font-bold">
          {article.title}
        </h2>
        <Markdown>{article.body}</Markdown>
      </section>

      <section className="space-y-2 border-t border-border pt-6 text-sm">
        {developers.length > 0 && (
          <dl className="flex flex-wrap gap-x-6 gap-y-1">
            {developers.map((dev) => (
              <div key={dev.name} className="flex gap-2">
                <dt className="text-muted">{dev.label}</dt>
                <dd>{dev.name}</dd>
              </div>
            ))}
          </dl>
        )}
        {releaseDate && (
          <p>
            <span className="text-muted">출시일</span> {releaseDate}
          </p>
        )}
      </section>

      {steamLink && (
        <a
          href={steamLink.url}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-2 rounded-lg bg-accent px-5 py-3 font-bold text-white hover:bg-accent/90"
        >
          Steam에서 보기 ↗
        </a>
      )}
    </article>
  );
}
