"use client";

import { useSearchParams } from "next/navigation";

/** 검색 결과 화면에서 지금 검색어를 채워 둔다 (고쳐서 다시 찾기 쉽게) */
export function SearchInput({ placeholder }: { placeholder: string }) {
  const q = useSearchParams().get("q") ?? "";
  return <PlainSearchInput key={q} placeholder={placeholder} defaultValue={q} />;
}

export function PlainSearchInput({ placeholder, defaultValue }: { placeholder: string; defaultValue?: string }) {
  return (
    <input
      type="search"
      name="q"
      maxLength={100}
      placeholder={placeholder}
      defaultValue={defaultValue}
      className="w-full min-w-0 bg-transparent text-sm outline-none placeholder:text-muted/70"
    />
  );
}
