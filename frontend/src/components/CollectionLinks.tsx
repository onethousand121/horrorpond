import Link from "next/link";
import { collectionsFor, type CollectionSlug } from "@/lib/collections";
import { getDictionary, localePath, type Locale } from "@/lib/i18n";

/** 색인되는 모음 페이지(/collections/{slug})로 가는 링크 줄. current는 지금 보고 있는 모음(빼고 보여준다) */
export function CollectionLinks({ locale, current, label }: { locale: Locale; current?: CollectionSlug; label: string }) {
  const t = getDictionary(locale).collections;
  return (
    <nav aria-label={label} className="flex flex-wrap gap-2">
      {collectionsFor(locale)
        .filter((c) => c.slug !== current)
        .map((c) => (
          <Link
            key={c.slug}
            href={localePath(locale, `/collections/${c.slug}`)}
            className="rounded-full border border-border bg-surface/60 px-3 py-1 text-sm text-muted hover:border-accent/50 hover:text-foreground"
          >
            {t.items[c.slug].title}
          </Link>
        ))}
    </nav>
  );
}
