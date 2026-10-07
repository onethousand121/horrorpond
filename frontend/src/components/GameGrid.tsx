import { GameCard } from "@/components/GameCard";
import type { GameSummary } from "@/lib/types";
import { getDictionary, type Locale } from "@/lib/i18n";

/** lg 화면 기준 첫 줄 카드 수 (LCP 후보) */
const EAGER_CARDS = 4;

export function GameGrid({ games, locale, emptyMessage }: {
  games: GameSummary[];
  locale: Locale;
  emptyMessage?: string;
}) {
  if (games.length === 0) {
    return <p className="rounded-lg border border-border p-8 text-center text-muted">{emptyMessage ?? getDictionary(locale).games.empty}</p>;
  }
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {games.map((game, index) => (
        <GameCard key={game.slug} game={game} locale={locale} eager={index < EAGER_CARDS} />
      ))}
    </div>
  );
}
