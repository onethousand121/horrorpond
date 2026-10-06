import type { Metadata } from "next";
import { connection } from "next/server";
import { GameGrid } from "@/components/GameGrid";
import { getGames, MAX_PAGE_SIZE } from "@/lib/api";

export const metadata: Metadata = {
  title: "협동 공포게임",
  description: "친구와 함께 무서워할 수 있는 협동 공포게임",
};

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

export default async function CoopPage() {
  await connection();
  // MVP: 첫 48개만 보여준다 (페이지네이션은 공개 게임이 늘면 추가)
  const games = await getGames({ coop: true, size: MAX_PAGE_SIZE });

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="text-2xl font-bold">협동 공포게임</h1>
        <p className="text-muted">혼자는 무섭다면, 친구와 함께. 온라인·로컬 협동을 지원하는 게임만 모았습니다.</p>
      </header>
      <GameGrid games={games.content} emptyMessage="협동 게임이 아직 없습니다." />
    </div>
  );
}
