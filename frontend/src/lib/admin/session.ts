import "server-only";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

/**
 * 관리자 키는 httpOnly 쿠키에만 둔다. 브라우저 JS에서 읽을 수 없고, 백엔드 호출은 서버에서만 한다.
 * 회원 시스템 전까지의 임시 방식: 키 자체의 검증은 백엔드 AdminKeyFilter가 매 요청마다 한다.
 */
export const ADMIN_COOKIE = "hp_admin_key";
export const ADMIN_COOKIE_MAX_AGE = 60 * 60 * 24 * 7;

export async function getAdminKey(): Promise<string | undefined> {
  return (await cookies()).get(ADMIN_COOKIE)?.value;
}

/** 키가 없으면 로그인 화면으로 보낸다. */
export async function requireAdminKey(): Promise<string> {
  const key = await getAdminKey();
  if (!key) redirect("/admin/login");
  return key;
}
