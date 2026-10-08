import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { alternatesFor, getDictionary, isLocale, type Locale } from "@/lib/i18n";
import { CONTACT_EMAIL, CURATOR, PRIVACY_EFFECTIVE_DATE, SITE_NAME } from "@/lib/site";

export async function generateMetadata({ params }: PageProps<"/[lang]/privacy">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) return {};
  return {
    title: getDictionary(lang).nav.privacy,
    alternates: alternatesFor(lang, "/privacy"),
  };
}

interface Section {
  title: string;
  body: string[];
}

/**
 * 지금 사이트가 실제로 하는 일만 적는다: 회원가입·입력 폼이 없고, 언어 선택 쿠키 하나와 호스팅 접속 기록만 남는다.
 * 방문 통계나 광고(쿠키)를 붙이면 그 전에 이 내용과 시행일(PRIVACY_EFFECTIVE_DATE)을 고친다.
 */
const CONTENT: Record<Locale, { title: string; intro: string; sections: Section[]; effective: string }> = {
  ko: {
    title: "개인정보처리방침",
    intro: `${SITE_NAME}(이하 "사이트")는 회원가입이나 입력 양식 없이 누구나 볼 수 있는 공포게임 정보 사이트입니다. 사이트가 처리하는 정보는 아래가 전부입니다.`,
    sections: [
      {
        title: "1. 처리하는 정보와 목적",
        body: [
          "접속 기록: 사이트에 접속하면 호스팅 서버에 IP 주소, 접속 시각, 브라우저 정보, 요청한 주소가 자동으로 기록됩니다. 서비스 운영, 장애 대응, 보안(비정상 접근 차단)에만 씁니다.",
          "쿠키: 언어 선택(한국어/영어)을 기억하는 쿠키(NEXT_LOCALE) 하나를 씁니다. 이 쿠키에는 고른 언어만 저장됩니다.",
          "문의 메일: 문의 메일을 보내면 보낸 사람의 이메일 주소와 내용이 답변을 위해 메일함에 보관됩니다.",
          "사이트는 이름, 연락처 등 개인정보를 입력받지 않고, 방문 통계나 광고용 쿠키를 쓰지 않습니다.",
        ],
      },
      {
        title: "2. 보유 기간과 파기",
        body: [
          "접속 기록은 호스팅 서비스의 로그 보관 기간(통상 30일 이내)이 지나면 자동으로 삭제됩니다.",
          "언어 선택 쿠키는 1년 뒤 만료되며, 브라우저 설정에서 언제든 지울 수 있습니다.",
          "문의 메일은 답변 후 1년간 보관한 뒤 삭제합니다. 삭제를 요청하면 바로 지웁니다.",
        ],
      },
      {
        title: "3. 제3자 제공과 처리 위탁",
        body: [
          "개인정보를 제3자에게 판매하거나 제공하지 않습니다. 법령에 따라 요청받은 경우는 예외입니다.",
          "사이트 운영을 위해 다음 서비스를 이용하며, 접속 기록이 이 서비스들의 서버(국외 포함)에 남을 수 있습니다: Vercel Inc.(웹사이트 호스팅, 미국/일본), Amazon Web Services(서버 호스팅, 서울 리전), Cloudflare Inc.(도메인·메일 전달).",
        ],
      },
      {
        title: "4. 외부 콘텐츠",
        body: [
          "게임 이미지는 Steam(Valve) 서버에서 불러오며, 이때 접속 정보가 Steam에 전달됩니다.",
          "유튜브 영상은 재생 버튼을 누를 때 유튜브의 개인정보 보호 강화 모드(youtube-nocookie.com)로 불러옵니다. 재생 후에는 Google의 개인정보처리방침이 적용됩니다.",
        ],
      },
      {
        title: "5. 이용자의 권리",
        body: [
          "자신의 정보(문의 메일 등)를 열람, 정정, 삭제, 처리 정지하도록 요청할 수 있습니다. 아래 메일로 요청하면 지체 없이 처리합니다.",
        ],
      },
      {
        title: "6. 안전성 확보 조치",
        body: [
          "모든 접속은 HTTPS로 암호화되며, 관리 기능은 별도 인증 키가 있어야만 쓸 수 있습니다. 서버와 데이터베이스는 외부에서 직접 접근할 수 없게 막아 둡니다.",
        ],
      },
      {
        title: "7. 개인정보 보호책임자",
        body: [`운영자 ${CURATOR.name} · ${CONTACT_EMAIL}`],
      },
      {
        title: "8. 변경",
        body: [
          "방문 통계나 광고 등 쿠키를 쓰는 기능을 추가하면, 적용하기 전에 이 방침을 고치고 사이트에 알립니다.",
        ],
      },
    ],
    effective: `시행일: ${PRIVACY_EFFECTIVE_DATE}`,
  },
  en: {
    title: "Privacy Policy",
    intro: `${SITE_NAME} ("the site") is a horror game information site that anyone can browse without an account or forms. This is everything the site processes.`,
    sections: [
      {
        title: "1. What we process and why",
        body: [
          "Access logs: when you visit, the hosting servers automatically record your IP address, time of access, browser information and the requested URL. These are used only to run the service, fix problems and block abuse.",
          "Cookies: one cookie (NEXT_LOCALE) remembers your language choice (Korean/English). It stores only that choice.",
          "Contact email: if you email us, your address and message are kept in our mailbox to reply.",
          "The site does not ask for names or contact details and does not use analytics or advertising cookies.",
        ],
      },
      {
        title: "2. Retention and deletion",
        body: [
          "Access logs are deleted automatically after the hosting providers' log retention period (usually within 30 days).",
          "The language cookie expires after one year, and you can delete it in your browser at any time.",
          "Contact emails are kept for one year after we reply, then deleted. We delete them right away on request.",
        ],
      },
      {
        title: "3. Sharing and processors",
        body: [
          "We do not sell or share personal information with third parties, except when required by law.",
          "We use these services to run the site, so access logs may be stored on their servers, including outside Korea: Vercel Inc. (website hosting, US/Japan), Amazon Web Services (server hosting, Seoul region), Cloudflare Inc. (domain and email forwarding).",
        ],
      },
      {
        title: "4. External content",
        body: [
          "Game images are loaded from Steam (Valve) servers, which receive your connection information.",
          "YouTube videos load in YouTube's privacy-enhanced mode (youtube-nocookie.com) only when you press play. Once playing, Google's privacy policy applies.",
        ],
      },
      {
        title: "5. Your rights",
        body: [
          "You can ask to access, correct, delete or stop the processing of your information (such as an email you sent). Email us and we will act without delay.",
        ],
      },
      {
        title: "6. Security",
        body: [
          "All traffic is encrypted with HTTPS, admin functions require a separate key, and the server and database are not reachable directly from the internet.",
        ],
      },
      {
        title: "7. Contact",
        body: [`Operator ${CURATOR.name} (Jaeil Ryu) · ${CONTACT_EMAIL}`],
      },
      {
        title: "8. Changes",
        body: [
          "If we add features that use cookies, such as analytics or advertising, we will update this policy and announce it on the site before they take effect.",
        ],
      },
    ],
    effective: `Effective date: ${PRIVACY_EFFECTIVE_DATE}`,
  },
};

export default async function PrivacyPage({ params }: PageProps<"/[lang]/privacy">) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const c = CONTENT[lang];

  return (
    <article className="mx-auto max-w-2xl space-y-8">
      <header className="space-y-3">
        <h1 className="font-pixel text-[22px]">{c.title}</h1>
        <p className="leading-relaxed text-foreground/90">{c.intro}</p>
      </header>
      {c.sections.map((section) => (
        <section key={section.title} className="space-y-2">
          <h2 className="font-bold">{section.title}</h2>
          <ul className="list-disc space-y-1.5 pl-5 text-sm leading-relaxed text-foreground/90">
            {section.body.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </section>
      ))}
      <p className="text-sm text-muted">{c.effective}</p>
    </article>
  );
}
