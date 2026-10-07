import type { Metadata } from "next";
import Link from "next/link";
import { GameGrid } from "@/components/GameGrid";
import { Pagination } from "@/components/Pagination";
import { getGames, type GetGamesParams } from "@/lib/api";
import { CURATOR } from "@/lib/site";

export const metadata: Metadata = {
  title: "공포게임 둘러보기",
  description: "출시 예정작부터 인기작까지, Steam 공포게임 전체 목록",
};

const VIEWS = [
  { key: "popular", label: "인기", params: { sort: "POPULAR" } },
  { key: "recent", label: "최근 출시", params: { release: "RECENT", sort: "RELEASE" } },
  { key: "upcoming", label: "출시 예정", params: { release: "UPCOMING" } },
  { key: "latest", label: "새로 추가", params: { sort: "LATEST" } },
  { key: "picked", label: `${CURATOR.name} 추천`, params: { picked: true } },
] as const satisfies readonly { key: string; label: string; params: GetGamesParams }[];

type ViewKey = (typeof VIEWS)[number]["key"];

function parseView(value: string | string[] | undefined): ViewKey {
  return VIEWS.some((v) => v.key === value) ? (value as ViewKey) : "popular";
}

/** URL의 ?page는 1부터. 잘못된 값은 1페이지로 본다. */
function parsePage(value: string | string[] | undefined): number {
  const page = Number(Array.isArray(value) ? value[0] : value);
  return Number.isInteger(page) && page >= 1 ? page : 1;
}

/** 검색어: 앞뒤 공백 제거, 백엔드 제한(100자)에 맞춰 자른다 */
function parseSearch(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : (value ?? "")).trim().slice(0, 100);
}

function href(view: ViewKey, page: number, q: string): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (view !== "popular") params.set("view", view);
  if (page > 1) params.set("page", String(page));
  const query = params.toString();
  return query ? `/games?${query}` : "/games";
}

// searchParams를 읽으므로 요청마다 렌더링된다. API 응답은 데이터 캐시(1시간)를 거친다.
export default async function GamesPage({ searchParams }: PageProps<"/games">) {
  const query = await searchParams;
  const view = parseView(query.view);
  const page = parsePage(query.page);
  const q = parseSearch(query.q);
  const params = VIEWS.find((v) => v.key === view)!.params;

  const games = await getGames({ ...params, q: q || undefined, page: page - 1 });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-baseline justify-between gap-4">
        <h1 className="font-pixel text-[22px]">
          {q ? `'${q}' 검색 결과` : "공포게임"} <span className="font-sans text-base font-normal text-muted">{games.totalElements}</span>
        </h1>
        <nav aria-label="보기" className="flex flex-wrap gap-1.5 text-sm">
          {VIEWS.map((v) => (
            <Link
              key={v.key}
              href={href(v.key, 1, q)}
              aria-current={v.key === view ? "page" : undefined}
              className={`rounded-full border px-3 py-1 ${
                v.key === view
                  ? "border-accent bg-accent font-medium text-accent-ink"
                  : "border-border text-muted hover:border-accent/50 hover:text-foreground"
              }`}
            >
              {v.label}
            </Link>
          ))}
        </nav>
      </div>
      <GameGrid games={games.content} emptyMessage={q ? "검색 결과가 없습니다." : undefined} />
      <Pagination page={page - 1} totalPages={games.totalPages} hrefFor={(p) => href(view, p + 1, q)} />
    </div>
  );
}
