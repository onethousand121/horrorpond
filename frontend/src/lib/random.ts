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
