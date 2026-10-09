import Image from "next/image";

/**
 * 주인장 추천 마스코트: 오케이 하는 개구리 (주인장 버츄얼 캐릭터).
 * head는 배지용(얼굴과 손만), full은 섹션 머리용(화분까지).
 */
export function KeeperFrog({ variant = "head", className = "" }: { variant?: "head" | "full"; className?: string }) {
  return variant === "full" ? (
    <Image src="/keeper-frog.webp" alt="" width={244} height={160} className={`h-auto ${className}`} />
  ) : (
    <Image src="/keeper-frog-head.webp" alt="" width={100} height={48} className={`h-auto ${className}`} />
  );
}
