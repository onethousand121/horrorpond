import { expect, mockRequests, test } from "./fixtures";

const card = (page: import("@playwright/test").Page, title: string) =>
  page.locator("article").filter({ has: page.getByRole("link", { name: title, exact: true }) });

test("모음 페이지: 제목·소개·카드, 전체 보기 링크, 다른 모음 링크", async ({ page, request }) => {
  const res = await page.goto("/collections/free");
  expect(res?.status()).toBe(200);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("무료 공포게임");
  await expect(page.getByText("지금 무료로 받을 수 있는 공포게임을 모았습니다.")).toBeVisible();
  await expect(card(page, "Free Scream")).toBeVisible();
  await expect(card(page, "어두운 집")).toHaveCount(0);
  await expect(page.getByRole("link", { name: "전체 보기" })).toHaveAttribute("href", "/games?view=popular&free=1");

  const others = page.getByRole("main").getByRole("navigation", { name: "다른 공포게임 모음" });
  await expect(others.getByRole("link", { name: "한국어 지원 공포게임" })).toHaveAttribute("href", "/collections/korean");
  await expect(others.getByRole("link", { name: "무료 공포게임" })).toHaveCount(0);

  const received = await mockRequests(request, "/api/games");
  expect(received.some((r) => r.query.free === "true" && r.query.sort === "POPULAR")).toBe(true);
});

test("모음 페이지는 색인 대상: 필터 목록과 달리 noindex, follow가 아니고 canonical·hreflang이 있다", async ({ page }) => {
  await page.goto("/collections/trending");
  // 비공개 운영 빌드라 사이트 전체 noindex(레이아웃)만 남고, 필터 목록의 noindex, follow는 붙지 않는다
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", "noindex, nofollow");
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", /\/collections\/trending$/);
  await expect(page.locator('link[rel="alternate"][hreflang="en"]')).toHaveAttribute("href", /\/en\/collections\/trending$/);
  await expect(page.locator('link[rel="alternate"][hreflang="x-default"]')).toHaveAttribute("href", /\d\/collections\/trending$/);

  // 성인 게임은 모음에 넣지 않는다
  await expect(card(page, "어두운 집")).toBeVisible();
  await expect(card(page, "Adult Night")).toHaveCount(0);

  const jsonLd = JSON.parse((await page.locator('script[type="application/ld+json"]').textContent()) ?? "{}");
  expect(jsonLd["@type"]).toBe("ItemList");
  expect(jsonLd.itemListElement[0].url).toMatch(/\/games\/[a-z-]+$/);
  expect(JSON.stringify(jsonLd)).not.toContain("adult-night");
});

test("영어 모음 페이지", async ({ page, request }) => {
  await page.goto("/en/collections/upcoming");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Upcoming horror games");
  await expect(card(page, "Coming Dread")).toBeVisible();
  await expect(card(page, "Dark House")).toHaveCount(0);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", /\/en\/collections\/upcoming$/);
  await expect(page.getByRole("link", { name: "See all" })).toHaveAttribute("href", "/en/games?view=upcoming");
  // 한국어 전용 모음은 영어 화면에 링크가 없다
  await expect(page.getByRole("link", { name: /Korean support/ })).toHaveCount(0);

  const received = await mockRequests(request, "/api/games");
  expect(received.some((r) => r.query.release === "UPCOMING" && r.query.lang === "en")).toBe(true);
});

test("한국어 지원 모음은 한국어 화면에만 있다", async ({ page }) => {
  expect((await page.goto("/collections/korean"))?.status()).toBe(200);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("한국어 지원 공포게임");
  await expect(card(page, "Night Ward")).toBeVisible();
  await expect(card(page, "Free Scream")).toHaveCount(0);
  // 영어 버전이 없으므로 hreflang en도 없고, 언어 전환기는 영어 목록으로 보낸다
  await expect(page.locator('link[rel="alternate"][hreflang="en"]')).toHaveCount(0);
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", /\/collections\/korean$/);
  await expect(page.getByRole("navigation", { name: "언어" }).getByRole("link", { name: "English" })).toHaveAttribute("href", "/en/games");

  expect((await page.goto("/en/collections/korean"))?.status()).toBe(404);
  expect((await page.goto("/collections/nope"))?.status()).toBe(404);
});

test("홈과 푸터에서 모음으로 간다", async ({ page }) => {
  await page.goto("/");
  const footer = page.getByRole("contentinfo").getByRole("navigation", { name: "공포게임 모음" });
  await expect(footer.getByRole("link", { name: "주인장 추천 공포게임" })).toHaveAttribute("href", "/collections/picks");
  await page.getByRole("main").getByRole("navigation", { name: "공포게임 모음" }).getByRole("link", { name: "주인장 추천 공포게임" }).click();
  await expect(page).toHaveURL(/\/collections\/picks$/);
  await expect(card(page, "등대지기")).toBeVisible();
  await expect(card(page, "Free Scream")).toHaveCount(0);
});

test("sitemap.xml에 모음 주소가 있다 (한국어 전용 모음은 한국어만)", async ({ request }) => {
  const body = await (await request.get("/sitemap.xml")).text();
  for (const slug of ["free", "upcoming", "trending", "picks"]) {
    expect(body).toContain(`/collections/${slug}</loc>`);
    expect(body).toContain(`/en/collections/${slug}</loc>`);
  }
  expect(body).toContain("/collections/korean</loc>");
  expect(body).not.toContain("/en/collections/korean");
});
