import type { Metadata } from "next";
import Link from "next/link";
import { ImportForm } from "./ImportForm";

export const metadata: Metadata = { title: "지금 뜨는 조사 반영" };

// itch.io 신규 게임은 페이지를 바로 받아 오느라 몇 초씩 걸린다 (Vercel 함수 시간 상한)
export const maxDuration = 60;

export default function TrendingImportPage() {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <Link href="/admin" className="text-sm text-muted hover:text-foreground">
          ← 게임 목록
        </Link>
        <h1 className="text-xl font-bold">지금 뜨는 조사 반영</h1>
        <p className="text-sm text-muted">
          매주 Perplexity가 보낸 결과(JSON)를 그대로 붙여 넣고 반영하기를 누르세요. 게임을 찾아 &quot;지금 뜨는&quot;에 고정하고,
          사이트에 없는 게임은 추가하고, 내 영상이 없는 게임에는 참고 영상을 붙여요. 지난주에 자동으로 고정한 게임 중
          이번 결과에 없는 건 고정을 풀어요(결과가 5개 이상일 때만, 직접 고정한 게임은 그대로).
        </p>
      </div>
      <ImportForm />
    </div>
  );
}
