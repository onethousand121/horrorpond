/** 물 위로 눈만 내민 개구리 + 워드마크. 눈동자는 accent(라임) 색. */
export function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <svg width="34" height="22" viewBox="0 0 34 22" aria-hidden className="shrink-0">
        <circle cx="10" cy="9" r="7" fill="#2a3b30" />
        <circle cx="24" cy="9" r="7" fill="#2a3b30" />
        <circle cx="10" cy="9" r="4.2" fill="var(--accent)" />
        <circle cx="24" cy="9" r="4.2" fill="var(--accent)" />
        <rect x="9" y="5.5" width="2" height="7" rx="1" fill="#070b0a" />
        <rect x="23" y="5.5" width="2" height="7" rx="1" fill="#070b0a" />
        <path d="M0 15.5 Q4.25 13 8.5 15.5 T17 15.5 T25.5 15.5 T34 15.5 V22 H0 Z" fill="var(--background)" />
        <path d="M0 15.5 Q4.25 13 8.5 15.5 T17 15.5 T25.5 15.5 T34 15.5" fill="none" stroke="#3b5a4a" strokeWidth="1.2" />
      </svg>
      <span className="text-lg font-bold tracking-tight">
        lurk<span className="text-accent">pond</span>
      </span>
    </span>
  );
}
