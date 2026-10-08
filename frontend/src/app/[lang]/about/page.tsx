import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { alternatesFor, getDictionary, isLocale, type Locale } from "@/lib/i18n";
import { CONTACT_EMAIL, SITE_NAME } from "@/lib/site";

const DESCRIPTION: Record<Locale, string> = {
  ko: "lurkpond는 공포게임을 한곳에서 모아 보는 사이트입니다.",
  en: "lurkpond collects every horror game on Steam in one place.",
};

export async function generateMetadata({ params }: PageProps<"/[lang]/about">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) return {};
  return {
    title: getDictionary(lang).nav.about,
    description: DESCRIPTION[lang],
    alternates: alternatesFor(lang, "/about"),
  };
}

/** 소개 문구는 문단 단위라 사전 대신 언어별로 쓴다 */
const CONTENT: Record<Locale, { title: string; intro: string; points: string[]; contactTitle: string; contact: string; contactLabel: string }> = {
  ko: {
    title: `${SITE_NAME} 소개`,
    intro:
      "lurkpond는 Steam의 공포게임을 한곳에서 모아 보는 사이트입니다. 새로 올라온 공포게임과 출시 예정작을 매일 자동으로 수집해 보여주고, 운영자 류재일이 직접 플레이한 게임에는 영상과 추천 이유를 덧붙입니다.",
    points: [
      "출시 예정작과 출시 후 10일 이내 신작은 바로, 그 뒤로는 Steam 리뷰가 어느 정도 쌓인 게임이 목록에 남습니다.",
      "심리 공포, 아날로그 호러 같은 서브장르는 Steam 태그를 바탕으로 자동 분류합니다.",
      "\"재일 추천\" 표시는 직접 플레이하고 추천하는 게임입니다.",
      "협찬을 받은 글은 항상 협찬 표시와 고지 문구를 함께 보여줍니다.",
    ],
    contactTitle: "개발사 프리뷰 / 노출 문의",
    contact:
      "출시 예정이거나 출시된 공포게임의 프리뷰 키 제공, 소개 요청은 아래로 연락해 주세요. 추천 여부와 내용은 직접 플레이한 뒤 독립적으로 결정하며, 협찬인 경우 글에 명시합니다.",
    contactLabel: "연락처",
  },
  en: {
    title: `About ${SITE_NAME}`,
    intro:
      "lurkpond gathers horror games on Steam in one place. New releases and upcoming games are collected automatically every day, and games played by Jaeil, the Korean horror game YouTuber behind the site, come with videos and recommendations.",
    points: [
      "Upcoming games and releases from the last 10 days appear right away; after that, games stay listed once they have a few Steam reviews.",
      "Subgenres such as psychological or analog horror are sorted automatically from Steam tags.",
      "\"Jaeil's Pick\" marks games he has played and recommends.",
      "Sponsored write-ups always carry a sponsorship badge and disclosure.",
    ],
    contactTitle: "For developers: previews and features",
    contact:
      "Making a horror game? Send preview keys or feature requests to the address below. Whether and how a game is recommended is decided independently after playing it, and sponsorships are always disclosed.",
    contactLabel: "Contact",
  },
};

export default async function AboutPage({ params }: PageProps<"/[lang]/about">) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const c = CONTENT[lang];

  return (
    <div className="mx-auto max-w-2xl space-y-10">
      <section className="space-y-4">
        <h1 className="font-pixel text-[22px]">{c.title}</h1>
        <p className="leading-relaxed text-foreground/90">{c.intro}</p>
        <ul className="list-disc space-y-1 pl-5 text-foreground/90">
          {c.points.map((point) => (
            <li key={point}>{point}</li>
          ))}
        </ul>
      </section>

      <section className="space-y-3 rounded-lg border border-border bg-surface/60 p-6">
        <h2 className="text-lg font-bold">{c.contactTitle}</h2>
        <p className="text-sm leading-relaxed text-foreground/90">{c.contact}</p>
        <p className="text-sm">
          <span className="text-muted">{c.contactLabel}</span>{" "}
          <a href={`mailto:${CONTACT_EMAIL}`} className="text-accent hover:underline">
            {CONTACT_EMAIL}
          </a>
        </p>
      </section>
    </div>
  );
}
