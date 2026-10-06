import type { Metadata } from "next";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = {
  title: "관리자 로그인",
  robots: { index: false, follow: false },
};

export default async function AdminLoginPage({ searchParams }: PageProps<"/admin/login">) {
  const { expired } = await searchParams;
  return (
    <div className="mx-auto max-w-sm space-y-4 py-16">
      <h1 className="text-xl font-bold">관리자 로그인</h1>
      {expired && <p className="text-sm text-muted">관리자 키가 바뀌었거나 만료되었습니다. 다시 입력해 주세요.</p>}
      <LoginForm />
    </div>
  );
}
