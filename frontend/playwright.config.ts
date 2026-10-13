import { defineConfig, devices } from "@playwright/test";

// 브라우저 e2e. 백엔드 대신 e2e/mock-api.mjs를 띄우고, 미리 만든 빌드(next build)를 next start로 띄운다.
//   API_BASE_URL=http://127.0.0.1:4010 npm run build && npm run test:e2e
const MOCK_PORT = 4010;
const APP_PORT = 3100;
const API_BASE_URL = `http://127.0.0.1:${MOCK_PORT}`;
// localhost: 관리자 쿠키가 Secure(production)라 http에서는 localhost에서만 저장된다
const BASE_URL = `http://localhost:${APP_PORT}`;

// 설치된 Chromium 리비전이 @playwright/test가 원하는 것과 다를 때 직접 지정 (예: /opt/pw-browsers/chromium-1194/chrome-linux/chrome)
const executablePath = process.env.PW_CHROMIUM_PATH || undefined;

// fetch 데이터 캐시가 이전 실행의 응답을 돌려주면 mock이 요청을 못 받으므로 시작 전에 비운다
const CLEAR_FETCH_CACHE = `node -e "require('node:fs').rmSync('.next/cache/fetch-cache',{recursive:true,force:true})"`;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : [["list"]],
  use: {
    baseURL: BASE_URL,
    locale: "ko-KR",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"], launchOptions: { executablePath } },
    },
  ],
  webServer: [
    {
      command: "node e2e/mock-api.mjs",
      url: `${API_BASE_URL}/__health`,
      env: { MOCK_API_PORT: String(MOCK_PORT) },
      reuseExistingServer: !process.env.CI,
      stdout: "pipe",
    },
    {
      command: `${CLEAR_FETCH_CACHE} && npx next start -p ${APP_PORT}`,
      url: `${BASE_URL}/robots.txt`,
      env: { API_BASE_URL, SITE_URL: BASE_URL },
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
});
