import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto max-w-md space-y-4 py-16 text-center">
      <p className="text-5xl font-bold text-accent">404</p>
      <h1 className="text-xl font-bold">페이지를 찾을 수 없습니다</h1>
      <p className="text-muted">주소가 바뀌었거나, 아직 소개되지 않은 게임일 수 있습니다.</p>
      <Link href="/games" className="inline-block rounded border border-border px-4 py-2 text-sm hover:border-foreground/40">
        전체 게임 보기
      </Link>
    </div>
  );
}
