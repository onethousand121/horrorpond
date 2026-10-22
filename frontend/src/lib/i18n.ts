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
export function alternatesFor(locale: Locale, path: string, locales: readonly Locale[] = LOCALES) {
  return {
    canonical: localePath(locale, path),
    languages: alternateLanguages(path, locales),
  };
}

/** hreflang 주소 (언어 접두사 없는 상대 주소). x-default는 한국어. 한 언어에만 있는 페이지는 그 언어만 */
export function alternateLanguages(path: string, locales: readonly Locale[] = LOCALES): Record<string, string> {
  const languages: Record<string, string> = {};
  for (const locale of locales) languages[locale] = localePath(locale, path);
  languages["x-default"] = localePath(locales.includes(DEFAULT_LOCALE) ? DEFAULT_LOCALE : locales[0], path);
  return languages;
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
    steamCredit: "게임 정보와 이미지는 Steam과 itch.io에서 제공됩니다.",
    disclaimer:
      "lurkpond는 Valve Corporation 및 Steam과 관련이 없는 개인 운영 사이트입니다. 게임 이름, 이미지, 상표의 권리는 각 개발사·배급사에 있습니다.",
    home: "홈",
  },
  nav: { games: "게임", coop: "협동", about: "소개", youtube: "YouTube", steamCurator: "Steam 큐레이터", privacy: "개인정보처리방침" },
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
    freeOnly: "무료 게임",
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
    ratings: "평가",
    steamReviews: "Steam 리뷰",
    itchRatings: "itch.io 평가",
    playVideo: "플레이 영상",
    pickShort: "추천",
    reviewShort: "후기",
    videoShort: "영상",
    achievementShort: "업적",
    hasPick: "주인장 추천 글 있음",
    hasReview: "주인장 플레이 후기 있음",
    sponsoredLabel: "협찬을 받은 추천",
    hasPlayVideo: "류재일 플레이 영상 있음",
    hasAchievementGuide: "업적 공략 있음",
  },
  guide: {
    playVideos: "플레이 영상",
    referenceVideos: "참고 영상",
    referenceNote: "주인장이 아직 플레이하지 않은 게임이라 다른 채널의 영상을 소개해요.",
    channel: (name: string) => `채널: ${name}`,
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
    endings: "엔딩",
    endingSpoiler: "엔딩 정보 · 스포일러 주의 · 눌러서 보기",
    singleEnding: "엔딩이 하나뿐인 게임이에요.",
    endingVideo: (name: string) => `${name} 엔딩 영상`,
  },
  badge: {
    coop: "협동",
    sponsored: "협찬",
    adult: "19",
    adultLabel: "성인 게임",
    korean: "한국어",
    free: "무료",
    koreanPatch: "한글패치",
    koreanPatchLabel: "유저 한글패치 있음 (비공식)",
  },
  adult: {
    title: "성인 게임",
    notice: "성인 콘텐츠가 포함된 게임이라 이 사이트에서는 이미지와 소개를 보여주지 않습니다. 자세한 내용은 Steam에서 연령 확인 후 볼 수 있습니다.",
  },
  games: {
    metaTitle: "공포게임 둘러보기",
    metaDescription: "출시 예정작부터 인기작까지, Steam·itch.io 공포게임 전체 목록",
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
  collections: {
    nav: "공포게임 모음",
    others: "다른 공포게임 모음",
    seeAll: "전체 보기",
    empty: "지금은 이 모음에 들어갈 게임이 없습니다.",
    items: {
      trending: {
        title: "지금 뜨는 공포게임",
        description: "최근 일주일 동안 Steam 리뷰가 가장 빠르게 늘어난 공포게임 순위. 요즘 많이 하는 공포게임을 한눈에 볼 수 있어요.",
        intro: [
          "요즘 사람들이 가장 많이 하고 있는 공포게임을 모았습니다.",
          "최근 7일 동안 Steam 리뷰가 늘어난 수를 기준으로 순위를 매기고, 주인장이 매주 커뮤니티와 영상 반응을 살펴 눈여겨볼 게임을 앞에 올려 둡니다.",
          "데이터는 매일 새로 받아서, 순위도 날마다 바뀝니다.",
        ],
      },
      upcoming: {
        title: "출시 예정 공포게임",
        description: "곧 나올 Steam 공포게임과 출시일을 한눈에. 찜해 둘 신작을 찾아보세요.",
        intro: [
          "곧 출시되는 공포게임을 모았습니다.",
          "Steam에 출시 예정으로 올라온 공포게임을 매일 확인하고, 출시일이 다가오면 더 자주 다시 확인해 연기나 출시 소식을 반영합니다.",
          "찜해 둘 신작을 찾고 있다면 여기서부터 보세요.",
        ],
      },
      free: {
        title: "무료 공포게임",
        description: "돈 들이지 않고 바로 해 볼 수 있는 무료 공포게임 모음. Steam과 itch.io의 인기 무료 공포게임을 리뷰 많은 순으로 소개합니다.",
        intro: [
          "지금 무료로 받을 수 있는 공포게임을 모았습니다.",
          "Steam과 itch.io에서 무료로 내려받을 수 있는 공포게임을 리뷰·평가가 많은 순으로 보여줍니다.",
          "가격은 매일 수집한 정가 기준이라, 잠깐 무료로 풀린 할인 이벤트는 포함되지 않습니다.",
        ],
      },
      korean: {
        title: "한국어 지원 공포게임",
        description: "한국어로 즐길 수 있는 공포게임 모음. 공식 한국어를 지원하거나 유저 한글패치가 있는 Steam·itch.io 공포게임을 소개합니다.",
        intro: [
          "한국어로 플레이할 수 있는 공포게임을 모았습니다.",
          "Steam 상점의 지원 언어에 한국어가 있는 게임과, 공식 한국어는 없지만 유저 한글패치를 주인장이 확인해 둔 게임을 함께 담았어요.",
          "리뷰가 많은 순으로 보여주고, 지원 언어 정보는 매일 Steam에서 새로 받아 반영합니다.",
        ],
      },
      picks: {
        title: "주인장 추천 공포게임",
        description: "공포게임 유튜버 류재일이 직접 플레이하고 고른 공포게임. 추천 글, 플레이 후기, 플레이 영상이 있는 게임만 모았습니다.",
        intro: [
          "주인장이 직접 플레이해 본 공포게임만 모았습니다.",
          "추천 글이나 플레이 후기를 남겼거나 유튜브에 플레이 영상을 올린 게임이 들어가고, 새 영상과 글이 올라올 때마다 늘어납니다.",
          "무엇을 할지 고민된다면 주인장의 선택부터 믿어 보세요.",
        ],
      },
    },
  },
  genre: { nav: "장르", empty: "이 장르의 게임이 아직 없습니다." },
  detail: {
    pickLabel: "주인장 추천",
    reviewLabel: "주인장 플레이 후기",
    sponsorNotice: "협찬 고지",
    about: "게임 소개",
    aboutSteam: "게임 소개 (Steam)",
    aboutItch: "게임 소개 (itch.io)",
    autoTranslated: "자동 번역",
    media: "영상과 스크린샷",
    info: "게임 정보",
    releaseDate: "출시일",
    price: "가격",
    priceFree: "무료",
    priceNote: "정가 기준 · 할인은 상점에서 확인",
    priceNoteKrw: "정가 기준 · 할인은 상점에서 확인",
    languages: "지원 언어",
    moreLanguages: (n: number) => `외 ${n}개`,
    steamReviews: "Steam 리뷰",
    itchRatings: "itch.io 평가",
    reviewCount: (count: string) => `${count}개`,
    viewOnSteam: "Steam에서 보기 ↗",
    viewOnItch: "itch.io에서 보기 ↗",
    highlightsLabel: "장점 포인트",
    highlightsTitle: "이 게임을 해야 하는 이유",
    articleKoreanOnly: null as string | null,
    koreanPatch: "한글패치",
    koreanPatchAuthor: "유저 한글패치",
    koreanPatchNote: "비공식 패치예요. 제작자 배포처에서 받으세요.",
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
  format: { comingSoon: "출시 예정", releasingToday: "오늘 출시 예정" },
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
    steamCredit: "Game data and images provided by Steam and itch.io.",
    disclaimer:
      "lurkpond is an independent site not affiliated with Valve Corporation or Steam. Game names, images and trademarks belong to their respective developers and publishers.",
    home: "Home",
  },
  nav: { games: "Games", coop: "Co-op", about: "About", youtube: "YouTube", steamCurator: "Steam Curator", privacy: "Privacy" },
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
    freeOnly: "Free games",
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
    ratings: "Ratings",
    steamReviews: "Steam reviews",
    itchRatings: "itch.io ratings",
    playVideo: "Gameplay",
    pickShort: "Pick",
    reviewShort: "Review",
    videoShort: "Video",
    achievementShort: "Achievements",
    hasPick: "Has a keeper's pick",
    hasReview: "Has the keeper's play notes",
    sponsoredLabel: "Sponsored pick",
    hasPlayVideo: "Has a gameplay video",
    hasAchievementGuide: "Has an achievement guide",
  },
  guide: {
    playVideos: "Gameplay videos",
    referenceVideos: "Gameplay from other channels",
    referenceNote: "The keeper hasn't played this one yet, so here's a video from another channel.",
    channel: (name: string) => `Channel: ${name}`,
    playVideoN: (n: number) => `Gameplay video ${n}`,
    achievements: "Achievement guides",
    achievementCount: (n: number) => `${n} ${n === 1 ? "achievement" : "achievements"}`,
    seeAllAchievements: (n: number) => `See all ${n} achievement ${n === 1 ? "guide" : "guides"} →`,
    backToGame: "← Back to game",
    pageTitle: (title: string, n: number) => `${title} Achievement Guide (${n} ${n === 1 ? "achievement" : "achievements"})`,
    pageDescription: (title: string, names: string) => `How to unlock ${title} achievements, with guide videos: ${names}`,
    hasGuideVideo: "Guide video",
    play: (title: string) => `Play ${title}`,
    chooseVideo: "Choose a video",
    endings: "Endings",
    endingSpoiler: "Ending info · Spoilers · click to reveal",
    singleEnding: "This game has a single ending.",
    endingVideo: (name: string) => `${name} ending video`,
  },
  badge: {
    coop: "Co-op",
    sponsored: "Sponsored",
    adult: "18+",
    adultLabel: "Adult game",
    korean: "Korean",
    free: "Free",
    koreanPatch: "Korean patch",
    koreanPatchLabel: "Has a fan-made Korean patch (unofficial)",
  },
  adult: {
    title: "Adult game",
    notice: "This game contains adult content, so images and descriptions are not shown here. See the details on Steam after its age check.",
  },
  games: {
    metaTitle: "Browse horror games",
    metaDescription: "Every horror game on Steam and itch.io, from upcoming releases to popular hits",
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
  collections: {
    nav: "Horror game collections",
    others: "More horror game collections",
    seeAll: "See all",
    empty: "No games in this collection right now.",
    items: {
      trending: {
        title: "Trending horror games right now",
        description: "The horror games gaining Steam reviews fastest this week. See what everyone is playing right now.",
        intro: [
          "These are the horror games people are playing the most right now.",
          "They're ranked by how many Steam reviews they gained over the last seven days, and every week the keeper checks community and video buzz to move notable games to the front.",
          "The data is refreshed daily, so the ranking changes every day.",
        ],
      },
      upcoming: {
        title: "Upcoming horror games",
        description: "Upcoming horror games on Steam, with release dates. Find the next scare to wishlist.",
        intro: [
          "Horror games that are about to come out.",
          "We check every horror game listed as coming soon on Steam each day, and check more often as the release date gets close, so delays and launches show up quickly.",
          "Looking for something to wishlist? Start here.",
        ],
      },
      free: {
        title: "Free horror games",
        description: "Free horror games you can play right now. Popular free-to-play horror on Steam and itch.io, sorted by reviews.",
        intro: [
          "Horror games you can download for free right now.",
          "This list shows free horror games on Steam and itch.io, with the most reviewed and rated ones first.",
          "Prices come from the regular list price we collect daily, so temporary giveaways and sales aren't included.",
        ],
      },
      korean: {
        title: "Horror games with Korean support",
        description: "Horror games you can play in Korean.",
        intro: ["Horror games you can play in Korean."],
      },
      picks: {
        title: "Keeper's picks: hand-picked horror games",
        description: "Horror games played and picked by horror YouTuber Jaeil, with recommendations, play notes or gameplay videos.",
        intro: [
          "Only horror games the keeper has actually played.",
          "A game makes the list when the keeper writes a recommendation or play notes for it, or uploads a gameplay video to YouTube, so it grows with every new video and post.",
          "Can't decide what to play next? Trust the keeper's taste.",
        ],
      },
    },
  },
  genre: { nav: "Genres", empty: "No games in this genre yet." },
  detail: {
    pickLabel: "Keeper's pick",
    reviewLabel: "Keeper's play notes",
    sponsorNotice: "Sponsorship notice",
    about: "About this game",
    aboutSteam: "About this game (Steam)",
    aboutItch: "About this game (itch.io)",
    autoTranslated: "Machine translated",
    media: "Videos and screenshots",
    info: "Game info",
    releaseDate: "Release date",
    price: "Price",
    priceFree: "Free",
    priceNote: "List price · check the store for discounts",
    priceNoteKrw: "Korean Steam list price · check the store for your region",
    languages: "Languages",
    moreLanguages: (n: number) => `+${n} more`,
    steamReviews: "Steam reviews",
    itchRatings: "itch.io ratings",
    reviewCount: (count: string) => count,
    viewOnSteam: "View on Steam ↗",
    viewOnItch: "View on itch.io ↗",
    highlightsLabel: "Highlights",
    highlightsTitle: "Why you should play it",
    articleKoreanOnly: "This post is written in Korean.",
    // 한글패치는 한국어 화면에만 보인다
    koreanPatch: "Korean patch",
    koreanPatchAuthor: "Fan Korean patch",
    koreanPatchNote: "Unofficial patch. Download it from the maker's page.",
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
  format: { comingSoon: "Coming soon", releasingToday: "Releasing today" },
};

const DICTIONARIES: Record<Locale, Dictionary> = { ko, en };

export function getDictionary(locale: Locale): Dictionary {
  return DICTIONARIES[locale];
}
