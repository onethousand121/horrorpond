import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { GameGrid } from "@/components/GameGrid";
import { Pagination } from "@/components/Pagination";
import { getGames, type GetGamesParams } from "@/lib/api";
import { alternatesFor, getDictionary, isLocale, localePath, type Locale } from "@/lib/i18n";

export async function generateMetadata({ params }: PageProps<"/[lang]/games">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) return {};
  const t = getDictionary(lang).games;
  return { title: t.metaTitle, description: t.metaDescription, alternates: alternatesFor(lang, "/games") };
}

const VIEWS = [
  { key: "trending", params: { sort: "TRENDING" } },
  { key: "popular", params: { sort: "POPULAR" } },
  { key: "recent", params: { release: "RECENT", sort: "RELEASE" } },
  { key: "upcoming", params: { release: "UPCOMING" } },
  { key: "latest", params: { sort: "LATEST" } },
  { key: "picked", params: { keeper: true, sort: "POPULAR" } },
] as const satisfies readonly { key: string; params: GetGamesParams }[];

type ViewKey = (typeof VIEWS)[number]["key"];

function parseView(value: string | string[] | undefined): ViewKey {
  return VIEWS.some((v) => v.key === value) ? (value as ViewKey) : "trending";
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

function href(locale: Locale, view: ViewKey, page: number, q: string, korean: boolean): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (view !== "trending") params.set("view", view);
  if (korean) params.set("korean", "1");
  if (page > 1) params.set("page", String(page));
  const query = params.toString();
  return localePath(locale, query ? `/games?${query}` : "/games");
}

// searchParams를 읽으므로 요청마다 렌더링된다. API 응답은 데이터 캐시(1시간)를 거친다.
export default async function GamesPage({ params, searchParams }: PageProps<"/[lang]/games">) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const t = getDictionary(lang);
  const query = await searchParams;
  const view = parseView(query.view);
  const page = parsePage(query.page);
  const q = parseSearch(query.q);
  // 한국어 지원 필터는 탭·검색과 함께 쓴다 (한국어 화면에서만 보여준다)
  const korean = lang === "ko" && query.korean === "1";
  // 검색은 탭(지금 뜨는 = 최근 90일 등) 조건 없이 사이트 전체에서 찾고, 많이 알려진 게임부터 보여준다
  const viewParams: GetGamesParams = q ? { sort: "POPULAR" } : VIEWS.find((v) => v.key === view)!.params;

  const games = await getGames({ ...viewParams, q: q || undefined, korean, page: page - 1, lang });
  const currentView: ViewKey = q ? "trending" : view;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-baseline justify-between gap-4">
        <h1 className="font-pixel text-[22px]">
          {q ? t.search.resultsFor(q) : t.games.heading} <span className="font-sans text-base font-normal text-muted">{games.totalElements}</span>
        </h1>
        {!q && (
          <nav aria-label={t.games.viewNav} className="flex flex-wrap gap-1.5 text-sm">
            {VIEWS.map((v) => (
              <Link
                key={v.key}
                href={href(lang, v.key, 1, q, korean)}
                aria-current={v.key === view ? "page" : undefined}
                className={`rounded-full border px-3 py-1 ${
                  v.key === view
                    ? "border-accent bg-accent font-medium text-accent-ink"
                    : "border-border text-muted hover:border-accent/50 hover:text-foreground"
                }`}
              >
                {t.games.views[v.key]}
              </Link>
            ))}
          </nav>
        )}
      </div>
      {lang === "ko" && (
        <Link
          href={href(lang, currentView, 1, q, !korean)}
          aria-current={korean ? "true" : undefined}
          className={`inline-flex w-fit items-center gap-1.5 rounded-full border px-3 py-1 text-sm ${
            korean
              ? "border-accent bg-accent/15 font-medium text-accent"
              : "border-border text-muted hover:border-accent/50 hover:text-foreground"
          }`}
        >
          <span aria-hidden>{korean ? "✓" : "+"}</span>
          {t.home.koreanOnly}
        </Link>
      )}
      <GameGrid games={games.content} locale={lang} emptyMessage={q ? t.search.noResults : undefined} />
      <Pagination
        page={page - 1}
        totalPages={games.totalPages}
        hrefFor={(p) => href(lang, currentView, p + 1, q, korean)}
        locale={lang}
      />
    </div>
  );
}
