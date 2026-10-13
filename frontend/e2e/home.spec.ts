import { expect, test } from "./fixtures";

test("한국어 홈: 제목과 빠른 필터", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveURL("/");
  await expect(page.locator("html")).toHaveAttribute("lang", "ko");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("공포게임 보관소");

  const filters = page.getByRole("navigation", { name: "빠른 필터" });
  await expect(filters.getByRole("link", { name: "무료 게임" })).toHaveAttribute("href", "/games?free=1");
  await expect(filters.getByRole("link", { name: "한국어 지원" })).toHaveAttribute("href", "/games?korean=1");
  // 장르 칩은 /api/genres에서 온다
  await expect(filters.getByRole("link", { name: "심리 공포" })).toBeVisible();
});

test("영어 홈: 영어 UI, 한국어 필터 없음", async ({ page }) => {
  await page.goto("/en");
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("The Horror Game Archive");

  const filters = page.getByRole("navigation", { name: "Quick filters" });
  await expect(filters.getByRole("link", { name: "Free games" })).toHaveAttribute("href", "/en/games?free=1");
  await expect(filters.getByRole("link", { name: "Psychological" })).toBeVisible();
  await expect(filters.getByRole("link", { name: "Korean supported" })).toHaveCount(0);
});

test("첫 화면은 브라우저 언어가 영어면 /en으로 보낸다", async ({ browser }) => {
  const context = await browser.newContext({ locale: "en-US" });
  await context.route(/^https?:\/\/(?!localhost[:/]|127\.0\.0\.1[:/])/, (route) => route.abort());
  const page = await context.newPage();
  await page.goto("/");
  await expect(page).toHaveURL("/en");
  await context.close();
});
