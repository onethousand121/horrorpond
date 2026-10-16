// e2e용 가짜 백엔드. 의존성 없는 Node http 서버로 lib/api.ts, lib/admin/api.ts가 부르는 경로에 fixture JSON을 돌려준다.
// 실행: MOCK_API_PORT=4010 node e2e/mock-api.mjs
// 디버그: GET /__requests?path=/api/games (받은 요청 목록), GET /__last?path=... (마지막 요청), GET /__health
import { createServer } from "node:http";

const PORT = Number(process.env.MOCK_API_PORT ?? 4010);
const ADMIN_KEY = "e2e-admin-key";

const STEAM_IMG = (appid) => `https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/${appid}/header.jpg`;
const genres = [
  { slug: "psychological", name: "심리 공포", nameEn: "Psychological", description: null },
  { slug: "survival", name: "생존 공포", nameEn: "Survival", description: null },
];

/** 오늘·내일 (Asia/Seoul, yyyy-MM-dd). 출시일이 오늘인 fixture가 실행한 날짜를 따라가게 한다 */
const SEOUL_DATE = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit" });
const seoulDate = (offsetDays = 0) => SEOUL_DATE.format(new Date(Date.now() + offsetDays * 86_400_000));

/** 백엔드 ReleaseWindow와 같은 규칙: 출시 예정 표시가 남아 있고 출시일이 없거나 오늘 이후면 출시 전 */
const isUpcoming = (g) => g.comingSoon && (!g.releaseDate || g.releaseDate >= seoulDate());

function steamGame(id, appid, fields) {
  return {
    id,
    source: "STEAM",
    externalId: String(appid),
    headerImageUrl: STEAM_IMG(appid),
    releaseDate: "2026-09-01",
    releaseDateText: null,
    comingSoon: false,
    coop: false,
    adult: false,
    reviewCount: 120,
    tags: ["Horror", "Psychological Horror"],
    genreSlugs: ["psychological"],
    languages: ["en"],
    audioLanguages: ["en"],
    free: false,
    priceAmount: null,
    priceCurrency: null,
    developers: [{ name: "Fog Studio", slug: "fog-studio", role: "DEVELOPER" }],
    media: [
      { type: "SCREENSHOT", url: `https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/${appid}/ss_1.jpg`, thumbnailUrl: null },
    ],
    storeLinks: [{ store: "STEAM", url: `https://store.steampowered.com/app/${appid}` }],
    article: null,
    playVideos: [],
    referenceVideos: [],
    achievements: [],
    endingType: null,
    endings: [],
    koreanPatches: [],
    status: "CANDIDATE",
    ...fields,
  };
}

/** fixture 게임. en은 lang=en일 때 바꿔 줄 값 (없으면 한국어 그대로, 백엔드와 같은 규칙) */
const games = [
  steamGame(1, 1001, {
    slug: "dark-house",
    title: "어두운 집",
    en: { title: "Dark House", shortDescription: "A house that remembers you." },
    shortDescription: "당신을 기억하는 집.",
    languages: ["ko", "en", "ja"],
    priceAmount: 1650000,
    priceCurrency: "KRW",
    reviewCount: 4321,
    endingType: "SINGLE",
  }),
  steamGame(2, 1002, {
    slug: "free-scream",
    title: "Free Scream",
    shortDescription: "무료로 즐기는 짧은 공포.",
    en: { shortDescription: "A short free scare." },
    free: true,
    priceAmount: 0,
    priceCurrency: "KRW",
  }),
  steamGame(3, 1003, {
    slug: "coming-dread",
    title: "Coming Dread",
    shortDescription: "곧 찾아올 공포.",
    en: { shortDescription: "Dread is coming." },
    comingSoon: true,
    releaseDate: "2099-01-15",
    reviewCount: null,
    languages: ["ko", "en"],
  }),
  steamGame(4, 1004, {
    slug: "adult-night",
    title: "Adult Night",
    shortDescription: "성인용 공포 게임 소개.",
    adult: true,
    status: "PUBLISHED",
  }),
  {
    ...steamGame(5, 0, {}),
    source: "ITCH",
    externalId: "whisper-creek",
    slug: "itch-whisper",
    title: "Whisper Creek",
    shortDescription: "itch.io에서 온 작은 공포.",
    en: { shortDescription: "A small scare from itch.io." },
    headerImageUrl: null,
    media: [],
    reviewCount: 812,
    priceAmount: 500,
    priceCurrency: "USD",
    storeLinks: [{ store: "ITCH", url: "https://example.itch.io/whisper-creek" }],
  },
  steamGame(6, 1006, {
    slug: "lighthouse-keeper",
    title: "등대지기",
    en: { title: "Lighthouse Keeper", shortDescription: "Keep the light on, whatever knocks." },
    shortDescription: "무엇이 두드리든 불을 지켜라.",
    languages: ["ko", "en"],
    coop: true,
    genreSlugs: ["psychological", "survival"],
    reviewCount: 25000,
    status: "PUBLISHED",
    article: {
      kind: "PICK",
      title: "등대에서 보낸 밤",
      oneLiner: "주인장이 밤새 붙잡고 한 게임",
      body: "## 좋았던 점\n\n소리가 정말 무섭다.",
      highlights: ["사운드", "긴장감"],
      sponsored: false,
      sponsorDisclosure: null,
      publishedAt: "2026-09-10T03:00:00Z",
    },
    playVideos: [{ youtubeId: "dQw4w9WgXcQ", title: "등대지기 1화", channelName: null }],
    endingType: "MULTIPLE",
    endings: [
      { name: "새벽의 등대", description: "마지막 밤까지 불을 지킨다.\n문은 끝까지 열지 않는다.", youtubeId: "aBcDeFgHiJk" },
      { name: "바다로", description: null, youtubeId: null },
    ],
  }),
  steamGame(7, 1007, {
    slug: "night-ward",
    title: "Night Ward",
    shortDescription: "불 꺼진 병동을 돌아다닌다.",
    en: { shortDescription: "Wander a ward with the lights out." },
    // 공식 한국어는 없고 유저 한글패치만 있다
    languages: ["en"],
    reviewCount: 300,
    koreanPatches: [{ url: "https://patch.example.com/night-ward", author: "병동 한글화팀", note: "v1.0 기준" }],
  }),
  // 출시일이 오늘인데 Steam에서 아직 안 풀린 게임 (출시 예정으로 본다)
  steamGame(8, 1008, {
    slug: "midnight-gate",
    title: "Midnight Gate",
    shortDescription: "자정에 열리는 문.",
    en: { shortDescription: "A gate that opens at midnight." },
    comingSoon: true,
    releaseDate: seoulDate(),
    reviewCount: null,
    priceAmount: 990000,
    priceCurrency: "KRW",
  }),
];

function localized(game, lang) {
  return lang === "en" && game.en ? { ...game, ...game.en } : game;
}

function genreSummaries(game, lang) {
  return game.genreSlugs.map((slug) => {
    const genre = genres.find((g) => g.slug === slug);
    return { slug, name: lang === "en" ? genre.nameEn : genre.name };
  });
}

function toSummary(raw, lang) {
  const g = localized(raw, lang);
  const picked = g.article?.kind === "PICK";
  return {
    slug: g.slug,
    title: g.title,
    headerImageUrl: g.headerImageUrl,
    releaseDate: g.releaseDate,
    releaseDateText: g.releaseDateText,
    shortDescription: g.shortDescription,
    shortDescriptionTranslated: false,
    languages: g.languages,
    audioLanguages: g.audioLanguages,
    comingSoon: g.comingSoon,
    coop: g.coop,
    adult: g.adult,
    reviewCount: g.reviewCount,
    tags: g.tags,
    genres: genreSummaries(g, lang),
    picked,
    oneLiner: picked ? g.article.oneLiner : null,
    highlights: picked ? g.article.highlights : [],
    sponsored: false,
    hasPlayVideo: g.playVideos.length > 0,
    hasAchievementGuide: g.achievements.length > 0,
    source: g.source,
    reviewed: g.article?.kind === "REVIEW",
    free: g.free,
    multiEnding: g.endingType === "MULTIPLE",
    koreanPatch: g.koreanPatches.length > 0,
  };
}

function toDetail(raw, lang) {
  const g = localized(raw, lang);
  return {
    slug: g.slug,
    title: g.title,
    shortDescription: g.shortDescription,
    shortDescriptionTranslated: false,
    headerImageUrl: g.headerImageUrl,
    languages: g.languages,
    audioLanguages: g.audioLanguages,
    releaseDate: g.releaseDate,
    releaseDateText: g.releaseDateText,
    comingSoon: g.comingSoon,
    coop: g.coop,
    adult: g.adult,
    reviewCount: g.reviewCount,
    genres: genreSummaries(g, lang),
    developers: g.developers,
    // 성인 게임도 백엔드는 미디어를 준다고 가정 (숨기는 건 프론트 몫인지 확인하려고)
    media: g.media,
    storeLinks: g.storeLinks,
    article: g.article,
    playVideos: g.playVideos,
    referenceVideos: g.referenceVideos,
    achievements: g.achievements,
    source: g.source,
    free: g.free,
    priceAmount: g.priceAmount,
    priceCurrency: g.priceCurrency,
    endingType: g.endingType,
    endings: g.endingType === "MULTIPLE" ? g.endings : [],
    koreanPatches: g.koreanPatches,
  };
}

function toAdminGame(g) {
  return {
    id: g.id,
    source: g.source,
    externalId: g.externalId,
    slug: g.slug,
    title: g.title,
    headerImageUrl: g.headerImageUrl,
    releaseDate: g.releaseDate,
    comingSoon: g.comingSoon,
    coop: g.coop,
    reviewCount: g.reviewCount,
    adult: g.adult,
    status: g.status,
    publiclyVisible: g.status !== "HIDDEN",
    hasArticle: g.article != null,
    articleStatus: g.article ? "PUBLISHED" : null,
    sameTitleCount: 0,
  };
}

function toAdminDetail(g) {
  return {
    ...toAdminGame(g),
    shortDescription: g.shortDescription,
    tags: g.tags,
    genreSlugs: g.genreSlugs,
    steamUrl: g.source === "STEAM" ? `https://store.steampowered.com/app/${g.externalId}` : null,
    itchUrl: g.source === "ITCH" ? g.storeLinks[0].url : null,
    trendingOverride: null,
    trendingAuto: false,
    article: g.article && { id: g.id, gameId: g.id, status: "PUBLISHED", ...g.article },
    playVideos: g.playVideos,
    referenceVideos: g.referenceVideos,
    achievements: g.achievements,
    endingType: g.endingType,
    endings: g.endings,
    koreanPatches: g.koreanPatches,
  };
}

function page(items, query, defaultSize) {
  const pageNo = Math.max(0, Number(query.get("page") ?? 0) || 0);
  const size = Math.max(1, Number(query.get("size") ?? defaultSize) || defaultSize);
  const totalPages = Math.ceil(items.length / size);
  return {
    content: items.slice(pageNo * size, pageNo * size + size),
    page: pageNo,
    size,
    totalElements: items.length,
    totalPages,
    hasNext: pageNo + 1 < totalPages,
  };
}

const bool = (query, name) => query.get(name) === "true";

function filterGames(query) {
  const q = query.get("q")?.toLowerCase();
  const release = query.get("release");
  return games.filter((g) => {
    if (g.status === "HIDDEN") return false;
    // 백엔드와 같이 무료 필터는 출시 예정작을 뺀다
    if (bool(query, "free") && (!g.free || g.comingSoon)) return false;
    // 백엔드와 같이 한국어 필터는 유저 한글패치가 있는 게임도 포함한다
    if (bool(query, "korean") && !g.languages.includes("ko") && g.koreanPatches.length === 0) return false;
    if (query.has("coop") && g.coop !== bool(query, "coop")) return false;
    if (bool(query, "picked") && g.article?.kind !== "PICK") return false;
    // 주인장 추천: 글(추천·후기) 또는 주인장 영상이 있는 게임
    if (bool(query, "keeper") && !g.article && g.playVideos.length === 0) return false;
    if (query.get("genre") && !g.genreSlugs.includes(query.get("genre"))) return false;
    if (release === "UPCOMING" && !isUpcoming(g)) return false;
    if (release === "RECENT" && (isUpcoming(g) || !g.releaseDate)) return false;
    // 오늘·내일 출시 숫자는 출시일 기준 (아직 안 풀린 게임도 센다)
    if (release === "TODAY" && g.releaseDate !== seoulDate()) return false;
    if (release === "TOMORROW" && g.releaseDate !== seoulDate(1)) return false;
    if (q && ![g.title, g.en?.title ?? ""].some((t) => t.toLowerCase().includes(q))) return false;
    return true;
  });
}

/** 받은 요청 기록 (경로별). 테스트가 백엔드에 넘어간 쿼리를 확인한다 */
const requests = [];

function send(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(body === undefined ? "" : JSON.stringify(body));
}

const notFound = (res, message = "not found") => send(res, 404, { code: "NOT_FOUND", message });

async function readBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const text = Buffer.concat(chunks).toString("utf8");
  try {
    return text ? JSON.parse(text) : null;
  } catch {
    return text;
  }
}

function handlePublic(req, res, path, query) {
  const lang = query.get("lang") === "en" ? "en" : "ko";
  if (req.method !== "GET") return send(res, 405, { code: "METHOD_NOT_ALLOWED", message: req.method });
  if (path === "/api/games") {
    return send(res, 200, page(filterGames(query).map((g) => toSummary(g, lang)), query, 24));
  }
  const detail = /^\/api\/games\/([^/]+)$/.exec(path);
  if (detail) {
    const slug = decodeURIComponent(detail[1]);
    const game = games.find((g) => g.slug === slug && g.status !== "HIDDEN");
    return game ? send(res, 200, toDetail(game, lang)) : notFound(res, `game ${slug}`);
  }
  if (path === "/api/genres") {
    return send(res, 200, genres.map((g) => ({ slug: g.slug, name: lang === "en" ? g.nameEn : g.name, description: g.description })));
  }
  if (path === "/api/stats") return send(res, 200, { releasedToday: 1, releasingTomorrow: 0 });
  return notFound(res, path);
}

async function handleAdmin(req, res, path, query) {
  if (req.headers["x-admin-key"] !== ADMIN_KEY) return send(res, 403, { code: "FORBIDDEN", message: "invalid admin key" });
  const body = req.method === "GET" ? null : await readBody(req);

  if (req.method === "GET" && path === "/api/admin/games") {
    const status = query.get("status");
    const q = query.get("q")?.toLowerCase();
    const list = games.filter((g) => (!status || g.status === status) && (!q || g.title.toLowerCase().includes(q)));
    return send(res, 200, page(list.map(toAdminGame), query, 30));
  }
  if (req.method === "GET" && path === "/api/admin/games/trending-pins") {
    return send(res, 200, [
      { id: 1, title: "어두운 집", auto: false, order: 0 },
      { id: 6, title: "등대지기", auto: true, order: null },
    ]);
  }
  if (req.method === "PUT" && path === "/api/admin/games/trending-pins/order") return send(res, 200, []);
  if (req.method === "GET" && path === "/api/admin/games/lookup") {
    const appid = query.get("steamAppId");
    const itchUrl = query.get("itchUrl");
    const game = games.find(
      (g) => (appid && g.source === "STEAM" && g.externalId === appid) || (itchUrl && g.storeLinks.some((l) => l.url === itchUrl)),
    );
    return game ? send(res, 200, toAdminDetail(game)) : notFound(res);
  }
  if (path === "/api/admin/ingestion/seeds" && req.method === "POST") return send(res, 200, { appid: body?.appid ?? 0 });
  if (path === "/api/admin/ingestion/itch" && req.method === "POST") return send(res, 200, { gameId: 5 });
  if (path === "/api/admin/ingestion/alert-test" && req.method === "POST") return send(res, 200, { sent: true });

  const byId = /^\/api\/admin\/games\/(\d+)(?:\/(.+))?$/.exec(path);
  if (byId) {
    const game = games.find((g) => g.id === Number(byId[1]));
    if (!game) return notFound(res, `game ${byId[1]}`);
    const action = byId[2];
    if (!action && req.method === "GET") return send(res, 200, toAdminDetail(game));
    // 변경 요청은 상태를 바꾸지 않고 현재 값을 돌려준다 (테스트끼리 서로 영향을 주지 않게)
    if (action === "article" && req.method === "PUT") return send(res, 200, { id: game.id, gameId: game.id, status: "DRAFT", ...body });
    if (action) return send(res, 200, toAdminGame(game));
  }
  return notFound(res, path);
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url ?? "/", "http://mock");
  const path = url.pathname;
  const query = url.searchParams;

  if (path === "/__health") return send(res, 200, { ok: true });
  if (path === "/__requests" || path === "/__last") {
    const target = query.get("path");
    const matched = requests.filter((r) => !target || r.path === target);
    return send(res, 200, path === "/__last" ? (matched.at(-1) ?? null) : matched);
  }

  requests.push({ method: req.method, path, query: Object.fromEntries(query), at: Date.now() });
  try {
    if (path.startsWith("/api/admin/")) return await handleAdmin(req, res, path, query);
    if (path.startsWith("/api/")) return handlePublic(req, res, path, query);
    return notFound(res, path);
  } catch (e) {
    send(res, 500, { code: "MOCK_ERROR", message: String(e) });
  }
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`mock API listening on http://127.0.0.1:${PORT}`);
});
