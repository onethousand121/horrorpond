import { GameCard } from "@/components/GameCard";
import type { GameSummary } from "@/lib/types";

/** lg 화면 기준 첫 줄 카드 수 (LCP 후보) */
const EAGER_CARDS = 3;

export function GameGrid({ games, emptyMessage = "아직 소개된 게임이 없습니다." }: {
  games: GameSummary[];
  emptyMessage?: string;
}) {
  if (games.length === 0) {
    return <p className="rounded-lg border border-border p-8 text-center text-muted">{emptyMessage}</p>;
  }
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {games.map((game, index) => (
        <GameCard key={game.slug} game={game} eager={index < EAGER_CARDS} />
      ))}
    </div>
  );
}
