[Architecture Decisions - horrorpond MVP]
- Frontend: Next.js App Router, ISR(revalidate 3600), 서버 컴포넌트에서만 백엔드 호출
- Backend: Spring Boot 4.1.x / Java 17, 모듈러 모놀리스
  패키지 모듈: catalog(조회 공개 API), curation(관리자 CRUD), ingestion(수집), common
  ingestion은 catalog/curation 서비스를 직접 호출하지 않고 Repository 레벨로만 반영
- DB: PostgreSQL 16, Flyway 마이그레이션. Redis 미사용(MVP)
- 노출 정책: Game.status = CANDIDATE | PUBLISHED | HIDDEN. 공개 API는 PUBLISHED만 반환
- 장르: 자체 택소노미(curator 관리). Steam genres/categories는 참고용 raw 데이터로만 보관
- 수집: discovery(SteamSpy tag) → enrichment(appdetails, 1.5s throttle, 429→60s backoff)
        → raw_snapshot(JSONB) 저장 → normalize(별도 단계, 재실행 가능)
- 트리거: @Scheduled 일 1회 + POST /api/admin/ingestion/run (X-Admin-Key)
- 동시성: ShedLock(JDBC provider)
- 외부 HTTP: RestClient + Spring Framework 7 @Retryable.
  Steam 호출은 단일 스레드 순차 실행 + 1.5s 고정 간격으로 제어
  (rate limiter 라이브러리 미사용). 429는 클라이언트에서 명시적으로 60s 대기
- 게임 소스: STEAM | ITCH | MANUAL, 외부 ID nullable

[Security Rules]
- Steam API Key, Admin Key 등 비밀값은 환경변수로만 주입. yml에 기본값 금지.
  로컬 DB 접속 정보(docker-compose 값)만 예외
