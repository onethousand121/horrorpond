import Link from "next/link";
import { getDictionary, type Locale } from "@/lib/i18n";

/**
 * page는 0부터 시작하는 백엔드 기준, 화면과 URL(?page=N)은 1부터 보여준다.
 */
export function Pagination({ page, totalPages, hrefFor, locale }: {
  page: number;
  totalPages: number;
  hrefFor: (page: number) => string;
  locale: Locale;
}) {
  if (totalPages <= 1) return null;
  const t = getDictionary(locale).pagination;

  const pages = visiblePages(page, totalPages);
  const linkClass = "rounded border border-border px-3 py-1.5 text-sm hover:border-foreground/40";

  return (
    <nav aria-label={t.label} className="mt-10 flex flex-wrap items-center justify-center gap-2">
      {page > 0 && (
        <Link href={hrefFor(page - 1)} className={linkClass} rel="prev">
          {t.prev}
        </Link>
      )}
      {pages.map((p, i) =>
        p === null ? (
          <span key={`gap-${i}`} className="px-1 text-muted">
            …
          </span>
        ) : (
          <Link
            key={p}
            href={hrefFor(p)}
            aria-current={p === page ? "page" : undefined}
            className={p === page ? `${linkClass} border-accent text-foreground` : `${linkClass} text-muted`}
          >
            {p + 1}
          </Link>
        ),
      )}
      {page < totalPages - 1 && (
        <Link href={hrefFor(page + 1)} className={linkClass} rel="next">
          {t.next}
        </Link>
      )}
    </nav>
  );
}

/** 첫/마지막 페이지와 현재 페이지 주변 2개만 보여주고 나머지는 생략(null). */
function visiblePages(current: number, total: number): (number | null)[] {
  const result: (number | null)[] = [];
  for (let p = 0; p < total; p++) {
    if (p === 0 || p === total - 1 || Math.abs(p - current) <= 2) {
      result.push(p);
    } else if (result[result.length - 1] !== null) {
      result.push(null);
    }
  }
  return result;
}
