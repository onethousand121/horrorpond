import Link from "next/link";
import type { GenreSummary } from "@/lib/types";
import { KeeperFrog } from "@/components/KeeperFrog";
import { getDictionary, localePath, type Locale } from "@/lib/i18n";

const BADGE = "rounded-full border border-border bg-surface-2/60 px-2 py-0.5 text-xs text-muted";

export function GenreBadge({ genre, locale, linked = false }: {
  genre: GenreSummary;
  locale: Locale;
  linked?: boolean;
}) {
  if (!linked) {
    return <span className={BADGE}>{genre.name}</span>;
  }
  return (
    <Link href={localePath(locale, `/genres/${genre.slug}`)} className={`${BADGE} hover:border-accent/50 hover:text-foreground`}>
      {genre.name}
    </Link>
  );
}

/** 성인 게임 표시. 흐리게 처리한 이미지 위에 크게 올린다 */
export function AdultBadge({ locale, large = false }: { locale: Locale; large?: boolean }) {
  const t = getDictionary(locale).badge;
  return (
    <span
      aria-label={t.adultLabel}
      className={`inline-flex items-center justify-center rounded-full border-2 border-red-400 bg-background/80 font-pixel text-red-300 ${
        large ? "size-14 text-lg" : "px-1.5 py-0.5 text-[11px]"
      }`}
    >
      {t.adult}
    </span>
  );
}

/** 주인장 추천(추천 글이 있는 게임). 오케이 하는 개구리 + "주인장 추천" */
export function KeeperPickBadge({ locale, className = "" }: { locale: Locale; className?: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded bg-accent-2/90 px-1.5 py-0.5 font-pixel text-[11px] text-accent-ink ${className}`}
    >
      <KeeperFrog className="w-[30px] shrink-0" />
      {getDictionary(locale).site.pick}
    </span>
  );
}

/** 주인장 플레이 후기(추천은 아닌 글). 추천 뱃지와 같은 모양에 다른 색 */
export function KeeperReviewBadge({ locale, className = "" }: { locale: Locale; className?: string }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded border border-border bg-surface-2 px-1.5 py-0.5 font-pixel text-[11px] text-foreground ${className}`}
    >
      <NoteIcon />
      {getDictionary(locale).detail.reviewLabel}
    </span>
  );
}

/** 한국어 화면에서만: 한국어를 지원하면 표시. 음성 지원 여부는 따로 보여주지 않는다 */
export function KoreanBadge({ locale, languages }: {
  locale: Locale;
  languages?: string[];
}) {
  if (locale !== "ko" || !languages?.includes("ko")) return null;
  const t = getDictionary(locale).badge;
  return (
    <span className="rounded-full border border-accent/35 bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">
      {t.korean}
    </span>
  );
}

const CHIP =
  "inline-flex items-center gap-1 rounded bg-background/85 px-1.5 py-0.5 text-[11px] font-medium text-foreground backdrop-blur-sm";

/** 유튜브 재생 버튼 모양 */
function PlayIcon() {
  return (
    <svg viewBox="0 0 20 14" aria-hidden className="h-[11px] w-[15px] shrink-0">
      <rect width="20" height="14" rx="4" fill="#ff0033" />
      <path d="M8 4l5.5 3L8 10z" fill="#ffffff" />
    </svg>
  );
}

/** 메모장 모양 (플레이 후기) */
function NoteIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden className="size-[13px] shrink-0">
      <rect x="2.5" y="1.5" width="11" height="13" rx="1.5" fill="#cfd8c4" />
      <path d="M5 5h6M5 8h6M5 11h4" stroke="#3b4a36" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  );
}

/** 트로피 모양 */
function TrophyIcon() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden className="size-[13px] shrink-0">
      <path
        d="M4 2h8v2h2.5v1.5A3 3 0 0 1 11.8 8.4 4 4 0 0 1 9 10.9V12h2v2H5v-2h2v-1.1A4 4 0 0 1 4.2 8.4 3 3 0 0 1 1.5 5.5V4H4zm0 3.5H3a1.5 1.5 0 0 0 1 1.3zm8 0v1.3a1.5 1.5 0 0 0 1-1.3z"
        fill="#f2b632"
      />
    </svg>
  );
}

/**
 * 주인장이 붙인 콘텐츠 표시 (카드 이미지 오른쪽 위). 모두 같은 칩 모양이고 순서는 협찬 → 추천/후기 → 영상 → 업적.
 * 협찬은 눈에 잘 띄어야 하므로(추천·보증 심사지침) 아이콘 없이 노란 테두리와 글자로 맨 앞에 둔다.
 */
export function ContentBadges({ locale, sponsored, picked, reviewed = false, hasPlayVideo, hasAchievementGuide }: {
  locale: Locale;
  sponsored: boolean;
  picked: boolean;
  reviewed?: boolean;
  hasPlayVideo: boolean;
  hasAchievementGuide: boolean;
}) {
  if (!sponsored && !picked && !reviewed && !hasPlayVideo && !hasAchievementGuide) return null;
  const t = getDictionary(locale);
  return (
    <>
      {sponsored && (
        <span
          className={`${CHIP} border border-amber-400/80 font-bold text-amber-300`}
          title={t.card.sponsoredLabel}
          aria-label={t.card.sponsoredLabel}
        >
          {t.badge.sponsored}
        </span>
      )}
      {picked && (
        <span className={CHIP} title={t.card.hasPick} aria-label={t.card.hasPick}>
          <KeeperFrog className="w-[22px] shrink-0" />
          {t.card.pickShort}
        </span>
      )}
      {reviewed && (
        <span className={CHIP} title={t.card.hasReview} aria-label={t.card.hasReview}>
          <NoteIcon />
          {t.card.reviewShort}
        </span>
      )}
      {hasPlayVideo && (
        <span className={CHIP} title={t.card.hasPlayVideo} aria-label={t.card.hasPlayVideo}>
          <PlayIcon />
          {t.card.videoShort}
        </span>
      )}
      {hasAchievementGuide && (
        <span className={CHIP} title={t.card.hasAchievementGuide} aria-label={t.card.hasAchievementGuide}>
          <TrophyIcon />
          {t.card.achievementShort}
        </span>
      )}
    </>
  );
}

export function CoopBadge({ locale }: { locale: Locale }) {
  return (
    <span className="rounded-full border border-accent-2/35 bg-accent-2/10 px-2 py-0.5 text-xs font-medium text-accent-2">
      {getDictionary(locale).badge.coop}
    </span>
  );
}
