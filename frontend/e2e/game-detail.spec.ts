import type { Page } from "@playwright/test";
import { expect, test } from "./fixtures";

/** 게임 정보 표의 한 줄 (dt 이름으로 찾는다) */
const infoRow = (page: Page, label: string) =>
  page.locator("dl > div").filter({ has: page.locator("dt", { hasText: new RegExp(`^${label}$`) }) });

test("유료 게임: 원화 정가와 할인 안내", async ({ page }) => {
  await page.goto("/games/dark-house");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("어두운 집");
  const price = infoRow(page, "가격");
  await expect(price.locator("dd")).toContainText("₩16,500");
  await expect(price.locator("dd")).toContainText("할인은 상점에서 확인");
  await expect(infoRow(page, "지원 언어").locator("dd")).toContainText("한국어");
});

test("무료 게임: 가격은 무료, 할인 안내 없음", async ({ page }) => {
  await page.goto("/games/free-scream");
  const price = infoRow(page, "가격");
  await expect(price.locator("dd")).toHaveText("무료");
  await expect(page.getByText("할인은 상점에서 확인")).toHaveCount(0);
});

test("출시 예정 게임: 가격 줄이 없다", async ({ page }) => {
  await page.goto("/games/coming-dread");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Coming Dread");
  await expect(infoRow(page, "출시일")).toBeVisible();
  await expect(infoRow(page, "가격")).toHaveCount(0);
});

test("itch.io 게임: 평가 수와 itch.io 링크", async ({ page }) => {
  await page.goto("/games/itch-whisper");
  await expect(infoRow(page, "itch.io 평가")).toBeVisible();
  await expect(page.getByRole("link", { name: /itch\.io에서 보기/ })).toHaveAttribute(
    "href",
    "https://example.itch.io/whisper-creek",
  );
  await expect(page.getByRole("link", { name: /Steam에서 보기/ })).toHaveCount(0);
});

test("VideoGame JSON-LD가 있다", async ({ page }) => {
  await page.goto("/games/lighthouse-keeper");
  const script = page.locator('script[type="application/ld+json"]');
  await expect(script).toHaveCount(1);
  const data = JSON.parse((await script.textContent()) ?? "{}");
  expect(data).toMatchObject({ "@type": "VideoGame", name: "등대지기", inLanguage: "ko" });
  // 플레이 영상은 공개일(uploadDate)을 몰라 넣지 않는다
  expect(data.subjectOf).toBeUndefined();
});

test("JSON-LD: 출시된 유료 게임은 정가(offers)가 있고 출시 예정작은 출시일·가격이 없다", async ({ page }) => {
  const jsonLd = async (path: string) => {
    await page.goto(path);
    return JSON.parse((await page.locator('script[type="application/ld+json"]').textContent()) ?? "{}");
  };
  expect((await jsonLd("/games/dark-house")).offers).toMatchObject({ "@type": "Offer", price: 16500, priceCurrency: "KRW" });
  const upcoming = await jsonLd("/games/coming-dread");
  expect(upcoming.offers).toBeUndefined();
  expect(upcoming.datePublished).toBeUndefined();
});

test("추천 글 게임: 한국어는 한 줄 소개가 설명, 영어는 게임 소개가 설명", async ({ page }) => {
  await page.goto("/games/lighthouse-keeper");
  await expect(page.getByText("주인장이 밤새 붙잡고 한 게임").first()).toBeVisible();
  await expect(page.getByRole("heading", { name: "등대에서 보낸 밤" })).toBeVisible();
  await expect(page.locator('meta[name="description"]')).toHaveAttribute("content", "주인장이 밤새 붙잡고 한 게임");

  await page.goto("/en/games/lighthouse-keeper");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Lighthouse Keeper");
  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    "content",
    "Keep the light on, whatever knocks.",
  );
  await expect(page.locator('link[rel="canonical"]')).toHaveAttribute("href", /\/en\/games\/lighthouse-keeper$/);
});

test("성인 게임: 소개·스크린샷 없이 안내만, noindex", async ({ page }) => {
  await page.goto("/games/adult-night");
  await expect(page.getByRole("region", { name: "성인 게임" })).toBeVisible();
  await expect(page.getByText("성인용 공포 게임 소개.")).toHaveCount(0);
  await expect(page.getByRole("region", { name: "영상과 스크린샷" })).toHaveCount(0);
  await expect(page.locator('img[src*="ss_1"]')).toHaveCount(0);
  await expect(page.locator('script[type="application/ld+json"]')).toHaveCount(0);
  await expect(page.locator('meta[name="robots"]')).toHaveAttribute("content", /noindex/);
});

test("없는 게임은 404", async ({ page }) => {
  const res = await page.goto("/games/no-such-game");
  expect(res?.status()).toBe(404);
  await expect(page.getByText("404", { exact: true })).toBeVisible();

  const en = await page.goto("/en/games/no-such-game");
  expect(en?.status()).toBe(404);
});

test("라우트에 없는 주소도 사이트 모양의 404", async ({ page }) => {
  const res = await page.goto("/no/such/page");
  expect(res?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "페이지를 찾을 수 없습니다" })).toBeVisible();

  const en = await page.goto("/en/no/such/page");
  expect(en?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "Page not found" })).toBeVisible();
});
