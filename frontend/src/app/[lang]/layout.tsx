import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Logo } from "@/components/Logo";
import { SearchBox } from "@/components/SearchBox";
import { galmuri, PRETENDARD_CSS } from "@/lib/fonts";
import { getDictionary, isLocale, LOCALES, localePath } from "@/lib/i18n";
import { CURATOR, SITE_INDEXING, SITE_NAME } from "@/lib/site";
import "../globals.css";

export function generateStaticParams() {
  return LOCALES.map((lang) => ({ lang }));
}

export async function generateMetadata({ params }: LayoutProps<"/[lang]">): Promise<Metadata> {
  const { lang } = await params;
  if (!isLocale(lang)) return {};
  const dict = getDictionary(lang);
  return {
    metadataBase: new URL(process.env.SITE_URL ?? "http://localhost:3000"),
    title: {
      default: `${SITE_NAME} — ${dict.site.tagline}`,
      template: `%s | ${SITE_NAME}`,
    },
    description: dict.site.description,
    // 비공개 운영 중에는 모든 페이지를 noindex (lib/site)
    ...(SITE_INDEXING ? {} : { robots: { index: false, follow: false } }),
  };
}

export default async function LocaleLayout({ children, params }: LayoutProps<"/[lang]">) {
  const { lang } = await params;
  if (!isLocale(lang)) notFound();
  const dict = getDictionary(lang);
  const footerLinks = [
    { href: localePath(lang, "/games"), label: dict.nav.games },
    { href: localePath(lang, "/coop"), label: dict.nav.coop },
    { href: localePath(lang, "/about"), label: dict.nav.about },
  ];

  return (
    <html lang={dict.htmlLang} className={`${galmuri.variable} h-full antialiased`}>
      <head>
        <link rel="preconnect" href="https://cdn.jsdelivr.net" crossOrigin="anonymous" />
        <link rel="stylesheet" href={PRETENDARD_CSS} crossOrigin="anonymous" />
      </head>
      <body className="flex min-h-full flex-col font-sans">
        <header className="sticky top-0 z-30 border-b border-border bg-background/85 backdrop-blur">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
            <Link href={localePath(lang, "/")} aria-label={`${SITE_NAME} ${dict.site.home}`}>
              <Logo />
            </Link>
            <SearchBox
              action={localePath(lang, "/games")}
              label={dict.search.label}
              placeholder={dict.search.placeholder}
              className="order-last w-full sm:order-none sm:ml-auto sm:w-64"
            />
            <div className="ml-auto sm:ml-0">
              <LanguageSwitcher current={lang} label={dict.language.label} />
            </div>
          </div>
        </header>
        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">{children}</main>
        <footer className="border-t border-border">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-3 px-4 py-6 text-xs text-muted">
            <nav className="flex flex-wrap gap-x-4 gap-y-2">
              {footerLinks.map((item) => (
                <Link key={item.href} href={item.href} className="hover:text-foreground">
                  {item.label}
                </Link>
              ))}
              <a href={CURATOR.youtubeUrl} target="_blank" rel="noopener noreferrer" className="hover:text-foreground">
                {dict.nav.youtube}
              </a>
            </nav>
            <span>
              {SITE_NAME} · {dict.site.runBy} {dict.site.curatorName} · {dict.site.steamCredit}
            </span>
          </div>
        </footer>
      </body>
    </html>
  );
}
