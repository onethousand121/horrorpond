import { PondIcon } from "@/components/PondIcon";

/** 연잎 위 연꽃 픽셀 아이콘 + 픽셀 워드마크 */
export function Logo({ className = "" }: { className?: string }) {
  return (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <PondIcon size={30} />
      <span className="font-pixel text-[22px] leading-none tracking-tight">
        lurk<span className="text-accent">pond</span>
      </span>
    </span>
  );
}
