import Link from "next/link";
import { Pagination } from "@/components/Pagination";
import { searchAdminGames } from "@/lib/admin/api";
import type { GameStatus } from "@/lib/admin/types";
import { formatDate } from "@/lib/format";
import { hideAction, unhideAction } from "./actions";
import { ActionButton } from "./components/ActionButton";
import { ArticleStatusBadge, GameStatusBadge } from "./components/StatusBadge";

const TABS = [
  { key: "candidate", label: "후보", status: "CANDIDATE" },
  { key: "published", label: "공개", status: "PUBLISHED" },
  { key: "hidden", label: "숨김", status: "HIDDEN" },
  { key: "all", label: "전체", status: undefined },
] as const satisfies readonly { key: string; label: string; status: GameStatus | undefined }[];

type TabKey = (typeof TABS)[number]["key"];

const PAGE_SIZE = 30;

function single(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function parseTab(value: string | undefined): TabKey {
  return TABS.some((tab) => tab.key === value) ? (value as TabKey) : "candidate";
}

/** URL의 ?page는 1부터 */
function parsePage(value: string | undefined): number {
  const page = Number(value);
  return Number.isInteger(page) && page >= 1 ? page - 1 : 0;
}

function href(tab: TabKey, q: string, page = 0): string {
  const params = new URLSearchParams();
  if (tab !== "candidate") params.set("tab", tab);
  if (q) params.set("q", q);
  if (page > 0) params.set("page", String(page + 1));
  const query = params.toString();
  return query ? `/admin?${query}` : "/admin";
}

function steamUrl(externalId: string): string {
  return `https://store.steampowered.com/app/${externalId}`;
}

export default async function AdminGamesPage({ searchParams }: PageProps<"/admin">) {
  const params = await searchParams;
  const tab = parseTab(single(params.tab));
  const q = (single(params.q) ?? "").trim();
  const page = parsePage(single(params.page));
  const status = TABS.find((t) => t.key === tab)?.status;
  const result = await searchAdminGames({ status, q, page, size: PAGE_SIZE });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <nav className="flex gap-1" aria-label="상태">
          {TABS.map((t) => (
            <Link
              key={t.key}
              href={href(t.key, q)}
              aria-current={t.key === tab ? "page" : undefined}
              className={`rounded px-3 py-1.5 text-sm ${t.key === tab ? "bg-surface text-foreground" : "text-muted hover:text-foreground"}`}
            >
              {t.label}
            </Link>
          ))}
        </nav>
        <form action="/admin" className="flex gap-2">
          {tab !== "candidate" && <input type="hidden" name="tab" value={tab} />}
          <input
            type="search"
            name="q"
            defaultValue={q}
            placeholder="제목 검색"
            className="w-56 rounded border border-border bg-surface px-3 py-1.5 text-sm"
          />
          <button type="submit" className="rounded border border-border px-3 py-1.5 text-sm hover:border-foreground/40">
            검색
          </button>
        </form>
      </div>

      <p className="text-sm text-muted">{result.totalElements.toLocaleString()}개</p>

      {result.content.length === 0 ? (
        <p className="py-12 text-center text-muted">해당하는 게임이 없습니다.</p>
      ) : (
        <ul className="divide-y divide-border rounded border border-border">
          {result.content.map((game) => (
            <li key={game.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
              <div className="min-w-0 flex-1 space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <Link href={`/admin/games/${game.id}`} className="font-medium hover:text-accent">
                    {game.title}
                  </Link>
                  <GameStatusBadge status={game.status} />
                  <ArticleStatusBadge status={game.articleStatus} />
                  {game.sameTitleCount > 0 && (
                    <Link
                      href={href("all", game.title)}
                      className="rounded border border-amber-500/40 px-2 py-0.5 text-xs text-amber-300 hover:bg-amber-500/10"
                      title="Steam에 같은 제목으로 따로 등록된 게임이 있습니다 (원작/리마스터, 본편/체험판 등)"
                    >
                      같은 제목 {game.sameTitleCount}개 더
                    </Link>
                  )}
                </div>
                <div className="flex flex-wrap gap-3 text-xs text-muted">
                  {game.source === "STEAM" && game.externalId ? (
                    <a href={steamUrl(game.externalId)} target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                      Steam {game.externalId} ↗
                    </a>
                  ) : (
                    <span>{game.source}</span>
                  )}
                  {game.releaseDate && <span>출시 {formatDate(game.releaseDate)}</span>}
                  <span>/{game.slug}</span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                {game.status === "HIDDEN" ? (
                  <ActionButton action={unhideAction} gameId={game.id} label="숨김 해제" />
                ) : (
                  <ActionButton
                    action={hideAction}
                    gameId={game.id}
                    label="숨기기"
                    variant="danger"
                    confirmMessage={game.status === "PUBLISHED" ? `"${game.title}"을(를) 사이트에서 내릴까요?` : undefined}
                  />
                )}
                <Link
                  href={`/admin/games/${game.id}`}
                  className="rounded border border-border px-3 py-1.5 text-sm hover:border-foreground/40"
                >
                  편집
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}

      <Pagination page={result.page} totalPages={result.totalPages} hrefFor={(p) => href(tab, q, p)} />
    </div>
  );
}
