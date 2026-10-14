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

test("멀티 엔딩: 추천 글 아래에 스포일러로 접혀 있고, 누르면 엔딩과 영상이 보인다", async ({ page }) => {
  await page.goto("/games/lighthouse-keeper");
  const article = page.getByRole("region", { name: "주인장 추천", exact: true });
  const endings = article.getByRole("region", { name: "엔딩", exact: true });
  await expect(endings).toBeVisible();
  const summary = endings.locator("summary");
  await expect(summary).toHaveText(/스포일러 주의 · 엔딩 2개 · 눌러서 보기/);
  // 기본은 닫혀 있다
  expect(await endings.locator("details").evaluate((d) => (d as HTMLDetailsElement).open)).toBe(false);
  await expect(endings.getByText("새벽의 등대")).toBeHidden();

  await summary.click();
  await expect(endings.getByRole("heading", { name: "새벽의 등대" })).toBeVisible();
  await expect(endings.getByRole("heading", { name: "바다로" })).toBeVisible();
  // 설명은 줄바꿈을 살린 일반 텍스트
  await expect(endings.getByText("마지막 밤까지 불을 지킨다.")).toBeVisible();
  await expect(endings.locator("ol")).toHaveAttribute("lang", "ko");
  // 영상은 눌러야 재생되는 썸네일
  await expect(endings.getByRole("button", { name: "새벽의 등대 엔딩 영상 재생" })).toBeVisible();
  await expect(endings.locator("iframe")).toHaveCount(0);
});

test("멀티 엔딩: 키보드로도 연다", async ({ page }) => {
  await page.goto("/games/lighthouse-keeper");
  const endings = page.getByRole("region", { name: "엔딩", exact: true });
  await endings.locator("summary").focus();
  await page.keyboard.press("Enter");
  await expect(endings.getByRole("heading", { name: "새벽의 등대" })).toBeVisible();
});

test("영어 화면: 엔딩 안내는 영어", async ({ page }) => {
  await page.goto("/en/games/lighthouse-keeper");
  const endings = page.getByRole("region", { name: "Endings", exact: true });
  await expect(endings.locator("summary")).toHaveText(/Spoilers · 2 endings · click to reveal/);
});

test("단일 엔딩: 한 줄 안내 (글이 없으면 본문 아래)", async ({ page }) => {
  await page.goto("/games/dark-house");
  const endings = page.getByRole("region", { name: "엔딩", exact: true });
  await expect(endings).toBeVisible();
  await expect(endings.getByText("엔딩이 하나뿐인 게임이에요.")).toBeVisible();
  await expect(endings.locator("details")).toHaveCount(0);

  await page.goto("/en/games/dark-house");
  await expect(page.getByText("This game has a single ending.")).toBeVisible();
});

test("엔딩 구분이 없으면 엔딩 영역이 없다", async ({ page }) => {
  await page.goto("/games/free-scream");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Free Scream");
  await expect(page.getByRole("region", { name: "엔딩", exact: true })).toHaveCount(0);
});

test("유저 한글패치: 한국어 화면에만 nofollow 외부 링크와 안내", async ({ page }) => {
  await page.goto("/games/night-ward");
  const row = infoRow(page, "한글패치");
  const link = row.getByRole("link", { name: /병동 한글화팀/ });
  await expect(link).toHaveAttribute("href", "https://patch.example.com/night-ward");
  await expect(link).toHaveAttribute("rel", /\bnofollow\b/);
  await expect(link).toHaveAttribute("rel", /\bugc\b/);
  await expect(link).toHaveAttribute("target", "_blank");
  await expect(row.getByText("v1.0 기준")).toBeVisible();
  await expect(row.getByText("비공식 패치예요. 제작자 배포처에서 받으세요.")).toBeVisible();

  await page.goto("/en/games/night-ward");
  await expect(page.getByRole("heading", { level: 1 })).toHaveText("Night Ward");
  await expect(page.locator('a[href="https://patch.example.com/night-ward"]')).toHaveCount(0);
  await expect(infoRow(page, "Korean patch")).toHaveCount(0);
});

test("추천 글 게임: 게임 소개가 주인장 글보다 먼저 나온다", async ({ page }) => {
  await page.goto("/games/lighthouse-keeper");
  const about = page.getByRole("region", { name: "게임 소개" });
  const articleTitle = page.locator("#article-title");
  await expect(about).toBeVisible();
  const aboutBox = await about.boundingBox();
  const titleBox = await articleTitle.boundingBox();
  expect(aboutBox!.y).toBeLessThan(titleBox!.y);
});
