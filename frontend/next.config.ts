import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactCompiler: true,
  images: {
    // 실제 API 응답의 이미지 호스트: 헤더/스크린샷/트레일러 썸네일 모두
    // shared.akamai.steamstatic.com/store_item_assets/steam/apps/... 이다.
    // URL마다 ?t=<timestamp> 쿼리가 달라 search는 지정하지 않는다.
    remotePatterns: [
      {
        protocol: "https",
        hostname: "shared.akamai.steamstatic.com",
        pathname: "/store_item_assets/steam/apps/**",
      },
    ],
    // Next.js 16부터 필수
    qualities: [75],
  },
};

export default nextConfig;
