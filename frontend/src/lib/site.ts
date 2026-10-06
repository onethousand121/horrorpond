/**
 * 검색엔진 노출 여부. SITE_INDEXING=true 일 때만 노출한다(오픈 전 비공개 운영이 기본값).
 * robots.txt와 메타데이터가 빌드 시점에 이 값을 읽으므로, 바꾸면 다시 배포해야 한다.
 */
export const SITE_INDEXING = process.env.SITE_INDEXING === "true";
