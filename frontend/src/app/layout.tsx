import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-noto-sans-kr",
  weight: ["400", "500", "700"],
  subsets: ["latin"],
  display: "swap",
});

const SITE_NAME = "horrorpond";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL ?? "http://localhost:3000"),
  title: {
    default: `${SITE_NAME} — 큐레이터가 고른 공포게임`,
    template: `%s | ${SITE_NAME}`,
  },
  description: "큐레이터가 직접 플레이하고 선별한 공포게임만 소개합니다.",
};

const NAV = [
  { href: "/games", label: "전체 게임" },
  { href: "/coop", label: "협동 공포게임" },
  { href: "/about", label: "소개" },
];

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${notoSansKr.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        <header className="border-b border-border">
          <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-4">
            <Link href="/" className="text-lg font-bold tracking-tight">
              horror<span className="text-accent">pond</span>
            </Link>
            <nav className="flex gap-4 text-sm text-muted">
              {NAV.map((item) => (
                <Link key={item.href} href={item.href} className="hover:text-foreground">
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">{children}</main>
        <footer className="border-t border-border">
          <div className="mx-auto max-w-6xl px-4 py-6 text-xs text-muted">
            게임 정보와 이미지는 Steam에서 제공됩니다. © {SITE_NAME}
          </div>
        </footer>
      </body>
    </html>
  );
}
