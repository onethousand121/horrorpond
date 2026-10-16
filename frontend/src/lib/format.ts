import { getDictionary, type Locale } from "./i18n";
import type { DeveloperCredit, DeveloperRole } from "./types";

const SEOUL_DATE = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

/** 영어 날짜: "Oct 6, 2026". LocalDate는 시간대 없는 달력 날짜라 UTC로 읽어 그대로 보여준다 */
const EN_LOCAL_DATE = new Intl.DateTimeFormat("en-US", { timeZone: "UTC", year: "numeric", month: "short", day: "numeric" });
const EN_SEOUL_DATE = new Intl.DateTimeFormat("en-US", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "short",
  day: "numeric",
});

/**
 * 날짜를 한국어는 'yyyy.MM.dd', 영어는 'Oct 6, 2026'으로 표시한다.
 * - "yyyy-MM-dd"(LocalDate)는 시간대가 없는 달력 날짜이므로 변환 없이 그대로 쓴다
 *   (Date로 파싱하면 UTC 자정으로 해석돼 날짜가 밀릴 수 있다).
 * - 그 외 ISO 시각(Instant)은 Asia/Seoul 기준 날짜로 바꾼다.
 */
export function formatDate(value: string, locale: Locale = "ko"): string {
  const localDate = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (localDate) {
    if (locale === "en") return EN_LOCAL_DATE.format(new Date(`${value}T00:00:00Z`));
    return `${localDate[1]}.${localDate[2]}.${localDate[3]}`;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  if (locale === "en") return EN_SEOUL_DATE.format(date);
  // en-CA는 yyyy-MM-dd 형식
  return SEOUL_DATE.format(date).replaceAll("-", ".");
}

/** 오늘 날짜(Asia/Seoul, yyyy-MM-dd) */
export function seoulToday(now: Date = new Date()): string {
  return SEOUL_DATE.format(now);
}

/**
 * 출시일. releaseDate가 없으면 Steam 원문(releaseDateText, 예: "2027년 4분기")을 쓴다.
 * 출시일이 오늘인데 아직 출시 전이면 "오늘 출시 예정"으로 보여준다 (Steam은 날짜만 줘서 시각은 모른다).
 */
export function formatReleaseDate(
  game: { releaseDate: string | null; releaseDateText?: string | null; comingSoon?: boolean },
  locale: Locale = "ko",
): string | null {
  if (game.releaseDate) {
    if (isReleasingToday({ comingSoon: game.comingSoon ?? false, releaseDate: game.releaseDate })) {
      return getDictionary(locale).format.releasingToday;
    }
    return formatDate(game.releaseDate, locale);
  }
  if (game.releaseDateText) return game.releaseDateText;
  return game.comingSoon ? getDictionary(locale).format.comingSoon : null;
}

/**
 * 아직 출시 전인지. Steam은 출시 날짜만 주고 실제 출시는 그날 정해진 시각에 풀리므로, 출시일이 오늘이어도
 * 출시 예정이면 출시 전으로 본다. 출시일이 지났는데 출시 예정으로 남은 게임은 갱신이 늦은 것으로 보고 출시된 것으로 본다
 * (백엔드 ReleaseWindow와 같은 규칙).
 */
export function isUpcoming(game: { comingSoon: boolean; releaseDate: string | null }, today: string = seoulToday()): boolean {
  if (!game.comingSoon) return false;
  return !game.releaseDate || game.releaseDate >= today;
}

/** 출시일이 오늘인데 아직 출시 전 */
export function isReleasingToday(game: { comingSoon: boolean; releaseDate: string | null }, today: string = seoulToday()): boolean {
  return game.comingSoon && game.releaseDate === today;
}

export interface DeveloperGroup {
  name: string;
  slug: string;
  label: string;
}

/**
 * 같은 회사가 개발과 배급을 모두 했으면 "개발·배급"으로 한 번만 보여준다.
 * 순서는 백엔드 응답 순서(개발 → 배급)를 따른다.
 */
export function groupDevelopers(credits: DeveloperCredit[], locale: Locale = "ko"): DeveloperGroup[] {
  const labels = getDictionary(locale).developers;
  const roleLabel: Record<DeveloperRole, string> = { DEVELOPER: labels.developer, PUBLISHER: labels.publisher };
  const groups = new Map<string, { slug: string; roles: Set<DeveloperRole> }>();
  for (const credit of credits) {
    const group = groups.get(credit.name) ?? { slug: credit.slug, roles: new Set<DeveloperRole>() };
    group.roles.add(credit.role);
    groups.set(credit.name, group);
  }
  return [...groups].map(([name, { slug, roles }]) => ({
    name,
    slug,
    label:
      roles.has("DEVELOPER") && roles.has("PUBLISHER")
        ? labels.both
        : roleLabel[roles.has("DEVELOPER") ? "DEVELOPER" : "PUBLISHER"],
  }));
}

const EN_COMPACT = new Intl.NumberFormat("en-US", { notation: "compact", maximumFractionDigits: 1 });

/** 리뷰 수를 짧게: 950 → "950", 12,300 → "1.2만", 684,837 → "68만" (영어는 "12.3K", "684.8K") */
export function formatCount(n: number, locale: Locale = "ko"): string {
  if (locale === "en") return EN_COMPACT.format(n);
  if (n < 10_000) return n.toLocaleString("ko-KR");
  const man = n / 10_000;
  return `${man < 10 ? man.toFixed(1).replace(/\.0$/, "") : Math.round(man)}만`;
}

/** ISO 시각 → "3일 전" / "3 days ago" 같은 상대 시간 (오늘 기준, 대략) */
export function formatRelative(iso: string, locale: Locale = "ko", now: Date = new Date()): string {
  const days = Math.floor((now.getTime() - new Date(iso).getTime()) / 86_400_000);
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });
  if (days < 1) return rtf.format(0, "day");
  if (days < 7) return rtf.format(-days, "day");
  if (days < 30) return rtf.format(-Math.floor(days / 7), "week");
  if (days < 365) return rtf.format(-Math.floor(days / 30), "month");
  return rtf.format(-Math.floor(days / 365), "year");
}
