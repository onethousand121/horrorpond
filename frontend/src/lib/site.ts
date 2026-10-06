/**
 * 검색엔진 노출 여부. SITE_INDEXING=true 일 때만 노출한다(오픈 전 비공개 운영이 기본값).
 * robots.txt와 메타데이터가 빌드 시점에 이 값을 읽으므로, 바꾸면 다시 배포해야 한다.
 */
export const SITE_INDEXING = process.env.SITE_INDEXING === "true";

export const SITE_NAME = "lurkpond";
export const SITE_TAGLINE = "연못 아래 숨은 공포게임";

/** 큐레이터. 유튜브 주소는 실제 채널 핸들로 확인 필요 */
export const CURATOR = {
  name: "류재일",
  youtubeUrl: "https://www.youtube.com/@ryujaeil",
} as const;
