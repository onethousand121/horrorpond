// 관리자 API 응답 DTO와 1:1로 맞춘 타입.
// 출처: backend curation/application/{AdminGameResponse, AdminGameDetailResponse, AdminArticleResponse}

export type GameSource = "STEAM" | "ITCH" | "MANUAL";
export type GameStatus = "CANDIDATE" | "PUBLISHED" | "HIDDEN";
export type ArticleStatus = "DRAFT" | "PUBLISHED";

/** 백엔드 CurationArticle 제약 (서버가 최종 검증한다) */
export const ARTICLE_LIMITS = {
  title: 200,
  oneLiner: 120,
  highlights: 5,
  highlight: 40,
  disclosure: 300,
} as const;

export interface AdminGame {
  id: number;
  source: GameSource;
  externalId: string | null;
  slug: string;
  title: string;
  headerImageUrl: string | null;
  /** yyyy-MM-dd */
  releaseDate: string | null;
  comingSoon: boolean;
  coop: boolean;
  reviewCount: number | null;
  adult: boolean;
  status: GameStatus;
  /** 지금 공개 사이트에 보이는지 (자동 노출 포함) */
  publiclyVisible: boolean;
  hasArticle: boolean;
  articleStatus: ArticleStatus | null;
  /** 제목이 같은(대소문자 무시) 다른 게임 수 */
  sameTitleCount: number;
}

export interface AdminArticle {
  id: number;
  gameId: number;
  status: ArticleStatus;
  title: string;
  oneLiner: string;
  body: string;
  highlights: string[];
  sponsored: boolean;
  sponsorDisclosure: string | null;
  /** ISO-8601 Instant */
  publishedAt: string | null;
}

export interface AdminGameDetail {
  id: number;
  source: GameSource;
  externalId: string | null;
  slug: string;
  title: string;
  shortDescription: string | null;
  headerImageUrl: string | null;
  releaseDate: string | null;
  comingSoon: boolean;
  coop: boolean;
  reviewCount: number | null;
  adult: boolean;
  tags: string[];
  status: GameStatus;
  publiclyVisible: boolean;
  genreSlugs: string[];
  steamUrl: string | null;
  article: AdminArticle | null;
}

/** 서버 액션 결과. 폼 아래에 메시지로 보여준다. */
export interface ActionResult {
  ok: boolean;
  message: string;
}
