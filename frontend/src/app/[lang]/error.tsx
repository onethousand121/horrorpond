"use client";

import { useParams } from "next/navigation";
import { useEffect } from "react";
import { DEFAULT_LOCALE, getDictionary, isLocale } from "@/lib/i18n";

// Next.js 16.3: error 경계는 reset 대신 retry를 받는다 (node_modules/next/dist/docs 기준)
export default function Error({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  const { lang } = useParams<{ lang: string }>();
  const t = getDictionary(isLocale(lang) ? lang : DEFAULT_LOCALE).error;
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="mx-auto max-w-md space-y-4 py-16 text-center">
      <h1 className="text-xl font-bold">{t.title}</h1>
      <p className="text-muted">{t.body}</p>
      <button
        type="button"
        onClick={() => retry()}
        className="rounded border border-border px-4 py-2 text-sm hover:border-foreground/40"
      >
        {t.retry}
      </button>
    </div>
  );
}
