"use client";

import { usePathname } from "next/navigation";
import { LOCALE_COOKIE, LOCALES, localePath, stripLocale, type Locale } from "@/lib/i18n";

const LABELS: Record<Locale, string> = { ko: "KO", en: "EN" };
const NAMES: Record<Locale, string> = { ko: "한국어", en: "English" };
const ONE_YEAR = 60 * 60 * 24 * 365;

/**
 * 같은 페이지의 다른 언어 주소로 이동한다. 고른 언어는 쿠키에 남겨, 접두사 없는 주소로 와도
 * proxy가 브라우저 언어 대신 이 선택을 따르게 한다.
 *
 * 한국어 주소는 proxy가 /ko 로 rewrite 하므로 서버는 "/ko/games", 브라우저는 "/games"를 본다.
 * 둘 다 언어 접두사를 떼고 만들어 서버와 브라우저의 결과가 같다(hydration 불일치 없음).
 * 검색어 같은 쿼리는 useSearchParams 대신(캐시된 페이지가 클라이언트 렌더링으로 바뀜) 누를 때 붙인다.
 */
export function LanguageSwitcher({ current, label }: { current: Locale; label: string }) {
  const pathname = stripLocale(usePathname());

  return (
    <nav aria-label={label} className="flex items-center gap-1 font-pixel text-[11px]">
      {LOCALES.map((locale) => {
        const href = localePath(locale, pathname);
        return (
          <a
            key={locale}
            href={href}
            hrefLang={locale}
            lang={locale}
            aria-label={NAMES[locale]}
            aria-current={locale === current ? "true" : undefined}
            onClick={(event) => {
              event.preventDefault();
              document.cookie = `${LOCALE_COOKIE}=${locale}; path=/; max-age=${ONE_YEAR}; samesite=lax`;
              window.location.assign(href + window.location.search);
            }}
            className={`rounded px-2 py-1 ${
              locale === current ? "bg-accent/15 text-accent" : "text-muted hover:text-foreground"
            }`}
          >
            {LABELS[locale]}
          </a>
        );
      })}
    </nav>
  );
}
