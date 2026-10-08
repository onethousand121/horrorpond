import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getAdminGame } from "@/lib/admin/api";
import { getGenres } from "@/lib/api";
import { formatCount, formatDate } from "@/lib/format";
import { hideAction, publishAction, unhideAction } from "../../actions";
import { ActionButton } from "../../components/ActionButton";
import { ArticleForm } from "../../components/ArticleForm";
import { CurationForm } from "../../components/CurationForm";
import { AchievementForm, PlayVideoForm } from "../../components/GuideForms";
import { AdultBadge, ArticleStatusBadge, GameStatusBadge, VisibilityBadge } from "../../components/StatusBadge";

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
  /** 자동 노출이 안 되는 이유 (공개 사이트 규칙: 백엔드 ExposurePolicy) */
  const hiddenReason =
    game.status === "HIDDEN"
      ? "숨김 상태입니다."
      : game.adult
        ? "성인 콘텐츠라 자동 노출되지 않습니다. 보여주려면 고정 노출하세요."
        : "출시 10일이 지났고 Steam 리뷰가 자동 노출 기준(10개)에 못 미칩니다. 보여주려면 고정 노출하세요.";

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
            <VisibilityBadge visible={game.publiclyVisible} />
            <GameStatusBadge status={game.status} />
            {game.adult && <AdultBadge />}
            <ArticleStatusBadge status={article?.status ?? null} />
          </div>
          <div className="flex flex-wrap gap-3 text-sm text-muted">
            {game.steamUrl && (
              <a href={game.steamUrl} target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                Steam 스토어 ↗
              </a>
            )}
            {game.comingSoon ? <span>출시 예정</span> : game.releaseDate && <span>출시 {formatDate(game.releaseDate)}</span>}
            <span>리뷰 {game.reviewCount == null ? "-" : formatCount(game.reviewCount)}</span>
            {game.publiclyVisible && (
              <Link href={`/games/${game.slug}`} target="_blank" className="hover:text-foreground">
                사이트에서 보기 ↗
              </Link>
            )}
          </div>
          {game.shortDescription && <p className="text-sm text-muted">{game.shortDescription}</p>}
          {game.tags.length > 0 && <p className="text-xs text-muted">태그: {game.tags.slice(0, 10).join(", ")}</p>}
        </div>
      </header>

      <section className="space-y-3 rounded border border-border bg-surface p-4">
        <h2 className="font-semibold">노출</h2>
        {!game.publiclyVisible && <p className="text-sm text-amber-300">{hiddenReason}</p>}
        <div className="flex flex-wrap items-center gap-3">
          <ActionButton
            action={publishAction}
            gameId={game.id}
            variant="primary"
            label={game.status === "PUBLISHED" ? "글 공개 반영" : "고정 노출"}
            pendingLabel="처리 중…"
          />
          {game.status === "HIDDEN" ? (
            <ActionButton action={unhideAction} gameId={game.id} label="숨김 해제" />
          ) : (
            <ActionButton
              action={hideAction}
              gameId={game.id}
              label="숨기기"
              variant="danger"
              confirmMessage={game.publiclyVisible ? "사이트에서 이 게임을 내릴까요?" : undefined}
            />
          )}
        </div>
        <p className="text-xs text-muted">
          수집된 공포게임은 성인 콘텐츠가 아니면 출시 예정, 출시 후 10일 이내, 또는 리뷰 10개 이상일 때 자동으로
          노출됩니다.
          고정 노출은 이 기준과 상관없이 항상 보여줍니다. 장점 포인트가 있는 글이 있으면 &quot;{"재일 추천"}&quot;으로
          함께 공개됩니다. 모든 변경은 사이트에 바로 반영됩니다.
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

      <section className="space-y-3">
        <h2 className="font-semibold">플레이 영상</h2>
        <p className="text-sm text-muted">없으면 사이트에 이 영역이 보이지 않습니다. 있으면 게임 카드에 &quot;플레이 영상&quot; 표시가 붙습니다.</p>
        {/* 저장 후 서버 값(가져온 유튜브 제목 등)으로 다시 그리도록 내용이 바뀌면 새로 마운트한다 */}
        <PlayVideoForm key={JSON.stringify(game.playVideos)} gameId={game.id} videos={game.playVideos} />
      </section>

      <section className="space-y-3">
        <h2 className="font-semibold">업적 공략</h2>
        <p className="text-sm text-muted">없으면 사이트에 이 영역이 보이지 않습니다.</p>
        <AchievementForm key={JSON.stringify(game.achievements)} gameId={game.id} achievements={game.achievements} />
      </section>
    </div>
  );
}
