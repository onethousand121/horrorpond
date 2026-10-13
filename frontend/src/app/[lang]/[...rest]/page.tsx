import { notFound } from "next/navigation";

// 라우트에 없는 주소도 [lang] 레이아웃 안의 404(not-found.tsx)로 보여준다 (Next 기본 404 대신)
export default function CatchAll() {
  notFound();
}
