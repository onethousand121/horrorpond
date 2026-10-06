"use client";

import { useEffect } from "react";

// Next.js 16.3: error 경계는 reset 대신 retry를 받는다 (node_modules/next/dist/docs 기준)
export default function Error({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="mx-auto max-w-md space-y-4 py-16 text-center">
      <h1 className="text-xl font-bold">일시적인 오류가 발생했습니다</h1>
      <p className="text-muted">잠시 후 다시 시도해 주세요.</p>
      <button
        type="button"
        onClick={() => retry()}
        className="rounded border border-border px-4 py-2 text-sm hover:border-foreground/40"
      >
        다시 시도
      </button>
    </div>
  );
}
