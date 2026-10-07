// 백엔드 응답 DTO와 1:1로 맞춘 타입.
// 출처: backend curation/application/{GameSummaryResponse, GameDetailResponse},
//       catalog/api/GenreController.GenreResponse, common/web/PageResponse, common/error/ErrorResponse
// 날짜: LocalDate는 "yyyy-MM-dd" 문자열, Instant는 ISO-8601(UTC) 문자열로 직렬화된다.

export type DeveloperRole = "DEVELOPER" | "PUBLISHER";
export type MediaType = "SCREENSHOT" | "TRAILER";
export type Store = "STEAM" | "ITCH" | "HUMBLE" | "FANATICAL" | "GMG";
/** 백엔드 CuratedGameSort: 최근 추가 / 최신 출시 / 인기(Steam 리뷰 수) */
export type GameSort = "LATEST" | "RELEASE" | "POPULAR";
/** 백엔드 ReleaseWindow: 출시 예정 / 최근 90일 출시 / 오늘 포함 최근 7일 출시 */
export type ReleaseWindow = "UPCOMING" | "RECENT" | "THIS_WEEK";

/** 백엔드 HubStatsResponse: 홈 상단 숫자 (모두 사이트에 보이는 게임 기준) */
export interface HubStats {
  total: number;
  upcoming: number;
  releasedThisWeek: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export interface ErrorResponse {
  code: string;
  message: string;
}

export interface Genre {
  slug: string;
  name: string;
  description: string | null;
}

export interface GenreSummary {
  slug: string;
  name: string;
}

export interface GameSummary {
  slug: string;
  title: string;
  headerImageUrl: string | null;
  /** yyyy-MM-dd */
  releaseDate: string | null;
  /** Steam 원문 (예: "2027년 4분기") */
  releaseDateText: string | null;
  /** Steam 짧은 소개 */
  shortDescription: string | null;
  comingSoon: boolean;
  coop: boolean;
  /** Steam 리뷰 수 */
  reviewCount: number | null;
  /** SteamSpy 상위 태그 (영문, 표 많은 순, 최대 5개) */
  tags: string[];
  genres: GenreSummary[];
  /** 재일 추천(공개된 글이 있음). false면 oneLiner는 null, highlights는 빈 배열 */
  picked: boolean;
  oneLiner: string | null;
  /** 최대 3개 */
  highlights: string[];
  sponsored: boolean;
}

export interface DeveloperCredit {
  name: string;
  slug: string;
  role: DeveloperRole;
}

export interface Media {
  type: MediaType;
  url: string;
  thumbnailUrl: string | null;
}

export interface StoreLink {
  store: Store;
  url: string;
}

export interface Article {
  title: string;
  oneLiner: string;
  /** markdown 원문 */
  body: string;
  highlights: string[];
  sponsored: boolean;
  sponsorDisclosure: string | null;
  /** ISO-8601 Instant */
  publishedAt: string | null;
}

export interface GameDetail {
  slug: string;
  title: string;
  shortDescription: string | null;
  headerImageUrl: string | null;
  /** yyyy-MM-dd */
  releaseDate: string | null;
  releaseDateText: string | null;
  comingSoon: boolean;
  coop: boolean;
  reviewCount: number | null;
  genres: GenreSummary[];
  developers: DeveloperCredit[];
  /** sortOrder 순 */
  media: Media[];
  storeLinks: StoreLink[];
  /** 재일 추천 글. 없으면 null */
  article: Article | null;
}
