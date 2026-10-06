"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { isValidAdminKey } from "@/lib/admin/api";
import { ADMIN_COOKIE, ADMIN_COOKIE_MAX_AGE } from "@/lib/admin/session";
import type { ActionResult } from "@/lib/admin/types";

export async function login(_prev: ActionResult | null, formData: FormData): Promise<ActionResult> {
  const key = String(formData.get("key") ?? "").trim();
  if (!key) return { ok: false, message: "관리자 키를 입력하세요." };
  if (!(await isValidAdminKey(key))) return { ok: false, message: "관리자 키가 올바르지 않습니다." };

  (await cookies()).set(ADMIN_COOKIE, key, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/admin",
    maxAge: ADMIN_COOKIE_MAX_AGE,
  });
  redirect("/admin");
}

export async function logout(): Promise<void> {
  (await cookies()).delete({ name: ADMIN_COOKIE, path: "/admin" });
  redirect("/admin/login");
}
