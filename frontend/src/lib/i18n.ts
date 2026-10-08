/**
 * 사이트 언어. 한국어는 접두사 없는 주소(/games), 영어는 /en 아래(/en/games)다.
 * 실제 라우트는 app/[lang] 이고, 접두사 없는 주소는 proxy가 /ko 로 rewrite 한다.
 */
export const LOCALES = ["ko", "en"] as const;
export type Locale = (typeof LOCALES)[number];
export const DEFAULT_LOCALE: Locale = "ko";

/** 언어 전환기에서 고른 언어. proxy가 브라우저 언어보다 우선한다 */
export const LOCALE_COOKIE = "NEXT_LOCALE";

export function isLocale(value: string): value is Locale {
  return (LOCALES as readonly string[]).includes(value);
}

/** "/games" → 한국어 "/games", 영어 "/en/games" */
export function localePath(locale: Locale, path: string): string {
  if (locale === DEFAULT_LOCALE) return path;
  return path === "/" ? `/${locale}` : `/${locale}${path}`;
}

/** 주소에서 언어 접두사를 뗀다: "/en/games" → "/games", "/ko" → "/" */
export function stripLocale(pathname: string): string {
  for (const locale of LOCALES) {
    if (pathname === `/${locale}`) return "/";
    if (pathname.startsWith(`/${locale}/`)) return pathname.slice(locale.length + 1);
  }
  return pathname;
}

const ko = {
  htmlLang: "ko",
  site: {
    tagline: "공포게임 보관소",
    description: "새로 나오는 공포게임, 출시 예정작, 오래된 명작까지. 플레이 영상과 업적 클리어 방법도 찾아볼 수 있어요.",
    curatorName: "류재일",
    pick: "재일 추천",
    picks: "류재일 추천",
    runBy: "운영",
    steamCredit: "게임 정보와 이미지는 Steam에서 제공됩니다.",
    home: "홈",
  },
  nav: { games: "게임", coop: "협동", about: "소개", youtube: "YouTube" },
  language: { label: "언어", ko: "한국어", en: "English" },
  search: {
    label: "게임 검색",
    placeholder: "게임 검색",
    resultsFor: (q: string) => `'${q}' 검색 결과`,
    noResults: "검색 결과가 없습니다.",
  },
  home: {
    titleLead: "공포게임",
    titleAccent: "보관소",
    intro1: "새로 나오는 공포게임, 출시 예정작, 오래된 명작까지.",
    intro2: "플레이 영상과 업적 클리어 방법도 찾아볼 수 있어요.",
    statsLabel: "보관소 현황",
    /** [숫자 앞, 숫자 뒤] */
    statThisWeek: ["이번 주 출시 ", "개"] as [string, string],
    statUpcoming: ["출시 예정 ", "개"] as [string, string],
    statTotal: ["보관 중 ", "개"] as [string, string],
    quickFilters: "빠른 필터",
    coopOnly: "협동만 보기",
    popular: "현재 인기 있는 공포게임",
    recent: "최근 출시",
    upcoming: "출시 예정",
    videos: (name: string) => `${name}의 최근 영상`,
    more: "더 보기",
    channel: "채널 가기",
  },
  showcase: {
    rank: (n: number) => `인기 ${n}위`,
    choose: "인기 게임 선택",
  },
  card: {
    upcoming: "출시 예정",
    reviews: "리뷰",
    steamReviews: "Steam 리뷰",
    playVideo: "플레이 영상",
  },
  guide: {
    playVideos: "플레이 영상",
    playVideoN: (n: number) => `플레이 영상 ${n}`,
    achievements: "업적 공략",
    achievementCount: (n: number) => `업적 ${n}개`,
    hasGuideVideo: "공략 영상",
    play: (title: string) => `${title} 재생`,
    chooseVideo: "영상 선택",
  },
  badge: { coop: "협동", sponsored: "협찬" },
  games: {
    metaTitle: "공포게임 둘러보기",
    metaDescription: "출시 예정작부터 인기작까지, Steam 공포게임 전체 목록",
    heading: "공포게임",
    viewNav: "보기",
    views: { popular: "인기", recent: "최근 출시", upcoming: "출시 예정", latest: "새로 추가", picked: "류재일 추천" },
    empty: "아직 게임이 없습니다.",
  },
  coop: {
    title: "협동 공포게임",
    description: "친구와 함께 무서워할 수 있는 협동 공포게임",
    intro: "혼자는 무섭다면, 친구와 함께. 온라인·로컬 협동을 지원하는 게임만 모았습니다.",
    empty: "협동 게임이 아직 없습니다.",
  },
  genre: { nav: "장르", empty: "이 장르의 게임이 아직 없습니다." },
  detail: {
    pickLabel: "류재일 추천",
    sponsorNotice: "협찬 고지",
    about: "게임 소개",
    aboutSteam: "게임 소개 (Steam)",
    media: "영상과 스크린샷",
    info: "게임 정보",
    releaseDate: "출시일",
    steamReviews: "Steam 리뷰",
    reviewCount: (count: string) => `${count}개`,
    viewOnSteam: "Steam에서 보기 ↗",
    highlightsLabel: "장점 포인트",
    highlightsTitle: "이 게임을 해야 하는 이유",
    articleKoreanOnly: null as string | null,
  },
  developers: { developer: "개발", publisher: "배급", both: "개발·배급" },
  media: {
    screenshot: (title: string, n: number) => `${title} 스크린샷 ${n}`,
    showScreenshot: (n: number) => `스크린샷 ${n} 보기`,
    trailer: (title: string) => `${title} 트레일러`,
    playTrailer: (title: string) => `${title} 트레일러 재생`,
    trailerN: (n: number) => `트레일러 ${n}`,
    unsupported: "이 브라우저에서는 트레일러를 재생할 수 없습니다.",
    failed: "트레일러를 불러오지 못했습니다.",
  },
  pagination: { label: "페이지", prev: "이전", next: "다음" },
  error: { title: "일시적인 오류가 발생했습니다", body: "잠시 후 다시 시도해 주세요.", retry: "다시 시도" },
  notFound: {
    title: "페이지를 찾을 수 없습니다",
    body: "주소가 바뀌었거나, 아직 소개되지 않은 게임일 수 있습니다.",
    browse: "전체 게임 보기",
  },
  format: { comingSoon: "출시 예정" },
};

export type Dictionary = typeof ko;

const en: Dictionary = {
  htmlLang: "en",
  site: {
    tagline: "The Horror Game Archive",
    description: "New releases, upcoming games and timeless classics. Find gameplay videos and achievement guides, too.",
    curatorName: "Jaeil",
    pick: "Jaeil's Pick",
    picks: "Jaeil's Picks",
    runBy: "Run by",
    steamCredit: "Game data and images provided by Steam.",
    home: "Home",
  },
  nav: { games: "Games", coop: "Co-op", about: "About", youtube: "YouTube" },
  language: { label: "Language", ko: "한국어", en: "English" },
  search: {
    label: "Search games",
    placeholder: "Search games",
    resultsFor: (q: string) => `Results for '${q}'`,
    noResults: "No games found.",
  },
  home: {
    titleLead: "The Horror Game",
    titleAccent: "Archive",
    intro1: "New releases, upcoming games and timeless classics.",
    intro2: "Find gameplay videos and achievement guides, too.",
    statsLabel: "Archive stats",
    statThisWeek: ["", " released this week"],
    statUpcoming: ["", " coming soon"],
    statTotal: ["", " in the archive"],
    quickFilters: "Quick filters",
    coopOnly: "Co-op only",
    popular: "Popular right now",
    recent: "New releases",
    upcoming: "Coming soon",
    videos: (name: string) => `Latest videos from ${name}`,
    more: "More",
    channel: "Channel",
  },
  showcase: {
    rank: (n: number) => `#${n} popular`,
    choose: "Choose a popular game",
  },
  card: {
    upcoming: "Coming soon",
    reviews: "Reviews",
    steamReviews: "Steam reviews",
    playVideo: "Gameplay",
  },
  guide: {
    playVideos: "Gameplay videos",
    playVideoN: (n: number) => `Gameplay video ${n}`,
    achievements: "Achievement guides",
    achievementCount: (n: number) => `${n} achievements`,
    hasGuideVideo: "Guide video",
    play: (title: string) => `Play ${title}`,
    chooseVideo: "Choose a video",
  },
  badge: { coop: "Co-op", sponsored: "Sponsored" },
  games: {
    metaTitle: "Browse horror games",
    metaDescription: "Every horror game on Steam, from upcoming releases to popular hits",
    heading: "Horror games",
    viewNav: "View",
    views: { popular: "Popular", recent: "New releases", upcoming: "Coming soon", latest: "Recently added", picked: "Jaeil's Picks" },
    empty: "No games yet.",
  },
  coop: {
    title: "Co-op horror games",
    description: "Horror games to be scared of together with friends",
    intro: "Too scary alone? Bring friends. Only games with online or local co-op.",
    empty: "No co-op games yet.",
  },
  genre: { nav: "Genres", empty: "No games in this genre yet." },
  detail: {
    pickLabel: "Jaeil's Pick",
    sponsorNotice: "Sponsorship notice",
    about: "About this game",
    aboutSteam: "About this game (Steam)",
    media: "Videos and screenshots",
    info: "Game info",
    releaseDate: "Release date",
    steamReviews: "Steam reviews",
    reviewCount: (count: string) => count,
    viewOnSteam: "View on Steam ↗",
    highlightsLabel: "Highlights",
    highlightsTitle: "Why you should play it",
    articleKoreanOnly: "This recommendation is written in Korean.",
  },
  developers: { developer: "Developer", publisher: "Publisher", both: "Developer · Publisher" },
  media: {
    screenshot: (title: string, n: number) => `${title} screenshot ${n}`,
    showScreenshot: (n: number) => `Show screenshot ${n}`,
    trailer: (title: string) => `${title} trailer`,
    playTrailer: (title: string) => `Play ${title} trailer`,
    trailerN: (n: number) => `Trailer ${n}`,
    unsupported: "This browser can't play the trailer.",
    failed: "Couldn't load the trailer.",
  },
  pagination: { label: "Pages", prev: "Previous", next: "Next" },
  error: { title: "Something went wrong", body: "Please try again in a moment.", retry: "Try again" },
  notFound: {
    title: "Page not found",
    body: "The address may have changed, or the game isn't listed yet.",
    browse: "Browse all games",
  },
  format: { comingSoon: "Coming soon" },
};

const DICTIONARIES: Record<Locale, Dictionary> = { ko, en };

export function getDictionary(locale: Locale): Dictionary {
  return DICTIONARIES[locale];
}
