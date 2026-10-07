import type { Metadata } from "next";
import Link from "next/link";
import { Logo } from "@/components/Logo";
import { galmuri, PRETENDARD_CSS } from "@/lib/fonts";
import { SITE_NAME } from "@/lib/site";
import "../globals.css";

export const metadata: Metadata = {
  title: { default: `관리자 | ${SITE_NAME}`, template: `%s | ${SITE_NAME}` },
  robots: { index: false, follow: false },
};

/** 관리 화면의 root layout. 공개 사이트(app/[lang])와 언어 구조가 달라 따로 둔다 */
export default function AdminRootLayout({ children }: LayoutProps<"/admin">) {
  return (
    <html lang="ko" className={`${galmuri.variable} h-full antialiased`}>
      <head>
        <link rel="preconnect" href="https://cdn.jsdelivr.net" crossOrigin="anonymous" />
        <link rel="stylesheet" href={PRETENDARD_CSS} crossOrigin="anonymous" />
      </head>
      <body className="flex min-h-full flex-col font-sans">
        <header className="border-b border-border">
          <div className="mx-auto flex max-w-6xl items-center px-4 py-3">
            <Link href="/" aria-label={`${SITE_NAME} 홈`}>
              <Logo />
            </Link>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">{children}</main>
      </body>
    </html>
  );
}
