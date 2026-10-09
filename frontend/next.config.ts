import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactCompiler: true,
  images: {
    // Steam·itch.io·유튜브 CDN이 이미 크기별 이미지를 주므로 Vercel 이미지 최적화를 거치지 않고 원본 주소로 바로 받는다.
    // (최적화를 거치면 처음 보는 이미지마다 변환을 기다려 늦게 뜨고, Hobby 플랜의 월 변환 한도도 금방 찬다)
    unoptimized: true,
    // 실제 API 응답의 이미지 호스트: 헤더/스크린샷/트레일러 썸네일 모두
    // shared.akamai.steamstatic.com/store_item_assets/steam/apps/... 이다.
    // URL마다 ?t=<timestamp> 쿼리가 달라 search는 지정하지 않는다.
    remotePatterns: [
      {
        protocol: "https",
        hostname: "shared.akamai.steamstatic.com",
        pathname: "/store_item_assets/steam/apps/**",
      },
      // 유튜브 영상 썸네일 (lib/youtube)
      { protocol: "https", hostname: "i.ytimg.com", pathname: "/vi/**" },
      // itch.io 게임 표지·스크린샷
      { protocol: "https", hostname: "img.itch.zone", pathname: "/**" },
    ],
    // Next.js 16부터 필수
    qualities: [75],
  },
};

export default nextConfig;
