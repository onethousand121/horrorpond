import Link from "next/link";
import { connection } from "next/server";
import { GameGrid } from "@/components/GameGrid";
import { GenreChips } from "@/components/GenreChips";
import { getGames, getGenres } from "@/lib/api";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

const RECENT_COUNT = 12;

export default async function HomePage() {
  await connection();
  const [recent, genres] = await Promise.all([getGames({ size: RECENT_COUNT }), getGenres()]);

  return (
    <div className="space-y-12">
      <section className="space-y-4">
        <h1 className="text-2xl font-bold sm:text-3xl">큐레이터가 직접 고른 공포게임</h1>
        <p className="max-w-2xl text-muted">
          점수 대신, 이 게임을 해야 하는 이유를 적었습니다. 선별되었다는 것 자체가 추천입니다.
        </p>
        <GenreChips genres={genres} />
        <Link
          href="/coop"
          className="inline-flex items-center gap-2 rounded-lg border border-sky-500/40 bg-sky-500/10 px-4 py-2 text-sm font-medium text-sky-200 hover:bg-sky-500/20"
        >
          친구와 함께 무서워하기 → 협동 공포게임
        </Link>
      </section>

      <section className="space-y-4">
        <div className="flex items-baseline justify-between">
          <h2 className="text-xl font-bold">최근 소개된 게임</h2>
          <Link href="/games" className="text-sm text-muted hover:text-foreground">
            전체 보기 →
          </Link>
        </div>
        <GameGrid games={recent.content} />
      </section>
    </div>
  );
}
