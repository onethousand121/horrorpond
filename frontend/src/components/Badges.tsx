import Link from "next/link";
import type { GenreSummary } from "@/lib/types";

export function GenreBadge({ genre, linked = false }: { genre: GenreSummary; linked?: boolean }) {
  const className = "rounded bg-surface px-1.5 py-0.5 text-xs text-muted";
  if (!linked) {
    return <span className={className}>{genre.name}</span>;
  }
  return (
    <Link href={`/genres/${genre.slug}`} className={`${className} hover:text-foreground`}>
      {genre.name}
    </Link>
  );
}

export function CoopBadge() {
  return (
    <span className="rounded bg-sky-500/15 px-1.5 py-0.5 text-xs font-medium text-sky-300">협동</span>
  );
}
