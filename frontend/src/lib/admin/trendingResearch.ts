/**
 * 매주 Perplexity 조사 결과(JSON)를 읽는다. 형식은 관리 화면 "지금 뜨는 조사 반영"에 적힌 프롬프트와 같다.
 * 모델 출력이라 코드펜스·앞뒤 설명이 붙어 와도 첫 "{"부터 마지막 "}"까지를 JSON으로 본다.
 */

export interface ResearchVideo {
  url: string;
  title: string | null;
  channel: string | null;
  originalChannel: "verified" | "unverified";
}

export interface ResearchGame {
  title: string;
  steamAppId: number | null;
  itchUrl: string | null;
  why: string | null;
  confidence: "high" | "medium" | "low";
  referenceVideo: ResearchVideo | null;
}

export interface ResearchResult {
  researchedAt: string | null;
  games: ResearchGame[];
  reviewQueue: { title: string; reason: string }[];
  warnings: string[];
}

const STEAM_APP = /store\.steampowered\.com\/app\/(\d+)/i;
const ITCH_GAME = /^(?:https?:\/\/)?([a-z0-9][a-z0-9_-]*)\.itch\.io\/([a-z0-9][a-z0-9_-]*)\/?(?:[?#].*)?$/i;
const YOUTUBE_ID = /(?:youtu\.be\/|[?&]v=|\/shorts\/|\/embed\/|\/live\/)([A-Za-z0-9_-]{11})/;

export function steamAppIdOf(url: unknown): number | null {
  const m = typeof url === "string" ? STEAM_APP.exec(url) : null;
  return m ? Number(m[1]) : null;
}

/** 백엔드 ItchUrls.normalize와 같은 모양: https://작성자.itch.io/게임 (소문자) */
export function normalizeItchUrl(url: unknown): string | null {
  const m = typeof url === "string" ? ITCH_GAME.exec(url.trim()) : null;
  return m ? `https://${m[1].toLowerCase()}.itch.io/${m[2].toLowerCase()}` : null;
}

export function youtubeIdOf(url: string): string | null {
  if (/^[A-Za-z0-9_-]{11}$/.test(url)) return url;
  return YOUTUBE_ID.exec(url)?.[1] ?? null;
}

const text = (value: unknown): string | null =>
  typeof value === "string" && value.trim() ? value.trim() : null;

/**
 * @throws Error JSON이 아니거나 games 배열이 없으면
 */
export function parseResearch(raw: string): ResearchResult {
  const start = raw.indexOf("{");
  const end = raw.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("JSON을 찾지 못했어요. Perplexity 결과 전체를 붙여 넣어 주세요.");
  let data: unknown;
  try {
    data = JSON.parse(raw.slice(start, end + 1));
  } catch {
    throw new Error("JSON 형식이 깨져 있어요. 결과를 다시 복사해 주세요.");
  }
  if (typeof data !== "object" || data === null || !Array.isArray((data as { games?: unknown }).games)) {
    throw new Error("games 배열이 없어요. 프롬프트의 출력 형식대로 받은 결과인지 확인해 주세요.");
  }
  const root = data as Record<string, unknown>;
  const games: ResearchGame[] = [];
  for (const item of root.games as unknown[]) {
    if (typeof item !== "object" || item === null) continue;
    const g = item as Record<string, unknown>;
    const title = text(g.title);
    if (!title) continue;
    const video = typeof g.reference_video === "object" && g.reference_video !== null
      ? (g.reference_video as Record<string, unknown>)
      : null;
    const videoUrl = video ? text(video.url) : null;
    games.push({
      title,
      steamAppId: steamAppIdOf(g.steam_url),
      itchUrl: normalizeItchUrl(g.itch_url),
      why: text(g.why),
      confidence: g.confidence === "high" || g.confidence === "medium" ? g.confidence : "low",
      referenceVideo: video && videoUrl
        ? {
            url: videoUrl,
            title: text(video.title),
            channel: text(video.channel),
            originalChannel: video.original_channel === "verified" ? "verified" : "unverified",
          }
        : null,
    });
  }
  const reviewQueue = Array.isArray(root.review_queue)
    ? (root.review_queue as unknown[]).flatMap((item) => {
        if (typeof item !== "object" || item === null) return [];
        const r = item as Record<string, unknown>;
        const title = text(r.title);
        return title ? [{ title, reason: text(r.reason) ?? "" }] : [];
      })
    : [];
  const warnings = Array.isArray(root.warnings)
    ? (root.warnings as unknown[]).flatMap((w) => (text(w) ? [String(w).trim()] : []))
    : [];
  return { researchedAt: text(root.researched_at), games, reviewQueue, warnings };
}
