import Image from "next/image";
import Link from "next/link";
import { connection } from "next/server";
import { CuratorNote } from "@/components/CuratorNote";
import { FeaturedGame } from "@/components/FeaturedGame";
import { GameGrid } from "@/components/GameGrid";
import { GenreChips } from "@/components/GenreChips";
import { getGames, getGenres } from "@/lib/api";
import { CURATOR, SITE_TAGLINE } from "@/lib/site";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

/** 대표 1개 + 그리드 9개 */
const RECENT_COUNT = 10;

export default async function HomePage() {
  await connection();
  const [recent, genres] = await Promise.all([getGames({ size: RECENT_COUNT }), getGenres()]);
  const [featured, ...rest] = recent.content;

  return (
    <div className="space-y-14">
      <section className="space-y-6">
        {/* 유튜브 배너와 같은 8bit 연못. 간판과 개구리가 있는 가운데 띠가 보이게 자른다 */}
        <div className="relative -mx-4 overflow-hidden border-y border-border sm:mx-0 sm:rounded-2xl sm:border">
          <div className="relative aspect-[16/9] sm:aspect-[21/8]">
            <Image
              src="/brand/pond-banner.webp"
              alt={`${CURATOR.name} HORROR GAME, lurkpond: 밤의 연못과 컵 속 개구리`}
              fill
              priority
              sizes="(min-width: 1152px) 1152px, 100vw"
              className="object-cover object-[50%_55%]"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent" />
          </div>
        </div>
        <div className="space-y-4">
          <h1 className="font-pixel text-[22px] leading-snug sm:text-[33px]">
            {SITE_TAGLINE}을 <span className="whitespace-nowrap text-accent">건져 올립니다</span>
          </h1>
          <p className="max-w-2xl text-muted">
            큐레이터 {CURATOR.name}이 직접 플레이하고 고른 게임만 소개합니다. 점수 대신, 이 게임을 해야 하는 이유를
            적었습니다.
          </p>
          <GenreChips genres={genres} />
        </div>
      </section>

      <hr className="divider" />

      {featured ? (
        <FeaturedGame game={featured} label="새로 건져 올린 게임" />
      ) : (
        <p className="rounded-xl border border-border p-8 text-center text-muted">아직 소개된 게임이 없습니다.</p>
      )}

      {rest.length > 0 && (
        <section className="space-y-4">
          <div className="flex items-baseline justify-between">
            <h2 className="font-pixel text-[22px]">최근 소개한 게임</h2>
            <Link href="/games" className="text-sm text-muted hover:text-accent">
              전체 보기 →
            </Link>
          </div>
          <GameGrid games={rest} />
        </section>
      )}

      <hr className="divider" />

      <Link
        href="/coop"
        className="glow-hover flex items-center justify-between gap-4 rounded-2xl border border-accent-2/30 bg-gradient-to-r from-accent-2/10 to-transparent p-6"
      >
        <span>
          <span className="block font-pixel text-[22px] text-accent-2">혼자는 무섭다면</span>
          <span className="text-sm text-muted">친구와 함께 비명 지르기 좋은 협동 공포게임</span>
        </span>
        <span className="text-accent-2">→</span>
      </Link>

      <CuratorNote />
    </div>
  );
}
