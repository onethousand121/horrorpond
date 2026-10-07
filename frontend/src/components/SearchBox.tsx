/**
 * 헤더 검색. 일반 GET 폼이라 자바스크립트 없이도 {action}?q= 로 이동한다.
 */
export function SearchBox({ action, label, placeholder, className = "" }: {
  action: string;
  label: string;
  placeholder: string;
  className?: string;
}) {
  return (
    <form action={action} role="search" className={className}>
      <label className="flex items-center gap-2 rounded-full border border-border bg-surface/70 px-3 py-1.5 focus-within:border-accent/60">
        <svg aria-hidden viewBox="0 0 16 16" className="size-3.5 shrink-0 fill-none stroke-muted stroke-2">
          <circle cx="7" cy="7" r="5" />
          <path d="m11 11 3.5 3.5" />
        </svg>
        <span className="sr-only">{label}</span>
        <input
          type="search"
          name="q"
          maxLength={100}
          placeholder={placeholder}
          className="w-full min-w-0 bg-transparent text-sm outline-none placeholder:text-muted/70"
        />
      </label>
    </form>
  );
}
