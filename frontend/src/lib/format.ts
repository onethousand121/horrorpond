import type { DeveloperCredit, DeveloperRole } from "./types";

const SEOUL_DATE = new Intl.DateTimeFormat("en-CA", {
  timeZone: "Asia/Seoul",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
});

/**
 * 날짜를 'yyyy.MM.dd'로 표시한다.
 * - "yyyy-MM-dd"(LocalDate)는 시간대가 없는 달력 날짜이므로 변환 없이 그대로 쓴다
 *   (Date로 파싱하면 UTC 자정으로 해석돼 날짜가 밀릴 수 있다).
 * - 그 외 ISO 시각(Instant)은 Asia/Seoul 기준 날짜로 바꾼다.
 */
export function formatDate(value: string): string {
  const localDate = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (localDate) {
    return `${localDate[1]}.${localDate[2]}.${localDate[3]}`;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  // en-CA는 yyyy-MM-dd 형식
  return SEOUL_DATE.format(date).replaceAll("-", ".");
}

/** 출시일. releaseDate가 없으면 Steam 원문(releaseDateText, 예: "2027년 4분기")을 쓴다. */
export function formatReleaseDate(game: {
  releaseDate: string | null;
  releaseDateText?: string | null;
  comingSoon?: boolean;
}): string | null {
  if (game.releaseDate) return formatDate(game.releaseDate);
  if (game.releaseDateText) return game.releaseDateText;
  return game.comingSoon ? "출시 예정" : null;
}

export interface DeveloperGroup {
  name: string;
  slug: string;
  label: string;
}

const ROLE_LABEL: Record<DeveloperRole, string> = {
  DEVELOPER: "개발",
  PUBLISHER: "배급",
};

/**
 * 같은 회사가 개발과 배급을 모두 했으면 "개발·배급"으로 한 번만 보여준다.
 * 순서는 백엔드 응답 순서(개발 → 배급)를 따른다.
 */
export function groupDevelopers(credits: DeveloperCredit[]): DeveloperGroup[] {
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
        ? "개발·배급"
        : ROLE_LABEL[roles.has("DEVELOPER") ? "DEVELOPER" : "PUBLISHER"],
  }));
}

/** 리뷰 수를 짧게: 950 → "950", 12,300 → "1.2만", 684,837 → "68만" */
export function formatCount(n: number): string {
  if (n < 10_000) return n.toLocaleString("ko-KR");
  const man = n / 10_000;
  return `${man < 10 ? man.toFixed(1).replace(/\.0$/, "") : Math.round(man)}만`;
}

/** ISO 시각 → "3일 전", "2주 전" 같은 상대 시간 (오늘 기준, 대략) */
export function formatRelative(iso: string, now: Date = new Date()): string {
  const days = Math.floor((now.getTime() - new Date(iso).getTime()) / 86_400_000);
  if (days < 1) return "오늘";
  if (days < 7) return `${days}일 전`;
  if (days < 30) return `${Math.floor(days / 7)}주 전`;
  if (days < 365) return `${Math.floor(days / 30)}개월 전`;
  return `${Math.floor(days / 365)}년 전`;
}
