/**
 * 큐레이터가 꼽은 장점 포인트. 사이트의 핵심 정보라 시각적으로 강조한다.
 * - compact: 카드용 작은 목록
 * - 기본: 상세 페이지용 강조 블록
 */
export function HighlightList({ items, compact = false }: { items: string[]; compact?: boolean }) {
  if (items.length === 0) return null;

  if (compact) {
    return (
      <ul className="space-y-1 text-[13px] text-foreground/90">
        {items.map((item) => (
          <li key={item} className="flex gap-2">
            <span className="mt-[7px] size-1.5 shrink-0 rounded-full bg-accent" aria-hidden />
            {item}
          </li>
        ))}
      </ul>
    );
  }

  return (
    <section aria-label="장점 포인트" className="rounded-xl border border-accent/25 bg-accent/[0.06] p-5">
      <h2 className="mb-4 font-pixel text-[11px] text-accent">이 게임을 해야 하는 이유</h2>
      <ul className="space-y-2.5">
        {items.map((item, i) => (
          <li key={item} className="flex items-baseline gap-3 text-base font-medium">
            <span className="w-6 shrink-0 font-pixel text-[11px] text-accent" aria-hidden>
              {String(i + 1).padStart(2, "0")}
            </span>
            {item}
          </li>
        ))}
      </ul>
    </section>
  );
}
