import type { Metadata } from "next";
import { GameGrid } from "@/components/GameGrid";
import { getGames, MAX_PAGE_SIZE } from "@/lib/api";

export const metadata: Metadata = {
  title: "협동 공포게임",
  description: "친구와 함께 무서워할 수 있는 협동 공포게임",
};

// ISR: 정적 생성 후 1시간마다 갱신
export const revalidate = 3600;

export default async function CoopPage() {
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
