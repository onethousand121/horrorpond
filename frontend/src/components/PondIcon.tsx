/**
 * 연잎 위 연꽃 픽셀 아이콘 (16x10 격자). 사이트 로고와 파비콘(app/icon.svg)에 같은 그림을 쓴다.
 */
export function PondIcon({ size = 28 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={Math.round((size * 10) / 16)}
      viewBox="0 0 16 10"
      shapeRendering="crispEdges"
      aria-hidden
      className="shrink-0"
    >
      <rect x="6" y="0" width="2" height="1" fill="#b07fd6" />
      <rect x="10" y="0" width="2" height="1" fill="#b07fd6" />
      <rect x="5" y="1" width="1" height="1" fill="#b07fd6" />
      <rect x="6" y="1" width="2" height="1" fill="#d4a5f0" />
      <rect x="8" y="1" width="2" height="1" fill="#b07fd6" />
      <rect x="10" y="1" width="2" height="1" fill="#d4a5f0" />
      <rect x="12" y="1" width="1" height="1" fill="#b07fd6" />
      <rect x="6" y="2" width="1" height="1" fill="#b07fd6" />
      <rect x="7" y="2" width="4" height="1" fill="#d4a5f0" />
      <rect x="11" y="2" width="1" height="1" fill="#b07fd6" />
      <rect x="7" y="3" width="1" height="1" fill="#b07fd6" />
      <rect x="8" y="3" width="2" height="1" fill="#d4a5f0" />
      <rect x="10" y="3" width="1" height="1" fill="#b07fd6" />
      <rect x="3" y="4" width="5" height="1" fill="#3f8a6c" />
      <rect x="8" y="4" width="2" height="1" fill="#b07fd6" />
      <rect x="10" y="4" width="3" height="1" fill="#3f8a6c" />
      <rect x="1" y="5" width="2" height="1" fill="#3f8a6c" />
      <rect x="3" y="5" width="10" height="1" fill="#a8e6c9" />
      <rect x="13" y="5" width="2" height="1" fill="#3f8a6c" />
      <rect x="0" y="6" width="1" height="1" fill="#3f8a6c" />
      <rect x="1" y="6" width="14" height="1" fill="#a8e6c9" />
      <rect x="15" y="6" width="1" height="1" fill="#3f8a6c" />
      <rect x="0" y="7" width="1" height="1" fill="#3f8a6c" />
      <rect x="1" y="7" width="14" height="1" fill="#a8e6c9" />
      <rect x="15" y="7" width="1" height="1" fill="#3f8a6c" />
      <rect x="1" y="8" width="2" height="1" fill="#3f8a6c" />
      <rect x="3" y="8" width="5" height="1" fill="#a8e6c9" />
      <rect x="9" y="8" width="4" height="1" fill="#a8e6c9" />
      <rect x="13" y="8" width="2" height="1" fill="#3f8a6c" />
      <rect x="3" y="9" width="5" height="1" fill="#3f8a6c" />
      <rect x="10" y="9" width="4" height="1" fill="#3f8a6c" />
    </svg>
  );
}
