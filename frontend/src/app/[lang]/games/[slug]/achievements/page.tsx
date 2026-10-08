import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AchievementGuides } from "@/components/AchievementGuides";
import { getGame } from "@/lib/api";
import { alternatesFor, getDictionary, isLocale, localePath } from "@/lib/i18n";

// 게임 상세와 같은 ISR: 빌드 때는 만들지 않고 첫 방문 때 생성 후 1시간마다 갱신
export const revalidate = 3600;

export async function generateStaticParams() {
  return [];
}

/** 메타 설명에 넣을 업적 이름 (검색어 "게임 업적 이름"에 걸리도록) */
const DESCRIPTION_NAMES = 8;

export async function generateMetadata({ params }: PageProps<"/[lang]/games/[slug]/achievements">): Promise<Metadata> {
  const { lang, slug } = await params;
  if (!isLocale(lang)) return {};
  const game = await getGame(slug, lang);
  if (!game || game.adult || game.achievements.length === 0) return {};
  const t = getDictionary(lang).guide;
  const names = game.achievements.slice(0, DESCRIPTION_NAMES).map((a) => a.name).join(", ");
  const title = t.pageTitle(game.title, game.achievements.length);
  return {
    title,
    description: t.pageDescription(game.title, names),
    alternates: alternatesFor(lang, `/games/${game.slug}/achievements`),
    openGraph: { title, images: game.headerImageUrl ? [game.headerImageUrl] : [], type: "article" },
  };
}

/**
 * 업적 공략 전용 페이지. "○○ 업적", "○○ achievement" 검색이 이 페이지로 들어오도록 제목과 본문을 업적 위주로 둔다.
 * 업적 공략이 없거나 성인 게임이면 404.
 */
export default async function AchievementsPage({ params }: PageProps<"/[lang]/games/[slug]/achievements">) {
  const { lang, slug } = await params;
  if (!isLocale(lang)) notFound();
  const game = await getGame(slug, lang);
  if (!game || game.adult || game.achievements.length === 0) notFound();
  const t = getDictionary(lang).guide;

  return (
    <article className="mx-auto max-w-3xl space-y-6">
      <Link href={localePath(lang, `/games/${game.slug}`)} className="text-sm text-muted hover:text-accent">
        {t.backToGame}
      </Link>
      <header className="flex items-center gap-4">
        {game.headerImageUrl && (
          <Image
            src={game.headerImageUrl}
            alt=""
            width={184}
            height={86}
            className="hidden rounded-lg border border-border sm:block"
          />
        )}
        <h1 className="font-pixel text-[22px] leading-snug sm:text-[26px]">
          {t.pageTitle(game.title, game.achievements.length)}
        </h1>
      </header>
      <AchievementGuides achievements={game.achievements} locale={lang} />
    </article>
  );
}
