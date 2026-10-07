import { getDictionary, type Locale } from "@/lib/i18n";

/** 협찬 콘텐츠 표시. 카드와 상세 어디서든 눈에 띄게 보여야 한다. */
export function SponsorBadge({ locale }: { locale: Locale }) {
  return (
    <span className="inline-flex items-center rounded border border-amber-500/60 bg-amber-500/15 px-1.5 py-0.5 text-xs font-bold text-amber-300">
      {getDictionary(locale).badge.sponsored}
    </span>
  );
}
