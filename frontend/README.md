# horrorpond frontend

Next.js 16 (App Router) 공개 사이트입니다. 실행 방법과 캐시 방식은 저장소 루트 README의 "프론트엔드 실행" 섹션을 참고하세요.

- `src/lib/api.ts`: 백엔드 호출 (`server-only`, revalidate 3600, 캐시 태그 `games`, `game:{slug}`, `genres`)
- `src/lib/types.ts`: 백엔드 응답 DTO와 1:1 타입
- `src/components`: GameCard, MediaGallery, TrailerPlayer(hls.js 동적 로드), Markdown 등

## e2e 테스트 (Playwright)

백엔드 없이 `e2e/mock-api.mjs`(fixture JSON을 주는 가짜 API, 포트 4010)와 `next start`(포트 3100)를 띄워 브라우저로 확인합니다.
`next start`를 쓰므로 **먼저 같은 API 주소로 빌드**해야 합니다.

```bash
API_BASE_URL=http://127.0.0.1:4010 npm run build
npm run test:e2e
```

- 브라우저: `npx playwright install chromium`. 이미 설치된 다른 리비전의 Chromium을 쓰려면 `PW_CHROMIUM_PATH=<chrome 실행 파일>`
- 관리자 테스트 키는 `e2e-admin-key` (mock 전용)
- mock이 받은 요청은 `GET http://127.0.0.1:4010/__requests?path=/api/games`로 볼 수 있습니다
