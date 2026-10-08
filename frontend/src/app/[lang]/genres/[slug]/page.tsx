import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { GameGrid } from "@/components/GameGrid";
import { GenreChips } from "@/components/GenreChips";
import { getGames, getGenres, MAX_PAGE_SIZE } from "@/lib/api";
import { alternatesFor, getDictionary, isLocale, type Locale } from "@/lib/i18n";

// ISR: 빌드 때는 만들지 않고(빈 배열, 빌드가 백엔드에 의존하지 않게) 첫 방문 때 정적 생성 후 1시간마다 갱신.
export const revalidate = 3600;

export async function generateStaticParams() {
  return [];
}

async function findGenre(slug: string, lang: Locale) {
  const genres = await getGenres(lang);
  return { genres, genre: genres.find((g) => g.slug === slug) };
}

export async function generateMetadata({ params }: PageProps<"/[lang]/genres/[slug]">): Promise<Metadata> {
  const { lang, slug } = await params;
  if (!isLocale(lang)) return {};
  const { genre } = await findGenre(slug, lang);
  if (!genre) return {};
  return {
    title: genre.name,
    description: genre.description ?? undefined,
    alternates: alternatesFor(lang, `/genres/${genre.slug}`),
  };
}

export default async function GenrePage({ params }: PageProps<"/[lang]/genres/[slug]">) {
  const { lang, slug } = await params;
  if (!isLocale(lang)) notFound();
  const { genres, genre } = await findGenre(slug, lang);
  if (!genre) notFound();

  // MVP: 첫 48개만 보여준다 (페이지네이션은 공개 게임이 늘면 추가)
  const games = await getGames({ genre: genre.slug, sort: "POPULAR", size: MAX_PAGE_SIZE, lang });

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="font-pixel text-[22px]">{genre.name}</h1>
        {genre.description && <p className="text-muted">{genre.description}</p>}
      </header>
      <GenreChips genres={genres} locale={lang} activeSlug={genre.slug} />
      <GameGrid games={games.content} locale={lang} emptyMessage={getDictionary(lang).genre.empty} />
    </div>
  );
}
