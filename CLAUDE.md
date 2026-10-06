## Architecture Decisions (horrorpond MVP)
- Frontend: Next.js App Router, ISR(revalidate 3600), 서버 컴포넌트에서만 백엔드 호출
- Backend: Spring Boot 4.1.x / Java 17, 모듈러 모놀리스
  패키지 모듈: catalog(게임/장르 도메인, 장르 조회 API),
  curation(큐레이션 관리 API + 공개 게임 조회 API), ingestion(수집), common
  (공개 사이트의 게임은 곧 큐레이션된 게임이며, Article 조합이 필요하므로 공개 게임 조회는 curation이 담당)
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

## Security Rules
- Steam API Key, Admin Key 등 비밀값은 환경변수로만 주입. yml에 기본값 금지.
  로컬 DB 접속 정보(docker-compose 값)만 예외

## Package Rules
- 기능별 패키지: catalog, curation, ingestion, common
- 각 기능 내부: domain / repository / application / api / (ingestion만) client
- 의존 방향: curation → catalog, ingestion → catalog. catalog는 다른 기능을 모름.
  역방향 의존 금지

## Entity Rules
- setter 금지. 상태 변경은 의도가 드러나는 메서드로만
- @NoArgsConstructor(access = PROTECTED), 생성은 정적 팩토리
- Lombok @Data/@EqualsAndHashCode/@Setter 엔티티에 사용 금지. @ToString은 연관관계 제외
- enum은 @Enumerated(STRING)
- 시간 타입은 Instant (TIMESTAMPTZ), 날짜는 LocalDate
- 다른 애그리거트는 ID로만 참조 (예: CurationArticle.gameId)

## Migration Rules
- 블루그린 배포 대비: 모든 마이그레이션은 직전 버전 애플리케이션과 호환되어야 함
- 컬럼/테이블 삭제·이름 변경은 expand → migrate → contract 단계로 분리 배포
- 이미 적용된 마이그레이션 파일 수정 금지 (새 버전으로만 변경)

## Transaction Rules
- 외부 HTTP 호출과 sleep은 트랜잭션 밖에서. 트랜잭션은 아이템 1건 단위
- @Transactional/@Retryable은 프록시 기반: 같은 클래스 내부 호출 금지, 별도 빈으로 분리
