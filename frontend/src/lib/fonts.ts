import localFont from "next/font/local";

/** 제목·장식용 픽셀 폰트 (갈무리11 Bold, OFL. src/fonts/galmuri/OFL.md). 공개 사이트와 관리 화면이 함께 쓴다 */
export const galmuri = localFont({
  src: "../fonts/galmuri/Galmuri11-Bold.woff2",
  weight: "700",
  variable: "--font-galmuri",
  display: "swap",
});

/** 본문 폰트 Pretendard: 페이지에 쓰인 글자 묶음만 내려받는 동적 서브셋 (공식 권장 방식) */
export const PRETENDARD_CSS =
  "https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css";
