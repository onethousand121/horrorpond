"use client";

import { useActionState, useState } from "react";
import { Markdown } from "@/components/Markdown";
import { ARTICLE_LIMITS, type AdminArticle } from "@/lib/admin/types";
import { clearTranslationAction, saveTranslationAction } from "../actions";
import { ActionButton, ResultMessage } from "./ActionButton";

const inputClass = "w-full rounded border border-border bg-background px-3 py-1.5";

/**
 * 큐레이터 글 영어 번역 (영어 화면 /en 전용, 선택).
 * 번역이 없으면 영어 화면에는 한국어 글이 "한국어로 쓴 글" 안내와 함께 보인다.
 * 한국어 글을 고치면 "갱신 필요"가 뜬다 (번역을 다시 저장하면 사라진다).
 */
export function TranslationForm({ gameId, article }: { gameId: number; article: AdminArticle }) {
  const [state, action, pending] = useActionState(saveTranslationAction, null);
  const [title, setTitle] = useState(article.titleEn ?? "");
  const [oneLiner, setOneLiner] = useState(article.oneLinerEn ?? "");
  const [body, setBody] = useState(article.bodyEn ?? "");
  const [highlights, setHighlights] = useState(() =>
    Array.from({ length: ARTICLE_LIMITS.highlights }, (_, i) => article.highlightsEn?.[i] ?? ""),
  );
  const [preview, setPreview] = useState(false);
  const translated = Boolean(article.titleEn);
  const status = !translated
    ? { text: "번역 없음 · 영어 화면에 한국어 글이 보임", tone: "text-muted" }
    : article.translationStale
      ? { text: "한국어 글이 바뀜 · 번역 갱신 필요", tone: "text-amber-300" }
      : { text: "번역됨", tone: "text-accent" };

  return (
    <details className="rounded border border-border p-3" open={article.translationStale || undefined}>
      <summary className="cursor-pointer text-sm font-medium">
        영어 번역 (영어 화면용, 선택)
        <span className={`ml-2 text-xs font-normal ${status.tone}`}>{status.text}</span>
      </summary>
      <div className="mt-3 space-y-4">
        <p className="text-sm text-muted">
          종류·협찬 표시는 한국어 글을 따릅니다. 한국어 글의 장점 포인트 순서대로 옮겨 주세요. 비워 둔 포인트는 저장하지 않습니다.
        </p>
        <form action={action} className="space-y-4">
          <input type="hidden" name="gameId" value={gameId} />
          <label className="block space-y-1">
            <span className="text-sm font-medium">Title</span>
            <input
              name="titleEn"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              maxLength={ARTICLE_LIMITS.title}
              placeholder={article.title}
              className={inputClass}
            />
          </label>
          <label className="block space-y-1">
            <span className="flex justify-between text-sm font-medium">
              One-liner
              <span className="font-normal text-muted">
                {oneLiner.length}/{ARTICLE_LIMITS.oneLiner}
              </span>
            </span>
            <input
              name="oneLinerEn"
              value={oneLiner}
              onChange={(e) => setOneLiner(e.target.value)}
              required
              maxLength={ARTICLE_LIMITS.oneLiner}
              placeholder={article.oneLiner}
              className={inputClass}
            />
          </label>
          <fieldset className="space-y-2">
            <legend className="text-sm font-medium">Highlights (최대 {ARTICLE_LIMITS.highlights}개)</legend>
            {highlights.map((value, i) => (
              <input
                key={i}
                name="highlightEn"
                value={value}
                onChange={(e) => setHighlights((prev) => prev.map((v, j) => (j === i ? e.target.value : v)))}
                maxLength={ARTICLE_LIMITS.highlight}
                placeholder={article.highlights[i]}
                className={inputClass}
              />
            ))}
          </fieldset>
          <div className="space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">Body (markdown)</span>
              <button type="button" onClick={() => setPreview((p) => !p)} className="text-xs text-muted hover:text-foreground">
                {preview ? "편집으로" : "미리보기"}
              </button>
            </div>
            <textarea
              name="bodyEn"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              rows={14}
              required
              hidden={preview}
              className={`${inputClass} text-sm leading-relaxed`}
            />
            {preview && (
              <div className="min-h-40 rounded border border-border bg-background p-4">
                {body.trim() ? <Markdown>{body}</Markdown> : <p className="text-muted">본문이 비어 있습니다.</p>}
              </div>
            )}
          </div>
          <div className="flex items-center gap-3">
            <button
              type="submit"
              disabled={pending}
              className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
            >
              {pending ? "저장 중…" : "번역 저장"}
            </button>
            {state && <ResultMessage result={state} />}
          </div>
        </form>
        {translated && (
          <ActionButton
            action={clearTranslationAction}
            gameId={gameId}
            label="번역 지우기"
            variant="danger"
            confirmMessage="영어 번역을 지울까요? 영어 화면에는 한국어 글이 보입니다."
          />
        )}
      </div>
    </details>
  );
}
