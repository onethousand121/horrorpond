import { expect, test } from "./fixtures";

// SITE_INDEXING을 켜지 않은 빌드(비공개 운영): robots.txt는 전체 차단, 페이지는 noindex
test("robots.txt: 비공개 운영이면 전체 차단", async ({ request }) => {
  const res = await request.get("/robots.txt");
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toMatch(/User-Agent: \*/i);
  expect(body).toMatch(/Disallow: \/\s*$/m);
  expect(body).not.toMatch(/Sitemap:/i);
});

test("sitemap.xml: 공개 게임은 있고 성인 게임은 없다", async ({ request }) => {
  const res = await request.get("/sitemap.xml");
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toContain("<urlset");
  expect(body).toContain("/games/dark-house</loc>");
  expect(body).toContain("/genres/psychological</loc>");
  expect(body).toContain('hreflang="en"');
  // 영어 주소도 따로 항목이 있다
  expect(body).toContain("/en/games/dark-house</loc>");
  expect(body).toContain('hreflang="x-default"');
  expect(body).not.toContain("adult-night");
});

test("공개 페이지는 noindex", async ({ page }) => {
  await page.goto("/games");
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
});

test("검색·필터·2페이지 이후는 noindex, follow", async ({ page }) => {
  await page.goto("/games?q=house");
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex, follow/);
  await page.goto("/games?free=1");
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex, follow/);
});

test("범위를 넘은 페이지는 404", async ({ page }) => {
  expect((await page.goto("/games?page=999"))?.status()).toBe(404);
  // 백엔드 int 범위를 넘는 값은 1페이지로 본다
  expect((await page.goto("/games?page=3000000000"))?.status()).toBe(200);
});
