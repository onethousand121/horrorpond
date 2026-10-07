import Link from "next/link";
import { connection } from "next/server";
import { GameRow } from "@/components/GameRow";
import { PopularShowcase } from "@/components/PopularShowcase";
import { VideoRow } from "@/components/VideoRow";
import { getGames, getGenres } from "@/lib/api";
import { CURATOR } from "@/lib/site";
import { getFreshVideos } from "@/lib/youtube";

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드·유튜브 호출은 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

const SHOWCASE = 5;
const ROW = 8;
const VIDEOS = 4;
/** 마지막 영상이 이보다 오래되면 영상 줄을 숨긴다 */
const VIDEO_FRESH_DAYS = 60;

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

const CHIP = "rounded-full border px-3 py-1 text-sm";

export default async function HomePage() {
  await connection();
  const [genres, popular, recent, upcoming, videos] = await Promise.all([
    getGenres(),
    getGames({ sort: "POPULAR", size: SHOWCASE }),
    getGames({ release: "RECENT", sort: "RELEASE", size: ROW }),
    getGames({ release: "UPCOMING", size: ROW }),
    getFreshVideos(VIDEOS, VIDEO_FRESH_DAYS),
  ]);

  return (
    <div className="space-y-12">
      <section className="space-y-4 pt-2">
        <h1 className="font-pixel text-[22px] leading-snug sm:text-[30px]">
          공포게임을 <span className="whitespace-nowrap text-accent">한 곳에 모으다</span>
        </h1>
        <p className="max-w-2xl text-muted">
          Steam에 새로 올라오는 공포게임을 매일 모읍니다.
          <br className="hidden sm:block" /> 출시 예정작부터 꾸준히 사랑받는 게임까지 한곳에서 둘러보세요.
        </p>
        <nav aria-label="빠른 필터" className="flex flex-wrap gap-2">
          <Link
            href="/coop"
            className={`${CHIP} border-accent-2/50 bg-accent-2/15 font-medium text-accent-2 hover:bg-accent-2/25`}
          >
            협동만 보기
          </Link>
          {genres.map((genre) => (
            <Link
              key={genre.slug}
              href={`/genres/${genre.slug}`}
              className={`${CHIP} border-border bg-surface/60 text-muted hover:border-accent/50 hover:text-foreground`}
            >
              {genre.name}
            </Link>
          ))}
        </nav>
      </section>

      {popular.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title="현재 인기 있는 공포게임" href="/games?view=popular" />
          <PopularShowcase games={popular.content} />
        </section>
      )}

      {recent.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title="최근 출시" href="/games?view=recent" />
          <GameRow games={recent.content} />
        </section>
      )}

      {upcoming.content.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title="출시 예정" href="/games?view=upcoming" />
          <GameRow games={upcoming.content} />
        </section>
      )}

      {videos.length > 0 && (
        <section className="space-y-4">
          <SectionHeader title={`${CURATOR.name}의 최근 영상`} href={CURATOR.youtubeUrl} more="채널 가기" />
          <VideoRow videos={videos} />
        </section>
      )}
    </div>
  );
}
