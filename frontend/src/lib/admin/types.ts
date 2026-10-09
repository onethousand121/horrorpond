// 관리자 API 응답 DTO와 1:1로 맞춘 타입.
// 출처: backend curation/application/{AdminGameResponse, AdminGameDetailResponse, AdminArticleResponse}
import type { AchievementGuide, ArticleKind, PlayVideo } from "@/lib/types";

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
  /** 백엔드 배포 전 응답에는 없다 (그때는 추천) */
  kind?: ArticleKind;
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
  /** itch.io 게임 주소 (ITCH 게임만). 백엔드 배포 전 응답에는 없다 */
  itchUrl?: string | null;
  article: AdminArticle | null;
  playVideos: PlayVideo[];
  achievements: AchievementGuide[];
}

/** 백엔드 PlayVideo / AchievementGuide 제약 (서버가 최종 검증한다) */
export const GUIDE_LIMITS = {
  playVideos: 10,
  videoTitle: 200,
  achievements: 100,
  achievementName: 200,
  achievementDescription: 500,
} as const;

/** 서버 액션 결과. 폼 아래에 메시지로 보여준다. */
export interface ActionResult {
  ok: boolean;
  message: string;
}
