import Link from "next/link";
import type { GenreSummary } from "@/lib/types";

const BADGE = "rounded-full border border-border bg-surface-2/60 px-2 py-0.5 text-xs text-muted";

export function GenreBadge({ genre, linked = false }: { genre: GenreSummary; linked?: boolean }) {
  if (!linked) {
    return <span className={BADGE}>{genre.name}</span>;
  }
  return (
    <Link href={`/genres/${genre.slug}`} className={`${BADGE} hover:border-accent/50 hover:text-foreground`}>
      {genre.name}
    </Link>
  );
}

export function CoopBadge() {
  return (
    <span className="rounded-full border border-sky-400/30 bg-sky-400/10 px-2 py-0.5 text-xs font-medium text-sky-300">
      협동
    </span>
  );
}
