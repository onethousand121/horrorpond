import type { Metadata } from "next";
import Link from "next/link";
import { GameGrid } from "@/components/GameGrid";
import { Pagination } from "@/components/Pagination";
import { getGames } from "@/lib/api";
import type { GameSort } from "@/lib/types";

export const metadata: Metadata = {
  title: "전체 게임",
  description: "큐레이터가 소개한 공포게임 전체 목록",
};

const SORTS = [
  { key: "latest", label: "최근 소개순", api: "LATEST" },
  { key: "release", label: "최근 출시순", api: "RELEASE" },
] as const satisfies readonly { key: string; label: string; api: GameSort }[];

type SortKey = (typeof SORTS)[number]["key"];

function parseSort(value: string | string[] | undefined): SortKey {
  return value === "release" ? "release" : "latest";
}

/** URL의 ?page는 1부터. 잘못된 값은 1페이지로 본다. */
function parsePage(value: string | string[] | undefined): number {
  const page = Number(Array.isArray(value) ? value[0] : value);
  return Number.isInteger(page) && page >= 1 ? page : 1;
}

function href(sort: SortKey, page: number): string {
  const params = new URLSearchParams();
  if (sort !== "latest") params.set("sort", sort);
  if (page > 1) params.set("page", String(page));
  const query = params.toString();
  return query ? `/games?${query}` : "/games";
}

// searchParams를 읽으므로 요청마다 렌더링된다. API 응답은 데이터 캐시(1시간)를 거친다.
export default async function GamesPage({ searchParams }: PageProps<"/games">) {
  const query = await searchParams;
  const sort = parseSort(query.sort);
  const page = parsePage(query.page);
  const sortApi = SORTS.find((s) => s.key === sort)!.api;

  const games = await getGames({ sort: sortApi, page: page - 1 });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-baseline justify-between gap-4">
        <h1 className="font-pixel text-[22px]">
          전체 게임 <span className="text-base font-normal text-muted">{games.totalElements}</span>
        </h1>
        <nav aria-label="정렬" className="flex gap-3 text-sm">
          {SORTS.map((s) => (
            <Link
              key={s.key}
              href={href(s.key, 1)}
              aria-current={s.key === sort ? "true" : undefined}
              className={s.key === sort ? "font-bold text-foreground" : "text-muted hover:text-foreground"}
            >
              {s.label}
            </Link>
          ))}
        </nav>
      </div>
      <GameGrid games={games.content} />
      <Pagination page={page - 1} totalPages={games.totalPages} hrefFor={(p) => href(sort, p + 1)} />
    </div>
  );
}
