import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAdminGame } from "@/lib/admin/api";
import { getGenres } from "@/lib/api";
import { formatDate } from "@/lib/format";
import { hideAction, publishAction, unhideAction } from "../../actions";
import { ActionButton } from "../../components/ActionButton";
import { ArticleForm } from "../../components/ArticleForm";
import { CurationForm } from "../../components/CurationForm";
import { ArticleStatusBadge, GameStatusBadge } from "../../components/StatusBadge";

function parseId(id: string): number | null {
  const gameId = Number(id);
  return Number.isInteger(gameId) && gameId > 0 ? gameId : null;
}

export async function generateMetadata({ params }: PageProps<"/admin/games/[id]">): Promise<Metadata> {
  const gameId = parseId((await params).id);
  const game = gameId === null ? null : await getAdminGame(gameId);
  return { title: game ? `편집: ${game.title}` : "게임 편집" };
}

export default async function AdminGameEditPage({ params }: PageProps<"/admin/games/[id]">) {
  const { id } = await params;
  const gameId = parseId(id);
  if (gameId === null) notFound();
  const [game, genres] = await Promise.all([getAdminGame(gameId), getGenres()]);
  if (!game) notFound();

  const article = game.article;
  const canPublish = article !== null && article.highlights.length > 0;
  const publishHint = article === null
    ? "글을 먼저 저장하세요."
    : article.highlights.length === 0
      ? "장점 포인트를 1개 이상 넣고 저장하세요."
      : null;

  return (
    <div className="space-y-8">
      <Link href="/admin" className="text-sm text-muted hover:text-foreground">
        ← 목록
      </Link>

      <header className="flex flex-col gap-4 sm:flex-row">
        {game.headerImageUrl && (
          <Image
            src={game.headerImageUrl}
            alt=""
            width={460}
            height={215}
            preload
            className="w-full rounded border border-border sm:w-72"
          />
        )}
        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-xl font-bold">{game.title}</h1>
            <GameStatusBadge status={game.status} />
            <ArticleStatusBadge status={article?.status ?? null} />
          </div>
          <div className="flex flex-wrap gap-3 text-sm text-muted">
            {game.steamUrl && (
              <a href={game.steamUrl} target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Steam 스토어 ↗
              </a>
            )}
            {game.releaseDate && <span>출시 {formatDate(game.releaseDate)}</span>}
            {game.status === "PUBLISHED" && (
              <Link href={`/games/${game.slug}`} target="_blank" className="hover:text-foreground">
                사이트에서 보기 ↗
              </Link>
            )}
          </div>
          {game.shortDescription && <p className="text-sm text-muted">{game.shortDescription}</p>}
        </div>
      </header>

      <section className="space-y-3 rounded border border-border bg-surface p-4">
        <h2 className="font-semibold">공개 상태</h2>
        <div className="flex flex-wrap items-center gap-3">
          {/* 글이 생기거나 장점 포인트 수가 바뀌면 다시 마운트해 이전 실패 메시지("글이 필요합니다" 등)를 지운다.
              공개 상태는 key에 넣지 않는다(공개 성공 메시지가 바로 사라지므로). */}
          <ActionButton
            key={`${article ? "article" : "none"}-${article?.highlights.length ?? 0}`}
            action={publishAction}
            gameId={game.id}
            variant="primary"
            label={game.status === "PUBLISHED" ? "다시 공개(글 공개 상태 확인)" : "공개하기"}
            pendingLabel="공개 중…"
          />
          {game.status === "HIDDEN" ? (
            <ActionButton action={unhideAction} gameId={game.id} label="숨김 해제" />
          ) : (
            <ActionButton
              action={hideAction}
              gameId={game.id}
              label="숨기기"
              variant="danger"
              confirmMessage={game.status === "PUBLISHED" ? "사이트에서 이 게임을 내릴까요?" : undefined}
            />
          )}
        </div>
        {!canPublish && publishHint && <p className="text-sm text-amber-300">{publishHint}</p>}
        <p className="text-xs text-muted">
          공개하면 글과 게임이 함께 공개되고 사이트에 바로 반영됩니다. 공개 중인 글을 수정해도 공개 상태는 유지됩니다.
        </p>
      </section>

      <section className="space-y-3">
        <h2 className="font-semibold">주소와 장르</h2>
        <CurationForm game={game} genres={genres} />
      </section>

      <section className="space-y-3">
        <h2 className="font-semibold">큐레이션 글</h2>
        <ArticleForm gameId={game.id} article={article} />
      </section>
    </div>
  );
}
