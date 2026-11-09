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

test("오늘의 게임: 지금 뜨는 아래 큰 카드, 한 줄 소개·협찬 표시, 누르면 상세로", async ({ page }) => {
  await page.goto("/");
  const daily = page.getByRole("region", { name: "오늘의 게임" });
  await expect(daily.getByText("11월 9일")).toBeVisible();
  await expect(daily.getByText("찾아온 사람을 모두 기억하는 집")).toBeVisible();
  await expect(daily.getByLabel("협찬을 받은 추천")).toBeVisible();
  await daily.getByRole("link", { name: "어두운 집" }).click();
  await expect(page).toHaveURL("/games/dark-house");

  await page.goto("/en");
  const dailyEn = page.getByRole("region", { name: "Game of the day" });
  await expect(dailyEn.getByText("A house that remembers every visitor")).toBeVisible();
  await expect(dailyEn.getByText("November 9")).toBeVisible();
});
