import "server-only";

import { CURATOR } from "./site";
import { CACHE_TAGS, REVALIDATE_SECONDS } from "./api";

export interface YoutubeVideo {
  id: string;
  title: string;
  url: string;
  thumbnailUrl: string;
  /** ISO-8601 */
  publishedAt: string;
  shorts: boolean;
}

const FEED_URL = `https://www.youtube.com/feeds/videos.xml?channel_id=${CURATOR.youtubeChannelId}`;

/**
 * 채널 RSS 피드(API 키 불필요, 최근 15개)에서 최신 영상을 가져온다.
 * 실패해도 페이지는 그려야 하므로 빈 배열을 돌려준다. 1시간 캐시.
 */
export async function getLatestVideos(limit: number): Promise<YoutubeVideo[]> {
  try {
    const res = await fetch(FEED_URL, { next: { revalidate: REVALIDATE_SECONDS, tags: [CACHE_TAGS.videos] } });
    if (!res.ok) return [];
    return parseFeed(await res.text()).slice(0, limit);
  } catch {
    return [];
  }
}

/** RSS 구조가 단순해서 XML 파서 없이 필요한 태그만 뽑는다. */
export function parseFeed(xml: string): YoutubeVideo[] {
  const videos: YoutubeVideo[] = [];
  for (const [, entry] of xml.matchAll(/<entry>([\s\S]*?)<\/entry>/g)) {
    const id = /<yt:videoId>([^<]+)<\/yt:videoId>/.exec(entry)?.[1];
    const title = /<title>([^<]*)<\/title>/.exec(entry)?.[1];
    const publishedAt = /<published>([^<]+)<\/published>/.exec(entry)?.[1];
    if (!id || !title || !publishedAt) continue;
    const url = /<link rel="alternate" href="([^"]+)"/.exec(entry)?.[1] ?? `https://www.youtube.com/watch?v=${id}`;
    videos.push({
      id,
      title: decodeEntities(title),
      url,
      thumbnailUrl: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
      publishedAt,
      shorts: url.includes("/shorts/"),
    });
  }
  return videos;
}

function decodeEntities(text: string): string {
  return text
    .replaceAll("&amp;", "&")
    .replaceAll("&lt;", "<")
    .replaceAll("&gt;", ">")
    .replaceAll("&quot;", '"')
    .replaceAll("&#39;", "'");
}
