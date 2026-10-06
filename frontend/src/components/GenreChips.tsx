import Link from "next/link";
import type { Genre } from "@/lib/types";

export function GenreChips({ genres, activeSlug }: { genres: Genre[]; activeSlug?: string }) {
  return (
    <nav aria-label="장르" className="flex flex-wrap gap-2">
      {genres.map((genre) => {
        const active = genre.slug === activeSlug;
        return (
          <Link
            key={genre.slug}
            href={`/genres/${genre.slug}`}
            aria-current={active ? "page" : undefined}
            className={`rounded-full border px-3 py-1 text-sm ${
              active
                ? "border-accent bg-accent text-accent-ink font-medium"
                : "border-border bg-surface/60 text-muted hover:border-accent/50 hover:text-foreground"
            }`}
          >
            {genre.name}
          </Link>
        );
      })}
    </nav>
  );
}
