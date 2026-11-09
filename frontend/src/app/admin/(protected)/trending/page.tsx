import type { Metadata } from "next";
import Link from "next/link";
import { getDailyPicks, getTrendingPins } from "@/lib/admin/api";
import { ImportForm } from "./ImportForm";
import { PinOrderEditor } from "./PinOrderEditor";

export const metadata: Metadata = { title: "지금 뜨는 조사 반영" };

// itch.io 신규 게임은 페이지를 바로 받아 오느라 몇 초씩 걸린다 (Vercel 함수 시간 상한)
export const maxDuration = 60;

export default async function TrendingImportPage() {
  const [pins, dailyPicks] = await Promise.all([getTrendingPins(), getDailyPicks()]);
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Link href="/admin" className="text-sm text-muted hover:text-foreground">
          ← 게임 목록
        </Link>
        <h1 className="text-xl font-bold">지금 뜨는 조사 반영</h1>
        <p className="text-sm text-muted">
          매주 Perplexity가 보낸 결과(JSON)를 그대로 붙여 넣고 반영하기를 누르세요. 게임을 찾아 &quot;지금 뜨는&quot;에 고정하고,
          사이트에 없는 게임은 추가하고, 내 영상이 없는 게임에는 참고 영상을 붙여요. 지난주에 자동으로 고정한 게임 중
          이번 결과에 없는 건 고정을 풀어요(결과가 5개 이상일 때만, 직접 고정한 게임은 그대로).
        </p>
      </div>
      <ImportForm />
      <section className="space-y-3">
        <h2 className="text-lg font-bold">고정 순서</h2>
        {/* 조사 반영 후 목록이 바뀌면 다시 그리도록 key를 고정 목록으로 */}
        <PinOrderEditor key={pins.map((pin) => pin.id).join(",")} pins={pins} />
      </section>
      <section className="space-y-3">
        <h2 className="text-lg font-bold">오늘의 게임 후보 ({dailyPicks.length})</h2>
        <p className="text-sm text-muted">
          메인 &quot;오늘의 게임&quot;에 등록 순서대로 하루씩 돌아가요. 날짜를 고정한 게임은 그날 먼저 나오고, 그날이 오기 전에는
          순서에서 빠져요. 후보 등록·수정은 각 게임 관리 화면에서 해요. 후보가 없으면 주인장 추천 게임에서 골라요.
        </p>
        {dailyPicks.length === 0 ? (
          <p className="text-sm text-muted">아직 후보가 없어요.</p>
        ) : (
          <ul className="divide-y divide-border rounded border border-border text-sm">
            {dailyPicks.map((pick) => (
              <li key={pick.gameId} className="flex flex-wrap items-center gap-x-3 gap-y-1 p-3">
                <Link href={`/admin/games/${pick.gameId}`} className="font-medium hover:underline">
                  {pick.title ?? `#${pick.gameId}`}
                </Link>
                <span className="text-muted">{pick.intro}</span>
                {pick.sponsored && <span className="rounded bg-surface-2 px-1.5 text-xs">개발사 제공</span>}
                {pick.featuredOn && <span className="rounded bg-surface-2 px-1.5 text-xs">{pick.featuredOn} 고정</span>}
                {!pick.showable && <span className="text-xs text-amber-400">사이트에 안 보임 (공개 필요)</span>}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
