import type { Metadata } from "next";
import { CURATOR } from "@/lib/site";

export const metadata: Metadata = {
  title: "소개",
  description: "lurkpond는 공포게임을 한곳에서 모아 보는 사이트입니다.",
};

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-10">
      <section className="space-y-4">
        <h1 className="font-pixel text-[22px]">lurkpond 소개</h1>
        <p className="leading-relaxed text-foreground/90">
          lurkpond는 Steam의 공포게임을 한곳에서 모아 보는 사이트입니다. 새로 올라온 공포게임과 출시 예정작을 매일
          자동으로 수집해 보여주고, 운영자 {CURATOR.name}가 직접 플레이한 게임에는 영상과 추천 이유를 덧붙입니다.
        </p>
        <ul className="list-disc space-y-1 pl-5 text-foreground/90">
          <li>출시 예정작은 바로, 출시된 게임은 Steam 리뷰가 어느 정도 쌓이면 목록에 올라옵니다.</li>
          <li>심리 공포, 아날로그 호러 같은 서브장르는 Steam 태그를 바탕으로 자동 분류합니다.</li>
          <li>&quot;재일 추천&quot; 표시는 직접 플레이하고 추천하는 게임입니다.</li>
          <li>협찬을 받은 글은 항상 협찬 표시와 고지 문구를 함께 보여줍니다.</li>
        </ul>
      </section>

      <section className="space-y-3 rounded-lg border border-border bg-surface/60 p-6">
        <h2 className="text-lg font-bold">개발사 프리뷰 / 노출 문의</h2>
        <p className="text-sm leading-relaxed text-foreground/90">
          출시 예정이거나 출시된 공포게임의 프리뷰 키 제공, 소개 요청은 아래로 연락해 주세요. 추천 여부와 내용은
          직접 플레이한 뒤 독립적으로 결정하며, 협찬인 경우 글에 명시합니다.
        </p>
        {/* TODO: 실제 연락처(이메일/폼)로 교체 */}
        <p className="text-sm">
          <span className="text-muted">연락처</span> <span className="text-amber-300">TODO: contact@example.com</span>
        </p>
      </section>
    </div>
  );
}
