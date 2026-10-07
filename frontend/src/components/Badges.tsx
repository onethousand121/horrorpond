import Link from "next/link";
import type { GenreSummary } from "@/lib/types";
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

export function CoopBadge({ locale }: { locale: Locale }) {
  return (
    <span className="rounded-full border border-accent-2/35 bg-accent-2/10 px-2 py-0.5 text-xs font-medium text-accent-2">
      {getDictionary(locale).badge.coop}
    </span>
  );
}
