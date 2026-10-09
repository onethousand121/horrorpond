import type { Locale } from "./i18n";

/** 상세 페이지에 이름으로 다 보여줄 언어 수. 나머지는 "외 N개" */
export const VISIBLE_LANGUAGES = 6;

/**
 * Steam 지원 언어 코드(ko, en, zh-Hans, es-419 …)를 화면 언어 이름으로. 브라우저·Node 내장 언어 이름을 쓴다.
 */
export function languageName(code: string, locale: Locale): string {
  try {
    return new Intl.DisplayNames([locale], { type: "language" }).of(code) ?? code;
  } catch {
    return code;
  }
}

/** 보는 사람 언어를 맨 앞에(한국어 화면은 한국어, 영어 화면은 영어), 나머지는 Steam 순서 그대로 */
export function orderLanguages(codes: readonly string[], locale: Locale): string[] {
  const first = locale === "ko" ? ["ko", "en"] : ["en"];
  return [...first.filter((code) => codes.includes(code)), ...codes.filter((code) => !first.includes(code))];
}
