import { GameCard } from "@/components/GameCard";
import type { GameSummary } from "@/lib/types";
import type { Locale } from "@/lib/i18n";

/** 가로 스크롤 한 줄 (모바일에서는 옆으로 넘겨 본다) */
export function GameRow({ games, locale }: { games: GameSummary[]; locale: Locale }) {
  return (
    <div className="-mx-4 flex snap-x scroll-px-4 gap-4 overflow-x-auto px-4 pb-2 sm:mx-0 sm:scroll-px-0 sm:px-0">
      {games.map((game) => (
        <div key={game.slug} className="w-[72%] shrink-0 snap-start sm:w-[calc((100%-3rem)/4)]">
          <GameCard game={game} locale={locale} />
        </div>
      ))}
    </div>
  );
}
