import { expect, mockRequests, test } from "./fixtures";

const card = (page: import("@playwright/test").Page, title: string) =>
  page.locator("article").filter({ has: page.getByRole("link", { name: title, exact: true }) });

test("무료 칩: URL에 free=1, 백엔드에 free=true", async ({ page, request }) => {
  await page.goto("/games");
  await expect(card(page, "어두운 집")).toBeVisible();

  await page.getByRole("link", { name: "무료 게임", exact: true }).click();
  await expect(page).toHaveURL(/[?&]free=1(&|$)/);
  await expect(page.getByRole("link", { name: "무료 게임", exact: true })).toHaveAttribute("aria-current", "true");

  // mock이 free=true로 걸러 무료 게임만 돌려준다
  await expect(card(page, "Free Scream")).toBeVisible();
  await expect(card(page, "어두운 집")).toHaveCount(0);

  const received = await mockRequests(request, "/api/games");
  expect(received.some((r) => r.query.free === "true" && r.query.page === "0")).toBe(true);
});

test("한국어 칩은 한국어 화면에만 있고, 켜면 korean=true로 부른다", async ({ page, request }) => {
  await page.goto("/en/games");
  await expect(card(page, "Dark House")).toBeVisible();
  await expect(page.getByRole("link", { name: "Korean supported" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Free games", exact: true })).toBeVisible();

  await page.goto("/games");
  await page.getByRole("link", { name: "한국어 지원", exact: true }).click();
  await expect(page).toHaveURL(/[?&]korean=1(&|$)/);
  await expect(card(page, "어두운 집")).toBeVisible();
  await expect(card(page, "Free Scream")).toHaveCount(0);
  // 공식 한국어가 없어도 유저 한글패치가 있으면 들어간다 (백엔드 필터)
  await expect(card(page, "Night Ward")).toBeVisible();

  const received = await mockRequests(request, "/api/games");
  expect(received.some((r) => r.query.korean === "true")).toBe(true);
});

test("카드 뱃지: 무료, 한국어(한국어 화면만)", async ({ page }) => {
  await page.goto("/games");
  await expect(card(page, "Free Scream").getByText("무료", { exact: true })).toBeVisible();
  await expect(card(page, "어두운 집").getByText("한국어", { exact: true })).toBeVisible();
  // 한국어 미지원 게임에는 한국어 뱃지가 없다
  await expect(card(page, "Free Scream").getByText("한국어", { exact: true })).toHaveCount(0);
  await expect(card(page, "어두운 집").getByText("무료", { exact: true })).toHaveCount(0);

  await page.goto("/en/games");
  await expect(card(page, "Free Scream").getByText("Free", { exact: true })).toBeVisible();
  await expect(card(page, "Dark House")).toBeVisible();
  await expect(card(page, "Dark House").getByText("Korean", { exact: true })).toHaveCount(0);
});

test("검색어는 q로 넘어가고 결과를 보여준다", async ({ page, request }) => {
  await page.goto("/games?q=keeper");
  await expect(card(page, "등대지기")).toBeVisible();
  await expect(card(page, "어두운 집")).toHaveCount(0);
  const received = await mockRequests(request, "/api/games");
  expect(received.some((r) => r.query.q === "keeper" && r.query.sort === "POPULAR")).toBe(true);
  // 결과 화면의 검색창에 검색어가 남아 있다
  await expect(page.getByRole("search").getByRole("searchbox")).toHaveValue("keeper");
});

test("카드 뱃지: 한글패치(한국어 화면만, 공식 한국어 게임엔 없음), 멀티 엔딩은 스포일러라 없음", async ({ page }) => {
  await page.goto("/games");
  await expect(card(page, "Night Ward").getByText("한글패치", { exact: true })).toBeVisible();
  await expect(card(page, "Night Ward").getByText("한국어", { exact: true })).toHaveCount(0);
  await expect(card(page, "어두운 집").getByText("한글패치", { exact: true })).toHaveCount(0);
  await expect(card(page, "등대지기").getByText("멀티 엔딩", { exact: true })).toHaveCount(0);

  await page.goto("/en/games");
  await expect(card(page, "Night Ward")).toBeVisible();
  await expect(card(page, "Night Ward").getByText("Korean patch", { exact: true })).toHaveCount(0);
  await expect(card(page, "Lighthouse Keeper").getByText("Multiple endings", { exact: true })).toHaveCount(0);
});
