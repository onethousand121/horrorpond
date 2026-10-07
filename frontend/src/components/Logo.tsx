import Image from "next/image";

/** 컵 속 개구리(유튜브 프로필과 같은 캐릭터) + 픽셀 워드마크 */
export function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-end gap-2 ${className}`}>
      <Image src="/brand/frog-cup.webp" alt="" width={36} height={48} priority className="h-12 w-auto" />
      <span className="pb-1 font-pixel text-[22px] leading-none tracking-tight">
        lurk<span className="text-accent">pond</span>
      </span>
    </span>
  );
}
