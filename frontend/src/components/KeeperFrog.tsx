/**
 * 주인장 추천 마스코트: 따봉하는 개구리 (사이트 아이콘과 같은 픽셀 스타일).
 * 주인장 캐릭터 그림이 생기면 이 컴포넌트만 바꾸면 된다.
 */
export function KeeperFrog({ className = "size-4" }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" aria-hidden className={className} shapeRendering="crispEdges">
      {/* 눈 */}
      <rect x="2" y="1" width="4" height="3" fill="#3f8a6c" />
      <rect x="7" y="1" width="4" height="3" fill="#3f8a6c" />
      <rect x="3" y="2" width="2" height="2" fill="#ffffff" />
      <rect x="8" y="2" width="2" height="2" fill="#ffffff" />
      <rect x="4" y="3" width="1" height="1" fill="#0a1a1f" />
      <rect x="9" y="3" width="1" height="1" fill="#0a1a1f" />
      {/* 얼굴 */}
      <rect x="1" y="4" width="11" height="4" fill="#6fc59a" />
      <rect x="3" y="6" width="7" height="1" fill="#1f4d3a" />
      <rect x="2" y="5" width="1" height="1" fill="#f29bb0" />
      <rect x="10" y="5" width="1" height="1" fill="#f29bb0" />
      <rect x="1" y="8" width="11" height="1" fill="#3f8a6c" />
      {/* 몸 */}
      <rect x="2" y="9" width="9" height="5" fill="#6fc59a" />
      <rect x="4" y="10" width="5" height="4" fill="#a8e6c9" />
      <rect x="2" y="14" width="3" height="1" fill="#3f8a6c" />
      <rect x="8" y="14" width="3" height="1" fill="#3f8a6c" />
      {/* 따봉: 주먹 + 위로 세운 엄지 */}
      <rect x="13" y="3" width="2" height="5" fill="#6fc59a" />
      <rect x="13" y="3" width="2" height="1" fill="#a8e6c9" />
      <rect x="11" y="8" width="5" height="4" fill="#6fc59a" />
      <rect x="11" y="9" width="4" height="1" fill="#3f8a6c" />
      <rect x="11" y="11" width="4" height="1" fill="#3f8a6c" />
      <rect x="11" y="12" width="5" height="1" fill="#1f4d3a" />
    </svg>
  );
}
