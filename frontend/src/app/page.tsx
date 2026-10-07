import Link from "next/link";
import { connection } from "next/server";
import { GameGrid } from "@/components/GameGrid";
import { GameRow } from "@/components/GameRow";
import { GenreChips } from "@/components/GenreChips";
import { VideoRow } from "@/components/VideoRow";
import { getGames, getGenres } from "@/lib/api";
import { CURATOR } from "@/lib/site";
import { getLatestVideos } from "@/lib/youtube";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드·유튜브 호출은 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

const ROW = 8;
const POPULAR = 8;
const PICKS = 4;
const VIDEOS = 4;

function SectionHeader({ title, href, more = "더 보기" }: { title: string; href?: string; more?: string }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <h2 className="font-pixel text-[22px]">{title}</h2>
      {href && (
        <Link href={href} className="shrink-0 text-sm text-muted hover:text-accent">
          {more} →
        </Link>
      )}
    </div>
  );
}

export default async function HomePage() {
  await connection();
  const [genres, upcoming, recent, popular, picks, videos] = await Promise.all([
    getGenres(),
    getGames({ release: "UPCOMING", size: ROW }),
    getGames({ release: "RECENT", sort: "RELEASE", size: ROW }),
    getGames({ sort: "POPULAR", size: POPULAR }),
    getGames({ picked: true, size: PICKS }),
    getLatestVideos(VIDEOS),
  ]);

  return (
    <div className="space-y-12">
      <section className="space-y-4 pt-2">
        <h1 className="font-pixel text-[22px] leading-snug sm:text-[33px]">
          공포게임, <span className="whitespace-nowrap text-accent">한 연못에 모아 봤습니다</span>
        </h1>
        <p className="max-w-2xl text-muted">
          Steam에 새로 올라오는 공포게임을 매일 모읍니다. 출시 예정작부터 꾸준히 사랑받는 게임까지 한곳에서
          둘러보세요.
        </p>
        <GenreChips genres={genres} />
      </section>

      {upcoming.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title="출시 예정" href="/games?view=upcoming" />
          <GameRow games={upcoming.content} />
        </section>
      )}

      {recent.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title="최근 출시" href="/games?view=recent" />
          <GameRow games={recent.content} />
        </section>
      )}

      {videos.length > 0 && (
        <>
          <hr className="divider" />
          <section className="space-y-4">
            <SectionHeader title={`${CURATOR.name}의 최근 영상`} href={CURATOR.youtubeUrl} more="채널 가기" />
            <VideoRow videos={videos} />
          </section>
        </>
      )}

      <hr className="divider" />

      <section className="space-y-4">
        <SectionHeader title="인기 공포게임" href="/games?view=popular" />
        <GameGrid games={popular.content} />
      </section>

      {picks.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={`${CURATOR.name} 추천`} href="/games?view=picked" />
          <GameGrid games={picks.content} />
        </section>
      )}

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
    </div>
  );
}
