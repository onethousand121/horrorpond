"use client";

import { useActionState, useState } from "react";
import type { Genre } from "@/lib/types";
import type { AdminGameDetail } from "@/lib/admin/types";
import { saveCurationAction } from "../actions";
import { ResultMessage } from "./ActionButton";

/** 큐레이터 소유 필드: slug, 장르, (Steam 외 게임만) 협동 여부. 입력은 controlled(ArticleForm 참고). */
export function CurationForm({ game, genres }: { game: AdminGameDetail; genres: Genre[] }) {
  const [state, action, pending] = useActionState(saveCurationAction, null);
  const coopEditable = game.source !== "STEAM";
  const [slug, setSlug] = useState(game.slug);
  const [genreSlugs, setGenreSlugs] = useState(() => new Set(game.genreSlugs));
  const [coop, setCoop] = useState(game.coop);

  function toggleGenre(genreSlug: string, checked: boolean) {
    setGenreSlugs((prev) => {
      const next = new Set(prev);
      if (checked) next.add(genreSlug);
      else next.delete(genreSlug);
      return next;
    });
  }

  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="gameId" value={game.id} />
      <label className="block space-y-1">
        <span className="text-sm font-medium">주소(slug)</span>
        <div className="flex items-center gap-1 text-sm">
          <span className="text-muted">/games/</span>
          <input
            name="slug"
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            required
            maxLength={220}
            pattern="[a-z0-9]+(-[a-z0-9]+)*"
            title="영문 소문자, 숫자, 하이픈(-)만"
            className="flex-1 rounded border border-border bg-background px-3 py-1.5"
          />
        </div>
        <span className="text-xs text-muted">영문 소문자·숫자·하이픈. 공개 후 바꾸면 기존 주소는 404가 됩니다.</span>
      </label>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">장르</legend>
        <div className="flex flex-wrap gap-2">
          {genres.map((genre) => (
            <label
              key={genre.slug}
              className="flex cursor-pointer items-center gap-1.5 rounded border border-border px-2.5 py-1 text-sm has-[:checked]:border-accent has-[:checked]:text-foreground"
            >
              <input
                type="checkbox"
                name="genre"
                value={genre.slug}
                checked={genreSlugs.has(genre.slug)}
                onChange={(e) => toggleGenre(genre.slug, e.target.checked)}
              />
              {genre.name}
            </label>
          ))}
        </div>
      </fieldset>

      {coopEditable ? (
        <label className="flex items-center gap-2 text-sm">
          <input type="hidden" name="coopEditable" value="1" />
          <input type="checkbox" name="coop" checked={coop} onChange={(e) => setCoop(e.target.checked)} />
          협동 플레이 지원
        </label>
      ) : (
        <p className="text-xs text-muted">협동 여부는 Steam 카테고리에서 자동으로 정해집니다 (현재: {game.coop ? "협동" : "싱글"}).</p>
      )}

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "slug·장르 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
    </form>
  );
}
