import type { Metadata } from "next";
import localFont from "next/font/local";
import Link from "next/link";
import { Logo } from "@/components/Logo";
import { CURATOR, SITE_INDEXING, SITE_NAME, SITE_TAGLINE } from "@/lib/site";
import "./globals.css";

/** 제목·장식용 픽셀 폰트 (갈무리11 Bold, OFL. src/fonts/galmuri/OFL.md) */
const galmuri = localFont({
  src: "../fonts/galmuri/Galmuri11-Bold.woff2",
  weight: "700",
  variable: "--font-galmuri",
  display: "swap",
});

/** 본문 폰트 Pretendard: 페이지에 쓰인 글자 묶음만 내려받는 동적 서브셋 (공식 권장 방식) */
const PRETENDARD_CSS =
  "https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL ?? "http://localhost:3000"),
  title: {
    default: `${SITE_NAME} — ${SITE_TAGLINE}`,
    template: `%s | ${SITE_NAME}`,
  },
  description: `큐레이터 ${CURATOR.name}이 직접 플레이하고 건져 올린 공포게임만 소개합니다.`,
  // 비공개 운영 중에는 모든 페이지를 noindex (lib/site)
  ...(SITE_INDEXING ? {} : { robots: { index: false, follow: false } }),
};

const NAV = [
  { href: "/games", label: "게임" },
  { href: "/coop", label: "협동" },
  { href: "/about", label: "소개" },
];

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${galmuri.variable} h-full antialiased`}>
      <head>
        <link rel="preconnect" href="https://cdn.jsdelivr.net" crossOrigin="anonymous" />
        <link rel="stylesheet" href={PRETENDARD_CSS} crossOrigin="anonymous" />
      </head>
      <body className="flex min-h-full flex-col font-sans">
        <header className="sticky top-0 z-30 border-b border-border bg-background/85 backdrop-blur">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
            <Link href="/" aria-label={`${SITE_NAME} 홈`}>
              <Logo />
            </Link>
            <nav className="flex gap-5 font-pixel text-[11px] text-muted">
              {NAV.map((item) => (
                <Link key={item.href} href={item.href} className="hover:text-accent">
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">{children}</main>
        <footer className="border-t border-border">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-4 py-6 text-xs text-muted">
            <span>
              {SITE_NAME} · 큐레이터 {CURATOR.name} ·{" "}
              <a href={CURATOR.youtubeUrl} target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                YouTube
              </a>
            </span>
            <span>게임 정보와 이미지는 Steam에서 제공됩니다.</span>
          </div>
        </footer>
      </body>
    </html>
  );
}
