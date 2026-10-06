/**
 * 큐레이터가 꼽은 장점 포인트. 사이트의 핵심 정보라 시각적으로 강조한다.
 * - compact: 카드용 작은 목록
 * - 기본: 상세 페이지용 강조 블록
 */
export function HighlightList({ items, compact = false }: { items: string[]; compact?: boolean }) {
  if (items.length === 0) return null;

  if (compact) {
    return (
      <ul className="space-y-0.5 text-xs text-foreground/90">
        {items.map((item) => (
          <li key={item} className="flex gap-1.5">
            <span className="text-accent" aria-hidden>
              ▸
            </span>
            {item}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <section aria-label="장점 포인트" className="rounded-lg border border-accent/40 bg-accent/10 p-5">
      <h2 className="mb-3 text-sm font-bold text-accent">이 게임의 장점</h2>
      <ul className="grid gap-2 sm:grid-cols-2">
        {items.map((item) => (
          <li key={item} className="flex gap-2 text-base font-medium">
            <span className="text-accent" aria-hidden>
              ✦
            </span>
            {item}
          </li>
        ))}
      </ul>
    </section>
  );
}
