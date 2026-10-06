import type { Metadata } from "next";
import Link from "next/link";
import { requireAdminKey } from "@/lib/admin/session";
import { logout } from "../login/actions";

export const metadata: Metadata = {
  title: "관리자",
  robots: { index: false, follow: false },
};

/** /admin 하위는 관리자 키 쿠키가 있어야 한다. 키 자체의 검증은 백엔드가 매 요청마다 한다. */
export default async function AdminLayout({ children }: LayoutProps<"/admin">) {
  await requireAdminKey();
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between rounded border border-border bg-surface px-4 py-2 text-sm">
        <Link href="/admin" className="font-medium">
          큐레이션 관리
        </Link>
        <form action={logout}>
          <button type="submit" className="text-muted hover:text-foreground">
            로그아웃
          </button>
        </form>
      </div>
      {children}
    </div>
  );
}
