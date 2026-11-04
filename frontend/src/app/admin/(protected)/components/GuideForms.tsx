"use client";

import Image from "next/image";
import { useActionState, useState, useTransition } from "react";
import { GUIDE_LIMITS } from "@/lib/admin/types";
import type { AchievementGuide, EndingGuide, EndingType, KoreanPatch, PlayVideo } from "@/lib/types";
import {
  loadSteamAchievementsAction,
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
    replace: (next: T[]) => setRows(next.length > 0 ? next : [empty]),
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

/** 입력 중인 값은 미리보기하지 않는다 (이미지 컴포넌트는 절대 주소가 아니면 오류를 낸다) */
function isPreviewableIcon(value: string): boolean {
  if (!value.startsWith("https://")) return false;
  try {
    new URL(value);
    return true;
  } catch {
    return false;
  }
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
 * 업적 공략 목록. 이름은 필수, 설명·공략 영상·아이콘은 선택.
 * Steam 게임은 "Steam에서 불러오기"로 이름·설명(영어)·아이콘을 채울 수 있다(저장은 따로).
 */
export function AchievementForm({ gameId, steamAppId, achievements }: {
  gameId: number;
  /** Steam 게임이면 앱 ID. 없으면 불러오기 버튼을 숨긴다 */
  steamAppId: number | null;
  achievements: AchievementGuide[];
}) {
  const [state, action, pending] = useActionState(saveAchievementsAction, null);
  const [loadMessage, setLoadMessage] = useState<{ ok: boolean; message: string } | null>(null);
  const [loading, startLoading] = useTransition();
  const { rows, update, add, remove, move, replace } = useRows(
    achievements.map((a) => ({
      name: a.name,
      description: a.description ?? "",
      videoUrl: youtubeUrl(a.youtubeId),
      iconUrl: a.iconUrl ?? "",
    })),
    { name: "", description: "", videoUrl: "", iconUrl: "" },
  );

  function loadFromSteam() {
    if (steamAppId == null) return;
    startLoading(async () => {
      const result = await loadSteamAchievementsAction(steamAppId);
      if (!result.ok) {
        setLoadMessage({ ok: false, message: result.message });
        return;
      }
      if (result.achievements.length === 0) {
        setLoadMessage({ ok: false, message: "Steam에 등록된 업적이 없습니다." });
        return;
      }
      // 이름이 같은 행은 비어 있는 아이콘만 채우고, 없는 업적은 뒤에 붙인다 (적어 둔 설명·영상은 그대로)
      const filled = rows.filter((row) => row.name.trim());
      const key = (name: string) => name.trim().toLowerCase();
      const byName = new Map(result.achievements.map((a) => [key(a.name), a]));
      let icons = 0;
      const merged = filled.map((row) => {
        const steam = byName.get(key(row.name));
        if (steam?.iconUrl && !row.iconUrl) {
          icons++;
          return { ...row, iconUrl: steam.iconUrl };
        }
        return row;
      });
      const known = new Set(filled.map((row) => key(row.name)));
      const added = result.achievements
        .filter((a) => !known.has(key(a.name)))
        .slice(0, Math.max(0, GUIDE_LIMITS.achievements - merged.length))
        .map((a) => ({
          name: a.name.slice(0, GUIDE_LIMITS.achievementName),
          description: (a.description ?? "").slice(0, GUIDE_LIMITS.achievementDescription),
          videoUrl: "",
          iconUrl: a.iconUrl ?? "",
        }));
      replace([...merged, ...added]);
      setLoadMessage({
        ok: true,
        message: `Steam 업적 ${result.achievements.length}개: 새로 ${added.length}개 추가, 아이콘 ${icons}개 채움. 확인 후 "업적 저장"을 눌러 주세요.`,
      });
    });
  }

  return (
    <form action={action} className="space-y-3">
      <input type="hidden" name="gameId" value={gameId} />
      {steamAppId != null && (
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={loadFromSteam}
            disabled={loading}
            className="rounded border border-border px-3 py-1.5 text-sm hover:border-foreground/40 disabled:opacity-50"
          >
            {loading ? "불러오는 중…" : "Steam에서 불러오기"}
          </button>
          {loadMessage && <ResultMessage result={loadMessage} />}
        </div>
      )}
      {rows.map((row, i) => (
        <div key={i} className="space-y-2 rounded border border-border p-3">
          <div className="flex items-center gap-2">
            {isPreviewableIcon(row.iconUrl) && (
              <Image src={row.iconUrl} alt="" width={36} height={36} className="size-9 shrink-0 rounded" />
            )}
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
          <input
            name="achievementIconUrl"
            value={row.iconUrl}
            onChange={(e) => update(i, { iconUrl: e.target.value })}
            maxLength={GUIDE_LIMITS.achievementIconUrl}
            placeholder="아이콘 이미지 주소 (선택, Steam 업적 이미지 주소)"
            className={`${inputClass} font-mono text-xs`}
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
      <p className="text-xs text-muted">
        이름이 빈 행은 저장하지 않습니다. 아이콘은 Steam 업적 이미지 주소만 받습니다 (Steam 업적 페이지에서 이미지 우클릭 →
        이미지 주소 복사). 불러온 이름·설명은 영어라 필요하면 고쳐 주세요.
      </p>
    </form>
  );
}

const ENDING_TYPES: { value: EndingType | ""; label: string }[] = [
  { value: "", label: "표시 안 함" },
  { value: "SINGLE", label: "단일 엔딩" },
  { value: "MULTIPLE", label: "멀티 엔딩" },
];

/**
 * 엔딩 구분과 엔딩 목록. 구분과 목록을 한 번에 저장한다. 멀티 엔딩은 여러 개, 단일 엔딩은 엔딩 설명 1개(선택),
 * 표시 안 함이면 목록은 지워진다. 사이트에는 스포일러로 접혀서 보인다.
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
      {(type === "MULTIPLE" || type === "SINGLE") && (
        <>
          {(type === "SINGLE" ? rows.slice(0, 1) : rows).map((row, i) => (
            <div key={i} className="space-y-2 rounded border border-border p-3">
              <div className="flex items-center gap-2">
                <input
                  name="endingName"
                  value={row.name}
                  onChange={(e) => update(i, { name: e.target.value })}
                  maxLength={GUIDE_LIMITS.endingName}
                  placeholder={type === "SINGLE" ? "엔딩 이름 (예: 엔딩, 비워 두면 설명 없이 저장)" : "엔딩 이름"}
                  className={`${inputClass} font-medium`}
                />
                {type === "MULTIPLE" && (
                  <RowControls index={i} count={rows.length} onMove={(d) => move(i, d)} onRemove={() => remove(i)} />
                )}
              </div>
              <textarea
                name="endingDescription"
                value={row.description}
                onChange={(e) => update(i, { description: e.target.value })}
                maxLength={GUIDE_LIMITS.endingDescription}
                rows={3}
                placeholder={type === "SINGLE" ? "엔딩 내용·보는 방법 (스포일러, 선택)" : "보는 방법 (선택)"}
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
        글 저장과 따로 저장됩니다. 단일 엔딩은 첫 번째 엔딩 하나만, 표시 안 함이면 목록은 지워집니다. 이름이 빈 행은
        저장하지 않습니다.
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
