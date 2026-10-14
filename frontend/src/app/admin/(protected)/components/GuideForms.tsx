"use client";

import { useActionState, useState } from "react";
import { GUIDE_LIMITS } from "@/lib/admin/types";
import type { AchievementGuide, EndingGuide, EndingType, KoreanPatch, PlayVideo } from "@/lib/types";
import {
  saveAchievementsAction,
  saveEndingsAction,
  saveKoreanPatchesAction,
  savePlayVideosAction,
  saveReferenceVideosAction,
} from "../actions";
import { ResultMessage } from "./ActionButton";

const inputClass = "w-full rounded border border-border bg-background px-3 py-1.5 text-sm";
const smallButton = "rounded border border-border px-2 py-1 text-xs text-muted hover:text-foreground disabled:opacity-30";

/** 목록 행 편집 공통: 추가·삭제·위아래 이동. 입력은 controlled (폼 액션 후 리셋돼도 값이 남게) */
function useRows<T>(initial: T[], empty: T) {
  const [rows, setRows] = useState<T[]>(initial.length > 0 ? initial : [empty]);
  return {
    rows,
    update: (index: number, patch: Partial<T>) =>
      setRows((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row))),
    add: () => setRows((prev) => [...prev, empty]),
    remove: (index: number) => setRows((prev) => (prev.length === 1 ? [empty] : prev.filter((_, i) => i !== index))),
    move: (index: number, delta: -1 | 1) =>
      setRows((prev) => {
        const next = [...prev];
        const target = index + delta;
        if (target < 0 || target >= next.length) return prev;
        [next[index], next[target]] = [next[target], next[index]];
        return next;
      }),
  };
}

function RowControls({ index, count, onMove, onRemove }: {
  index: number;
  count: number;
  onMove: (delta: -1 | 1) => void;
  onRemove: () => void;
}) {
  return (
    <div className="flex shrink-0 gap-1">
      <button type="button" className={smallButton} disabled={index === 0} onClick={() => onMove(-1)} aria-label="위로">
        ↑
      </button>
      <button
        type="button"
        className={smallButton}
        disabled={index === count - 1}
        onClick={() => onMove(1)}
        aria-label="아래로"
      >
        ↓
      </button>
      <button type="button" className={smallButton} onClick={onRemove} aria-label="삭제">
        ✕
      </button>
    </div>
  );
}

function youtubeUrl(id: string | null): string {
  return id ? `https://youtu.be/${id}` : "";
}

/**
 * 플레이 영상 목록. 저장하면 목록 전체가 바뀐다(빈 목록이면 사이트에서 영역이 사라짐).
 * 제목을 비우면 저장할 때 유튜브 영상 제목을 가져온다.
 */
export function PlayVideoForm({ gameId, videos }: { gameId: number; videos: PlayVideo[] }) {
  const [state, action, pending] = useActionState(savePlayVideosAction, null);
  const { rows, update, add, remove, move } = useRows(
    videos.map((v) => ({ url: youtubeUrl(v.youtubeId), title: v.title ?? "" })),
    { url: "", title: "" },
  );

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="gameId" value={gameId} />
      {rows.map((row, i) => (
        <div key={i} className="flex flex-col gap-2 rounded border border-border p-3 sm:flex-row sm:items-center">
          <input
            name="videoUrl"
            value={row.url}
            onChange={(e) => update(i, { url: e.target.value })}
            placeholder="https://youtu.be/..."
            className={`${inputClass} sm:w-80`}
          />
          <input
            name="videoTitle"
            value={row.title}
            onChange={(e) => update(i, { title: e.target.value })}
            maxLength={GUIDE_LIMITS.videoTitle}
            placeholder="제목 (비우면 유튜브 제목)"
            className={inputClass}
          />
          <RowControls index={i} count={rows.length} onMove={(d) => move(i, d)} onRemove={() => remove(i)} />
        </div>
      ))}
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={add}
          disabled={rows.length >= GUIDE_LIMITS.playVideos}
          className="rounded border border-dashed border-border px-3 py-1.5 text-sm text-muted hover:text-foreground disabled:opacity-40"
        >
          + 영상 추가
        </button>
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "영상 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
      <p className="text-xs text-muted">
        최대 {GUIDE_LIMITS.playVideos}개, 위에서부터 보입니다. 주소가 빈 행은 저장하지 않습니다.
      </p>
    </form>
  );
}

/**
 * 다른 채널의 참고 영상. 채널 이름은 사이트에 출처로 보인다(비우면 유튜브에서 가져온다).
 * 주인장 영상이 있으면 사이트에 보이지 않는다.
 */
export function ReferenceVideoForm({ gameId, videos }: { gameId: number; videos: PlayVideo[] }) {
  const [state, action, pending] = useActionState(saveReferenceVideosAction, null);
  const { rows, update, add, remove, move } = useRows(
    videos.map((v) => ({ url: youtubeUrl(v.youtubeId), title: v.title ?? "", channel: v.channelName ?? "" })),
    { url: "", title: "", channel: "" },
  );

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="gameId" value={gameId} />
      {rows.map((row, i) => (
        <div key={i} className="flex flex-col gap-2 rounded border border-border p-3 sm:flex-row sm:items-center">
          <input
            name="videoUrl"
            value={row.url}
            onChange={(e) => update(i, { url: e.target.value })}
            placeholder="https://youtu.be/..."
            className={`${inputClass} sm:w-72`}
          />
          <input
            name="videoTitle"
            value={row.title}
            onChange={(e) => update(i, { title: e.target.value })}
            maxLength={GUIDE_LIMITS.videoTitle}
            placeholder="제목 (비우면 유튜브 제목)"
            className={inputClass}
          />
          <input
            name="videoChannel"
            value={row.channel}
            onChange={(e) => update(i, { channel: e.target.value })}
            maxLength={100}
            placeholder="채널 이름 (비우면 유튜브에서)"
            className={`${inputClass} sm:w-48`}
          />
          <RowControls index={i} count={rows.length} onMove={(d) => move(i, d)} onRemove={() => remove(i)} />
        </div>
      ))}
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={add}
          disabled={rows.length >= GUIDE_LIMITS.playVideos}
          className="rounded border border-dashed border-border px-3 py-1.5 text-sm text-muted hover:text-foreground disabled:opacity-40"
        >
          + 영상 추가
        </button>
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "참고 영상 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
    </form>
  );
}

/**
 * 업적 공략 목록. 이름은 필수, 설명과 공략 영상은 선택.
 */
export function AchievementForm({ gameId, achievements }: { gameId: number; achievements: AchievementGuide[] }) {
  const [state, action, pending] = useActionState(saveAchievementsAction, null);
  const { rows, update, add, remove, move } = useRows(
    achievements.map((a) => ({ name: a.name, description: a.description ?? "", videoUrl: youtubeUrl(a.youtubeId) })),
    { name: "", description: "", videoUrl: "" },
  );

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="gameId" value={gameId} />
      {rows.map((row, i) => (
        <div key={i} className="space-y-2 rounded border border-border p-3">
          <div className="flex items-center gap-2">
            <input
              name="achievementName"
              value={row.name}
              onChange={(e) => update(i, { name: e.target.value })}
              maxLength={GUIDE_LIMITS.achievementName}
              placeholder="업적 이름"
              className={`${inputClass} font-medium`}
            />
            <RowControls index={i} count={rows.length} onMove={(d) => move(i, d)} onRemove={() => remove(i)} />
          </div>
          <textarea
            name="achievementDescription"
            value={row.description}
            onChange={(e) => update(i, { description: e.target.value })}
            maxLength={GUIDE_LIMITS.achievementDescription}
            rows={2}
            placeholder="달성 방법 (선택)"
            className={inputClass}
          />
          <input
            name="achievementVideoUrl"
            value={row.videoUrl}
            onChange={(e) => update(i, { videoUrl: e.target.value })}
            placeholder="공략 영상 주소 (선택)"
            className={inputClass}
          />
        </div>
      ))}
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={add}
          disabled={rows.length >= GUIDE_LIMITS.achievements}
          className="rounded border border-dashed border-border px-3 py-1.5 text-sm text-muted hover:text-foreground disabled:opacity-40"
        >
          + 업적 추가
        </button>
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "업적 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
      <p className="text-xs text-muted">이름이 빈 행은 저장하지 않습니다.</p>
    </form>
  );
}

const ENDING_TYPES: { value: EndingType | ""; label: string }[] = [
  { value: "", label: "표시 안 함" },
  { value: "SINGLE", label: "단일 엔딩" },
  { value: "MULTIPLE", label: "멀티 엔딩" },
];

/**
 * 엔딩 구분과 엔딩 목록. 구분과 목록을 한 번에 저장한다(목록은 멀티 엔딩일 때만, 다른 구분으로 저장하면 목록은 지워진다).
 * 사이트에는 스포일러로 접혀서 보인다.
 */
export function EndingForm({ gameId, endingType, endings }: {
  gameId: number;
  endingType: EndingType | null;
  endings: EndingGuide[];
}) {
  const [state, action, pending] = useActionState(saveEndingsAction, null);
  const [type, setType] = useState<EndingType | "">(endingType ?? "");
  const { rows, update, add, remove, move } = useRows(
    endings.map((e) => ({ name: e.name, description: e.description ?? "", videoUrl: youtubeUrl(e.youtubeId) })),
    { name: "", description: "", videoUrl: "" },
  );

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="gameId" value={gameId} />
      <fieldset className="flex flex-wrap gap-4 text-sm">
        <legend className="sr-only">엔딩 구분</legend>
        {ENDING_TYPES.map((option) => (
          <label key={option.value} className="flex items-center gap-1.5">
            <input
              type="radio"
              name="endingType"
              value={option.value}
              checked={type === option.value}
              onChange={() => setType(option.value)}
            />
            {option.label}
          </label>
        ))}
      </fieldset>
      {type === "MULTIPLE" && (
        <>
          {rows.map((row, i) => (
            <div key={i} className="space-y-2 rounded border border-border p-3">
              <div className="flex items-center gap-2">
                <input
                  name="endingName"
                  value={row.name}
                  onChange={(e) => update(i, { name: e.target.value })}
                  maxLength={GUIDE_LIMITS.endingName}
                  placeholder="엔딩 이름"
                  className={`${inputClass} font-medium`}
                />
                <RowControls index={i} count={rows.length} onMove={(d) => move(i, d)} onRemove={() => remove(i)} />
              </div>
              <textarea
                name="endingDescription"
                value={row.description}
                onChange={(e) => update(i, { description: e.target.value })}
                maxLength={GUIDE_LIMITS.endingDescription}
                rows={3}
                placeholder="보는 방법 (선택)"
                className={inputClass}
              />
              <input
                name="endingVideoUrl"
                value={row.videoUrl}
                onChange={(e) => update(i, { videoUrl: e.target.value })}
                placeholder="엔딩 영상 주소 (선택, 쇼츠 가능)"
                className={inputClass}
              />
            </div>
          ))}
        </>
      )}
      <div className="flex flex-wrap items-center gap-3">
        {type === "MULTIPLE" && (
          <button
            type="button"
            onClick={add}
            disabled={rows.length >= GUIDE_LIMITS.endings}
            className="rounded border border-dashed border-border px-3 py-1.5 text-sm text-muted hover:text-foreground disabled:opacity-40"
          >
            + 엔딩 추가
          </button>
        )}
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "엔딩 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
      <p className="text-xs text-muted">
        글 저장과 따로 저장됩니다. 멀티 엔딩이 아니면 엔딩 목록은 지워집니다. 이름이 빈 행은 저장하지 않습니다.
      </p>
    </form>
  );
}

/**
 * 유저 한글패치 목록. 배포처 주소(https)만 링크하고 파일은 두지 않는다.
 */
export function KoreanPatchForm({ gameId, patches }: { gameId: number; patches: KoreanPatch[] }) {
  const [state, action, pending] = useActionState(saveKoreanPatchesAction, null);
  const { rows, update, add, remove, move } = useRows(
    patches.map((p) => ({ url: p.url, author: p.author ?? "", note: p.note ?? "" })),
    { url: "", author: "", note: "" },
  );

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="gameId" value={gameId} />
      {rows.map((row, i) => (
        <div key={i} className="flex flex-col gap-2 rounded border border-border p-3 sm:flex-row sm:items-center">
          <input
            name="patchUrl"
            type="url"
            value={row.url}
            onChange={(e) => update(i, { url: e.target.value })}
            maxLength={GUIDE_LIMITS.patchUrl}
            placeholder="https://배포처 주소"
            className={`${inputClass} sm:w-80`}
          />
          <input
            name="patchAuthor"
            value={row.author}
            onChange={(e) => update(i, { author: e.target.value })}
            maxLength={GUIDE_LIMITS.patchAuthor}
            placeholder="제작자 (선택)"
            className={`${inputClass} sm:w-40`}
          />
          <input
            name="patchNote"
            value={row.note}
            onChange={(e) => update(i, { note: e.target.value })}
            maxLength={GUIDE_LIMITS.patchNote}
            placeholder="메모 (선택, 예: v1.2 기준)"
            className={inputClass}
          />
          <RowControls index={i} count={rows.length} onMove={(d) => move(i, d)} onRemove={() => remove(i)} />
        </div>
      ))}
      <div className="flex flex-wrap items-center gap-3">
        <button
          type="button"
          onClick={add}
          disabled={rows.length >= GUIDE_LIMITS.koreanPatches}
          className="rounded border border-dashed border-border px-3 py-1.5 text-sm text-muted hover:text-foreground disabled:opacity-40"
        >
          + 한글패치 추가
        </button>
        <button
          type="submit"
          disabled={pending}
          className="rounded border border-border px-4 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
        >
          {pending ? "저장 중…" : "한글패치 저장"}
        </button>
        {state && <ResultMessage result={state} />}
      </div>
      <p className="text-xs text-muted">https 주소만 받습니다. 주소가 빈 행은 저장하지 않습니다.</p>
    </form>
  );
}
