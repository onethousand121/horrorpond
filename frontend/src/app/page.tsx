import Link from "next/link";
import { connection } from "next/server";
import { CuratorNote } from "@/components/CuratorNote";
import { FeaturedGame } from "@/components/FeaturedGame";
import { GameGrid } from "@/components/GameGrid";
import { GenreChips } from "@/components/GenreChips";
import { getGames, getGenres } from "@/lib/api";
import { CURATOR, SITE_TAGLINE } from "@/lib/site";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

/** 대표 1개 + 그리드 9개 */
const RECENT_COUNT = 10;

export default async function HomePage() {
  await connection();
  const [recent, genres] = await Promise.all([getGames({ size: RECENT_COUNT }), getGenres()]);
  const [featured, ...rest] = recent.content;

  return (
    <div className="space-y-14">
      <section className="space-y-5 pt-4">
        <h1 className="text-3xl leading-tight font-bold sm:text-4xl">
          {SITE_TAGLINE}을
          <br />
          <span className="text-accent">건져 올립니다</span>
        </h1>
        <p className="max-w-2xl text-muted">
          큐레이터 {CURATOR.name}이 직접 플레이하고 고른 게임만 소개합니다. 점수 대신, 이 게임을 해야 하는 이유를
          적었습니다.
        </p>
        <GenreChips genres={genres} />
      </section>

      {featured ? (
        <FeaturedGame game={featured} label="새로 건져 올린 게임" />
      ) : (
        <p className="rounded-xl border border-border p-8 text-center text-muted">아직 소개된 게임이 없습니다.</p>
      )}

      {rest.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-baseline justify-between">
            <h2 className="text-xl font-bold">최근 소개한 게임</h2>
            <Link href="/games" className="text-sm text-muted hover:text-accent">
              전체 보기 →
            </Link>
          </div>
          <GameGrid games={rest} />
        </section>
      )}

      <Link
        href="/coop"
        className="flex items-center justify-between gap-4 rounded-2xl border border-sky-400/25 bg-gradient-to-r from-sky-400/10 to-transparent p-6 hover:border-sky-400/50"
      >
        <span>
          <span className="block text-lg font-bold">혼자는 무섭다면</span>
          <span className="text-sm text-muted">친구와 함께 비명 지르기 좋은 협동 공포게임</span>
        </span>
        <span className="text-sky-300">→</span>
      </Link>

      <CuratorNote />
    </div>
  );
}
