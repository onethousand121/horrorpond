/**
 * 목록에서 무작위로 n개 (요청마다 다른 조합). 페이지 컴포넌트를 순수하게 두려고 따로 둔다.
 */
export function pickRandom<T>(items: readonly T[], n: number): T[] {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy.slice(0, n);
}

/**
 * 단계별로 채운다: 앞 단계부터 무작위로 고르고, 모자라면 다음 단계에서. 단계 순서는 유지하고 key가 같은 항목은 한 번만.
 */
export function pickRandomTiered<T>(tiers: readonly (readonly T[])[], n: number, key: (item: T) => string): T[] {
  const chosen: T[] = [];
  const seen = new Set<string>();
  for (const tier of tiers) {
    if (chosen.length >= n) break;
    const picked = pickRandom(tier.filter((item) => !seen.has(key(item))), n - chosen.length);
    picked.forEach((item) => seen.add(key(item)));
    chosen.push(...picked);
  }
  return chosen;
}
