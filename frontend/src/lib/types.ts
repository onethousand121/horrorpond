// 백엔드 응답 DTO와 1:1로 맞춘 타입.
// 출처: backend curation/application/{GameSummaryResponse, GameDetailResponse},
//       catalog/api/GenreController.GenreResponse, common/web/PageResponse, common/error/ErrorResponse
// 날짜: LocalDate는 "yyyy-MM-dd" 문자열, Instant는 ISO-8601(UTC) 문자열로 직렬화된다.

export type DeveloperRole = "DEVELOPER" | "PUBLISHER";
export type MediaType = "SCREENSHOT" | "TRAILER";
export type Store = "STEAM" | "ITCH" | "HUMBLE" | "FANATICAL" | "GMG";
/** 백엔드 CuratedGameSort */
export type GameSort = "LATEST" | "RELEASE";

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
  comingSoon: boolean;
  coop: boolean;
  genres: GenreSummary[];
  oneLiner: string;
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
  genres: GenreSummary[];
  developers: DeveloperCredit[];
  /** sortOrder 순 */
  media: Media[];
  storeLinks: StoreLink[];
  article: Article;
}
