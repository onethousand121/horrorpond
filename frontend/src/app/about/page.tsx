import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "소개",
  description: "horrorpond는 큐레이터가 직접 고른 공포게임만 소개합니다.",
};

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-2xl space-y-10">
      <section className="space-y-4">
        <h1 className="text-2xl font-bold">horrorpond 소개</h1>
        <p className="leading-relaxed text-foreground/90">
          horrorpond는 인디 공포게임을 중심으로, 큐레이터가 직접 플레이하고 선별한 게임만 소개합니다. Steam
          데이터를 자동으로 수집하지만, 큐레이터가 검증하고 글을 붙인 게임만 사이트에 공개됩니다.
        </p>
        <ul className="list-disc space-y-1 pl-5 text-foreground/90">
          <li>점수나 별점 대신, 이 게임의 장점을 짚어 드립니다.</li>
          <li>심리 공포, 아날로그 호러 같은 서브장르는 큐레이터가 직접 분류합니다.</li>
          <li>협찬을 받은 글은 항상 협찬 표시와 고지 문구를 함께 보여줍니다.</li>
        </ul>
      </section>

      <section className="space-y-3 rounded-lg border border-border bg-surface/60 p-6">
        <h2 className="text-lg font-bold">개발사 프리뷰 / 노출 문의</h2>
        <p className="text-sm leading-relaxed text-foreground/90">
          출시 예정이거나 출시된 공포게임의 프리뷰 키 제공, 소개 요청은 아래로 연락해 주세요. 소개 여부와
          내용은 큐레이터가 직접 플레이한 뒤 독립적으로 결정하며, 협찬인 경우 글에 명시합니다.
        </p>
        {/* TODO: 실제 연락처(이메일/폼)로 교체 */}
        <p className="text-sm">
          <span className="text-muted">연락처</span> <span className="text-amber-300">TODO: contact@example.com</span>
        </p>
      </section>
    </div>
  );
}
