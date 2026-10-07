import { NextResponse, type NextRequest } from "next/server";
import { DEFAULT_LOCALE, isLocale, LOCALE_COOKIE, localePath, stripLocale, type Locale } from "@/lib/i18n";

/**
 * 언어 라우팅. 실제 라우트는 app/[lang] 이다.
 * - /en/...  : 영어. 그대로 통과
 * - /ko/...  : 한국어 정식 주소는 접두사가 없으므로 /... 로 영구 이동
 * - /...     : 언어 전환기로 고른 언어(쿠키)가 영어면 /en/... 로 이동, 아니면 /ko/... 로 rewrite
 *              (첫 화면 "/"만 브라우저 언어도 본다. 공유된 링크는 링크의 언어를 지킨다)
 */
export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;

  if (pathname === `/${DEFAULT_LOCALE}` || pathname.startsWith(`/${DEFAULT_LOCALE}/`)) {
    return NextResponse.redirect(new URL(stripLocale(pathname) + search, request.url), 308);
  }
  if (pathname.split("/")[1] && isLocale(pathname.split("/")[1])) {
    return NextResponse.next();
  }

  const locale = preferredLocale(request, pathname === "/");
  if (locale !== DEFAULT_LOCALE) {
    return NextResponse.redirect(new URL(localePath(locale, pathname) + search, request.url));
  }
  return NextResponse.rewrite(new URL(`/${DEFAULT_LOCALE}${pathname === "/" ? "" : pathname}${search}`, request.url));
}

function preferredLocale(request: NextRequest, useBrowserLanguage: boolean): Locale {
  const chosen = request.cookies.get(LOCALE_COOKIE)?.value;
  if (chosen && isLocale(chosen)) return chosen;
  if (!useBrowserLanguage) return DEFAULT_LOCALE;
  // 브라우저의 첫 번째 언어만 본다. 헤더가 없으면(검색 로봇 등) 한국어
  const first = request.headers.get("accept-language")?.split(",")[0]?.trim().toLowerCase();
  return !first || first.startsWith("ko") ? DEFAULT_LOCALE : "en";
}

export const config = {
  // 정적 파일(_next, 확장자 있는 파일), 관리 화면, API 라우트는 언어와 무관하다
  matcher: ["/((?!_next|admin|api|.*\\..*).*)"],
};
