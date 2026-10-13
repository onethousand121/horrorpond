import { expect, test as base, type APIRequestContext } from "@playwright/test";

export const MOCK_API = "http://127.0.0.1:4010";
export const ADMIN_KEY = "e2e-admin-key";

/** mock API가 받은 요청 (e2e/mock-api.mjs의 /__requests) */
export interface MockRequest {
  method: string;
  path: string;
  query: Record<string, string>;
}

export async function mockRequests(request: APIRequestContext, path: string): Promise<MockRequest[]> {
  const res = await request.get(`${MOCK_API}/__requests?path=${encodeURIComponent(path)}`);
  expect(res.ok()).toBeTruthy();
  return res.json();
}

/**
 * 외부 호스트(Steam 이미지, 폰트 CDN, 유튜브)는 막는다: 네트워크에 따라 load 이벤트가 늦어져 테스트가 흔들리지 않게.
 */
export const test = base.extend({
  context: async ({ context }, provide) => {
    await context.route(/^https?:\/\/(?!localhost[:/]|127\.0\.0\.1[:/])/, (route) => route.abort());
    await provide(context);
  },
});

export { expect };
