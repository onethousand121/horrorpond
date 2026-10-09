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

/**
 * 페이지 메타데이터의 canonical + hreflang. path는 언어 접두사 없는 주소("/games/x")
 * 검색엔진이 한국어·영어 페이지를 같은 내용의 언어별 버전으로 묶는다.
 */
export function alternatesFor(locale: Locale, path: string) {
  return {
    canonical: localePath(locale, path),
    languages: { ko: path, en: localePath("en", path), "x-default": path },
  };
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
    pick: "주인장 추천",
    picks: "주인장 추천",
    runBy: "운영",
    steamCredit: "게임 정보와 이미지는 Steam에서 제공됩니다.",
    disclaimer:
      "lurkpond는 Valve Corporation 및 Steam과 관련이 없는 개인 운영 사이트입니다. 게임 이름, 이미지, 상표의 권리는 각 개발사·배급사에 있습니다.",
    home: "홈",
  },
  nav: { games: "게임", coop: "협동", about: "소개", youtube: "YouTube", privacy: "개인정보처리방침" },
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
    statsLabel: "출시 현황",
    /** [숫자 앞, 숫자 뒤] */
    statToday: ["오늘 출시 ", "개"] as [string, string],
    statTomorrow: ["내일 출시 예정 ", "개"] as [string, string],
    quickFilters: "빠른 필터",
    coopOnly: "협동만 보기",
    koreanOnly: "한국어 지원",
    keeper: "주인장 추천",
    keeperIntro: "주인장이 직접 플레이한 게임 중에서 골라 왔어요.",
    trending: "지금 뜨는 공포게임",
    steady: "꾸준히 사랑받는 명작",
    recent: "최근 출시",
    upcoming: "출시 예정",
    videos: (name: string) => `${name}의 최근 영상`,
    more: "더 보기",
    channel: "채널 가기",
  },
  showcase: {
    rank: (n: number) => `지금 뜨는 ${n}위`,
    choose: "지금 뜨는 게임 선택",
  },
  card: {
    upcoming: "출시 예정",
    reviews: "리뷰",
    steamReviews: "Steam 리뷰",
    playVideo: "플레이 영상",
    pickShort: "추천",
    videoShort: "영상",
    achievementShort: "업적",
    hasPick: "주인장 추천 글 있음",
    sponsoredLabel: "협찬을 받은 추천",
    hasPlayVideo: "류재일 플레이 영상 있음",
    hasAchievementGuide: "업적 공략 있음",
  },
  guide: {
    playVideos: "플레이 영상",
    playVideoN: (n: number) => `플레이 영상 ${n}`,
    achievements: "업적 공략",
    achievementCount: (n: number) => `업적 ${n}개`,
    seeAllAchievements: (n: number) => `업적 공략 전체 보기 (${n}개) →`,
    backToGame: "← 게임 정보",
    pageTitle: (title: string, n: number) => `${title} 업적 공략 (전체 ${n}개)`,
    pageDescription: (title: string, names: string) => `${title} 업적 달성 방법과 공략 영상: ${names}`,
    hasGuideVideo: "공략 영상",
    play: (title: string) => `${title} 재생`,
    chooseVideo: "영상 선택",
  },
  badge: { coop: "협동", sponsored: "협찬", adult: "19", adultLabel: "성인 게임", korean: "한국어", koreanAudio: "한국어 음성" },
  adult: {
    title: "성인 게임",
    notice: "성인 콘텐츠가 포함된 게임이라 이 사이트에서는 이미지와 소개를 보여주지 않습니다. 자세한 내용은 Steam에서 연령 확인 후 볼 수 있습니다.",
  },
  games: {
    metaTitle: "공포게임 둘러보기",
    metaDescription: "출시 예정작부터 인기작까지, Steam 공포게임 전체 목록",
    heading: "공포게임",
    viewNav: "보기",
    views: { trending: "지금 뜨는", popular: "명작·스테디셀러", recent: "최근 출시", upcoming: "출시 예정", latest: "새로 추가", picked: "주인장 추천" },
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
    pickLabel: "주인장 추천",
    sponsorNotice: "협찬 고지",
    about: "게임 소개",
    aboutSteam: "게임 소개 (Steam)",
    autoTranslated: "자동 번역",
    media: "영상과 스크린샷",
    info: "게임 정보",
    releaseDate: "출시일",
    languages: "지원 언어",
    audio: "음성",
    moreLanguages: (n: number) => `외 ${n}개`,
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
    pick: "Keeper's pick",
    picks: "Keeper's picks",
    runBy: "Run by",
    steamCredit: "Game data and images provided by Steam.",
    disclaimer:
      "lurkpond is an independent site not affiliated with Valve Corporation or Steam. Game names, images and trademarks belong to their respective developers and publishers.",
    home: "Home",
  },
  nav: { games: "Games", coop: "Co-op", about: "About", youtube: "YouTube", privacy: "Privacy" },
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
    statsLabel: "Release stats",
    statToday: ["", " out today"],
    statTomorrow: ["", " out tomorrow"],
    quickFilters: "Quick filters",
    coopOnly: "Co-op only",
    koreanOnly: "Korean supported",
    keeper: "Keeper's picks",
    keeperIntro: "Picked from the games the keeper has played.",
    trending: "Trending now",
    steady: "All-time favorites",
    recent: "New releases",
    upcoming: "Coming soon",
    videos: (name: string) => `Latest videos from ${name}`,
    more: "More",
    channel: "Channel",
  },
  showcase: {
    rank: (n: number) => `#${n} trending`,
    choose: "Choose a trending game",
  },
  card: {
    upcoming: "Coming soon",
    reviews: "Reviews",
    steamReviews: "Steam reviews",
    playVideo: "Gameplay",
    pickShort: "Pick",
    videoShort: "Video",
    achievementShort: "Achievements",
    hasPick: "Has a keeper's pick",
    sponsoredLabel: "Sponsored pick",
    hasPlayVideo: "Has a gameplay video",
    hasAchievementGuide: "Has an achievement guide",
  },
  guide: {
    playVideos: "Gameplay videos",
    playVideoN: (n: number) => `Gameplay video ${n}`,
    achievements: "Achievement guides",
    achievementCount: (n: number) => `${n} achievements`,
    seeAllAchievements: (n: number) => `See all ${n} achievement guides →`,
    backToGame: "← Back to game",
    pageTitle: (title: string, n: number) => `${title} Achievement Guide (${n} achievements)`,
    pageDescription: (title: string, names: string) => `How to unlock ${title} achievements, with guide videos: ${names}`,
    hasGuideVideo: "Guide video",
    play: (title: string) => `Play ${title}`,
    chooseVideo: "Choose a video",
  },
  badge: { coop: "Co-op", sponsored: "Sponsored", adult: "18+", adultLabel: "Adult game", korean: "Korean", koreanAudio: "Korean audio" },
  adult: {
    title: "Adult game",
    notice: "This game contains adult content, so images and descriptions are not shown here. See the details on Steam after its age check.",
  },
  games: {
    metaTitle: "Browse horror games",
    metaDescription: "Every horror game on Steam, from upcoming releases to popular hits",
    heading: "Horror games",
    viewNav: "View",
    views: { trending: "Trending", popular: "All-time favorites", recent: "New releases", upcoming: "Coming soon", latest: "Recently added", picked: "Keeper's picks" },
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
    pickLabel: "Keeper's pick",
    sponsorNotice: "Sponsorship notice",
    about: "About this game",
    aboutSteam: "About this game (Steam)",
    autoTranslated: "Machine translated",
    media: "Videos and screenshots",
    info: "Game info",
    releaseDate: "Release date",
    languages: "Languages",
    audio: "audio",
    moreLanguages: (n: number) => `+${n} more`,
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
