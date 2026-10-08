/**
 * 검색엔진 노출 여부. SITE_INDEXING=true 일 때만 노출한다(오픈 전 비공개 운영이 기본값).
 * robots.txt와 메타데이터가 빌드 시점에 이 값을 읽으므로, 바꾸면 다시 배포해야 한다.
 */
export const SITE_INDEXING = process.env.SITE_INDEXING === "true";

export const SITE_NAME = "lurkpond";

/** 사이트 정식 주소 (끝 슬래시 없음). 메타데이터·구조화 데이터·사이트맵의 절대 주소에 쓴다 */
export const SITE_URL = (process.env.SITE_URL ?? "http://localhost:3000").replace(/\/+$/, "");

/** 문의 메일. 도메인의 이메일 라우팅(예: Cloudflare)으로 운영자 메일함에 전달된다 */
export const CONTACT_EMAIL = "contact@lurkpond.com";

/** 개인정보처리방침 시행일 (내용을 바꾸면 갱신) */
export const PRIVACY_EFFECTIVE_DATE = "2026-10-08";

/** 운영자(유튜버). channelId는 채널 RSS 피드용 (youtube.com/@ryujaeil의 canonical) */
export const CURATOR = {
  name: "류재일",
  youtubeUrl: "https://www.youtube.com/@ryujaeil",
  youtubeChannelId: "UCogL3pDWR26j0R3H0UH4sbg",
} as const;
