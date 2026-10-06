import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { GameGrid } from "@/components/GameGrid";
import { GenreChips } from "@/components/GenreChips";
import { getGames, getGenres, MAX_PAGE_SIZE } from "@/lib/api";

// ISR: 장르는 빌드 때 모두 생성하고 1시간마다 갱신. 큐레이터가 새 장르를 추가하면 첫 방문 때 생성된다.
export const revalidate = 3600;

export async function generateStaticParams() {
  const genres = await getGenres();
  return genres.map((genre) => ({ slug: genre.slug }));
}

async function findGenre(slug: string) {
  const genres = await getGenres();
  return { genres, genre: genres.find((g) => g.slug === slug) };
}

export async function generateMetadata({ params }: PageProps<"/genres/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const { genre } = await findGenre(slug);
  if (!genre) return {};
  return { title: genre.name, description: genre.description ?? undefined };
}

export default async function GenrePage({ params }: PageProps<"/genres/[slug]">) {
  const { slug } = await params;
  const { genres, genre } = await findGenre(slug);
  if (!genre) notFound();

  // MVP: 첫 48개만 보여준다 (페이지네이션은 공개 게임이 늘면 추가)
  const games = await getGames({ genre: genre.slug, size: MAX_PAGE_SIZE });

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="text-2xl font-bold">{genre.name}</h1>
        {genre.description && <p className="text-muted">{genre.description}</p>}
      </header>
      <GenreChips genres={genres} activeSlug={genre.slug} />
      <GameGrid games={games.content} emptyMessage="이 장르에 소개된 게임이 아직 없습니다." />
    </div>
  );
}
