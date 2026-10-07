/**
 * 검색엔진 노출 여부. SITE_INDEXING=true 일 때만 노출한다(오픈 전 비공개 운영이 기본값).
 * robots.txt와 메타데이터가 빌드 시점에 이 값을 읽으므로, 바꾸면 다시 배포해야 한다.
 */
export const SITE_INDEXING = process.env.SITE_INDEXING === "true";

export const SITE_NAME = "lurkpond";

/** 운영자(유튜버). channelId는 채널 RSS 피드용 (youtube.com/@ryujaeil의 canonical) */
export const CURATOR = {
  name: "류재일",
  youtubeUrl: "https://www.youtube.com/@ryujaeil",
  youtubeChannelId: "UCogL3pDWR26j0R3H0UH4sbg",
} as const;
