# horrorpond frontend

Next.js 16 (App Router) 공개 사이트입니다. 실행 방법과 캐시 방식은 저장소 루트 README의 "프론트엔드 실행" 섹션을 참고하세요.

- `src/lib/api.ts`: 백엔드 호출 (`server-only`, revalidate 3600, 캐시 태그 `games`, `game:{slug}`, `genres`)
- `src/lib/types.ts`: 백엔드 응답 DTO와 1:1 타입
- `src/components`: GameCard, MediaGallery, TrailerPlayer(hls.js 동적 로드), Markdown 등
