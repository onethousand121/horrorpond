import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CollectionLinks } from "@/components/CollectionLinks";
import { GameGrid } from "@/components/GameGrid";
import { getGames, MAX_PAGE_SIZE } from "@/lib/api";
import { collectionLocales, findCollection } from "@/lib/collections";
import { alternatesFor, getDictionary, isLocale, localePath, type Locale } from "@/lib/i18n";
import { SITE_URL } from "@/lib/site";
import type { GameSummary } from "@/lib/types";

// ISR: 빌드 때는 만들지 않고(빈 배열, 빌드가 백엔드에 의존하지 않게) 첫 방문 때 정적 생성 후 1시간마다 갱신.
// 없는 모음(또는 그 언어에 없는 모음)은 notFound()로 404.
export const revalidate = 3600;

export async function generateStaticParams() {
  return [];
}

export async function generateMetadata({ params }: PageProps<"/[lang]/collections/[slug]">): Promise<Metadata> {
  const { lang, slug } = await params;
  if (!isLocale(lang)) return {};
  const collection = findCollection(slug, lang);
  if (!collection) return {};
  const t = getDictionary(lang).collections.items[collection.slug];
  return {
    title: t.title,
    description: t.description,
    alternates: alternatesFor(lang, `/collections/${collection.slug}`, collectionLocales(collection)),
    openGraph: { title: t.title, description: t.description },
  };
}

/** schema.org ItemList: 모음에 담긴 게임 순서 */
function itemListJsonLd(title: string, path: string, games: GameSummary[], lang: Locale) {
  return {
    "@context": "https://schema.org",
    "@type": "ItemList",
    name: title,
    url: `${SITE_URL}${localePath(lang, path)}`,
    inLanguage: lang,
    numberOfItems: games.length,
    itemListElement: games.map((game, index) => ({
      "@type": "ListItem",
      position: index + 1,
      url: `${SITE_URL}${localePath(lang, `/games/${game.slug}`)}`,
      name: game.title,
    })),
  };
}

export default async function CollectionPage({ params }: PageProps<"/[lang]/collections/[slug]">) {
  const { lang, slug } = await params;
  if (!isLocale(lang)) notFound();
  const collection = findCollection(slug, lang);
  if (!collection) notFound();
  const t = getDictionary(lang).collections;
  const item = t.items[collection.slug];
  const path = `/collections/${collection.slug}`;

  // 첫 페이지만 보여주고 나머지는 필터 목록(/games?...)으로 잇는다.
  // 성인 게임은 고정 노출이어도 색인용 모음에는 넣지 않는다 (상세가 noindex·사이트맵 제외인 것과 같은 기준)
  const result = await getGames({ ...collection.query, size: MAX_PAGE_SIZE, lang });
  const games = result.content.filter((game) => !game.adult);

  return (
    <div className="space-y-8">
      {games.length > 0 && (
        <script
          type="application/ld+json"
          // 검색엔진용 구조화 데이터. "<"를 이스케이프해 스크립트 태그가 닫히지 않게 한다
          dangerouslySetInnerHTML={{ __html: JSON.stringify(itemListJsonLd(item.title, path, games, lang)).replace(/</g, "\\u003c") }}
        />
      )}
      <header className="space-y-3">
        <h1 className="font-pixel text-[22px]">{item.title}</h1>
        <p className="max-w-3xl text-muted">{item.intro.join(" ")}</p>
      </header>
      <section className="space-y-4">
        <GameGrid games={games} locale={lang} emptyMessage={t.empty} />
        <div className="flex justify-end">
          <Link href={localePath(lang, collection.seeAll)} className="text-sm text-muted hover:text-accent">
            {t.seeAll} →
          </Link>
        </div>
      </section>
      <section className="space-y-3 border-t border-border pt-6">
        <h2 className="font-pixel text-[16px]">{t.others}</h2>
        <CollectionLinks locale={lang} current={collection.slug} label={t.others} />
      </section>
    </div>
  );
}
