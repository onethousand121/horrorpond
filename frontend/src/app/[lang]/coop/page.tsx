import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { GameGrid } from "@/components/GameGrid";
import { getGames, MAX_PAGE_SIZE } from "@/lib/api";
import { alternatesFor, getDictionary, isLocale } from "@/lib/i18n";

export async function generateMetadata({ params }: PageProps<"/[lang]/coop">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) return {};
  const t = getDictionary(lang).coop;
  return { title: t.title, description: t.description, alternates: alternatesFor(lang, "/coop") };
}

// 요청 시 렌더링: 고정 경로는 빌드 때 사전 렌더링되므로, connection()으로 빌드가 백엔드에 의존하지 않게 한다.
// 백엔드 호출은 lib/api의 fetch 데이터 캐시(1시간, 태그 기반 갱신)가 막아 준다.

export default async function CoopPage({ params }: PageProps<"/[lang]/coop">) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  await connection();
  const t = getDictionary(lang).coop;
  // MVP: 첫 48개만 보여준다 (페이지네이션은 공개 게임이 늘면 추가)
  const games = await getGames({ coop: true, sort: "POPULAR", size: MAX_PAGE_SIZE, lang });

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="font-pixel text-[22px]">{t.title}</h1>
        <p className="text-muted">{t.intro}</p>
      </header>
      <GameGrid games={games.content} locale={lang} emptyMessage={t.empty} />
    </div>
  );
}
