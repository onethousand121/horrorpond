// 백엔드 응답 DTO와 1:1로 맞춘 타입.
// 출처: backend curation/application/{GameSummaryResponse, GameDetailResponse},
//       catalog/api/GenreController.GenreResponse, common/web/PageResponse, common/error/ErrorResponse
// 날짜: LocalDate는 "yyyy-MM-dd" 문자열, Instant는 ISO-8601(UTC) 문자열로 직렬화된다.

export type DeveloperRole = "DEVELOPER" | "PUBLISHER";
export type MediaType = "SCREENSHOT" | "TRAILER";
export type Store = "STEAM" | "ITCH" | "HUMBLE" | "FANATICAL" | "GMG";
/** 백엔드 CuratedGameSort: 최근 추가 / 최신 출시 / 인기(누적 Steam 리뷰 수) / 지금 뜨는(최근 출시작의 하루 평균 리뷰 수) */
export type GameSort = "LATEST" | "RELEASE" | "POPULAR" | "TRENDING";
/** 백엔드 ReleaseWindow: 출시 예정 / 최근 90일 출시 / 오늘 포함 최근 7일 출시 */
export type ReleaseWindow = "UPCOMING" | "RECENT" | "TODAY" | "TOMORROW";

/** 백엔드 HubStatsResponse: 홈 상단 숫자 (모두 사이트에 보이는 게임 기준) */
export interface HubStats {
  releasedToday: number;
  releasingTomorrow: number;
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
  /** Steam 짧은 소개 (한국어 소개가 없으면 자동 번역) */
  shortDescription: string | null;
  /** 소개가 자동 번역이면 true. 백엔드 배포 전 응답에는 없다 */
  shortDescriptionTranslated?: boolean;
  /** Steam 지원 언어 코드 (ko, en, ja …). 백엔드 배포 전 응답에는 없다 */
  languages?: string[];
  /** 음성까지 지원하는 언어 코드 */
  audioLanguages?: string[];
  comingSoon: boolean;
  coop: boolean;
  /** 성인 콘텐츠 (고정 노출한 경우만 목록에 나온다). 이미지를 흐리게, 소개는 숨긴다 */
  adult: boolean;
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
  /** 플레이 영상이 하나 이상 있다 */
  hasPlayVideo: boolean;
  /** 업적 공략이 하나 이상 있다 (업적 공략 페이지가 있다) */
  hasAchievementGuide: boolean;
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
  shortDescriptionTranslated?: boolean;
  headerImageUrl: string | null;
  languages?: string[];
  audioLanguages?: string[];
  /** yyyy-MM-dd */
  releaseDate: string | null;
  releaseDateText: string | null;
  comingSoon: boolean;
  coop: boolean;
  /** 성인 콘텐츠: 상세 페이지는 소개·미디어 없이 Steam 링크만 보여준다 */
  adult: boolean;
  reviewCount: number | null;
  genres: GenreSummary[];
  developers: DeveloperCredit[];
  /** sortOrder 순 */
  media: Media[];
  storeLinks: StoreLink[];
  /** 재일 추천 글. 없으면 null */
  article: Article | null;
  /** 플레이 영상. 없으면 빈 배열 (사이트에서 영역을 숨긴다) */
  playVideos: PlayVideo[];
  /** 업적 공략. 없으면 빈 배열 (사이트에서 영역을 숨긴다) */
  achievements: AchievementGuide[];
}

/** 백엔드 GameGuideResponses.PlayVideoResponse */
export interface PlayVideo {
  youtubeId: string;
  title: string | null;
}

/** 백엔드 GameGuideResponses.AchievementGuideResponse */
export interface AchievementGuide {
  name: string;
  description: string | null;
  /** 공략 영상. 없으면 null */
  youtubeId: string | null;
}
