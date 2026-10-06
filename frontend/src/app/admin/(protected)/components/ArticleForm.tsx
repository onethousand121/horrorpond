"use client";

import { useActionState, useState } from "react";
import { Markdown } from "@/components/Markdown";
import { ARTICLE_LIMITS, type AdminArticle } from "@/lib/admin/types";
import { saveArticleAction } from "../actions";
import { ResultMessage } from "./ActionButton";

const inputClass = "w-full rounded border border-border bg-background px-3 py-1.5";

/**
 * 큐레이션 글. 장점 포인트(highlights)는 최대 5개, 빈 칸은 저장하지 않는다. 공개 중인 글은 저장 즉시 사이트에 반영된다.
 * 입력은 모두 controlled: React 폼 액션은 끝날 때 폼을 리셋하므로, 검증 실패 시 입력이 사라지지 않게 한다.
 */
export function ArticleForm({ gameId, article }: { gameId: number; article: AdminArticle | null }) {
  const [state, action, pending] = useActionState(saveArticleAction, null);
  const [title, setTitle] = useState(article?.title ?? "");
  const [body, setBody] = useState(article?.body ?? "");
  const [oneLiner, setOneLiner] = useState(article?.oneLiner ?? "");
  const [sponsored, setSponsored] = useState(article?.sponsored ?? false);
  const [disclosure, setDisclosure] = useState(article?.sponsorDisclosure ?? "");
  const [highlights, setHighlights] = useState(() =>
    Array.from({ length: ARTICLE_LIMITS.highlights }, (_, i) => article?.highlights[i] ?? ""),
  );
  const [preview, setPreview] = useState(false);

  return (
    <form action={action} className="space-y-4">
      <input type="hidden" name="gameId" value={gameId} />

      <label className="block space-y-1">
        <span className="text-sm font-medium">제목</span>
        <input
          name="title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          maxLength={ARTICLE_LIMITS.title}
          className={inputClass}
        />
      </label>

      <label className="block space-y-1">
        <span className="flex justify-between text-sm font-medium">
          한 줄 소개
          <span className="font-normal text-muted">
            {oneLiner.length}/{ARTICLE_LIMITS.oneLiner}
          </span>
        </span>
        <input
          name="oneLiner"
          value={oneLiner}
          onChange={(e) => setOneLiner(e.target.value)}
          required
          maxLength={ARTICLE_LIMITS.oneLiner}
          className={inputClass}
        />
        <span className="text-xs text-muted">게임 카드와 검색 결과 설명에 쓰입니다.</span>
      </label>

      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">장점 포인트 (공개하려면 1개 이상, 최대 {ARTICLE_LIMITS.highlights}개)</legend>
        {highlights.map((value, i) => (
          <input
            key={i}
            name="highlight"
            value={value}
            onChange={(e) => setHighlights((prev) => prev.map((v, j) => (j === i ? e.target.value : v)))}
            maxLength={ARTICLE_LIMITS.highlight}
            placeholder={i === 0 ? "예: 짧고 굵은 2시간 분량" : undefined}
            className={inputClass}
          />
        ))}
        <span className="text-xs text-muted">각 {ARTICLE_LIMITS.highlight}자 이내. 목록 카드에는 위에서부터 3개가 보입니다.</span>
      </fieldset>

      <div className="space-y-1">
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium">본문 (마크다운)</span>
          <button type="button" onClick={() => setPreview((p) => !p)} className="text-xs text-muted hover:text-foreground">
            {preview ? "편집으로" : "미리보기"}
          </button>
        </div>
        <textarea
          name="body"
          value={body}
          onChange={(e) => setBody(e.target.value)}
          rows={16}
          hidden={preview}
          className={`${inputClass} text-sm leading-relaxed`}
        />
        {preview && (
          <div className="min-h-40 rounded border border-border bg-background p-4">
            {body.trim() ? <Markdown>{body}</Markdown> : <p className="text-muted">본문이 비어 있습니다.</p>}
          </div>
        )}
      </div>

      <fieldset className="space-y-2 rounded border border-border p-3">
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" name="sponsored" checked={sponsored} onChange={(e) => setSponsored(e.target.checked)} />
          협찬·제공받은 콘텐츠 (경제적 이해관계 있음)
        </label>
        {sponsored && (
          <input
            name="sponsorDisclosure"
            value={disclosure}
            onChange={(e) => setDisclosure(e.target.value)}
            required
            maxLength={ARTICLE_LIMITS.disclosure}
            placeholder="예: 개발사로부터 리뷰용 키를 제공받았습니다."
            className={inputClass}
          />
        )}
        <span className="block text-xs text-muted">체크하면 표시 문구가 글에 반드시 함께 노출됩니다.</span>
      </fieldset>

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : article ? "글 저장" : "초안 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
    </form>
  );
}
