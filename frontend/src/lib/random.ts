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
 * 앞 목록(우선)에서 먼저 무작위로 고르고, 모자라면 뒤 목록에서 채운다. key가 같은 항목은 한 번만.
 */
export function pickRandomPreferring<T>(preferred: readonly T[], rest: readonly T[], n: number, key: (item: T) => string): T[] {
  const chosen = pickRandom(preferred, n);
  const seen = new Set(chosen.map(key));
  return [...chosen, ...pickRandom(rest.filter((item) => !seen.has(key(item))), n - chosen.length)];
}
