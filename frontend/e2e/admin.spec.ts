import { ADMIN_KEY, expect, test } from "./fixtures";

test("쿠키가 없으면 로그인 화면으로", async ({ page }) => {
  await page.goto("/admin");
  await expect(page).toHaveURL("/admin/login");
  await expect(page.getByRole("heading", { name: "관리자 로그인" })).toBeVisible();
});

test("틀린 키는 오류를 보여준다", async ({ page }) => {
  await page.goto("/admin/login");
  await page.getByPlaceholder("ADMIN_API_KEY").fill("wrong-key");
  await page.getByRole("button", { name: "로그인" }).click();
  await expect(page.getByText("관리자 키가 올바르지 않습니다.")).toBeVisible();
  await expect(page).toHaveURL("/admin/login");
});

test("맞는 키로 로그인 → 게임 목록 → 로그아웃", async ({ page }) => {
  await page.goto("/admin/login");
  await page.getByPlaceholder("ADMIN_API_KEY").fill(ADMIN_KEY);
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page).toHaveURL("/admin");
  // 기본 탭(자동 = CANDIDATE)
  await expect(page.getByRole("link", { name: "자동" })).toHaveAttribute("aria-current", "page");
  await expect(page.getByRole("link", { name: "어두운 집" })).toBeVisible();
  await expect(page.getByRole("link", { name: "등대지기" })).toHaveCount(0);

  await page.getByRole("link", { name: "고정 노출" }).click();
  await expect(page).toHaveURL("/admin?tab=published");
  await expect(page.getByRole("link", { name: "등대지기" })).toBeVisible();

  await page.getByRole("link", { name: "등대지기" }).click();
  await expect(page).toHaveURL("/admin/games/6");
  await expect(page.getByRole("heading", { level: 1, name: "등대지기" })).toBeVisible();

  await page.getByRole("button", { name: "로그아웃" }).click();
  await expect(page).toHaveURL("/admin/login");
  await page.goto("/admin");
  await expect(page).toHaveURL("/admin/login");
});
