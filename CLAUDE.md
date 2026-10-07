## Architecture Decisions (horrorpond MVP)
- Frontend: Next.js App Router, ISR(revalidate 3600), 서버 컴포넌트에서만 백엔드 호출
  다국어(ko/en): 라우트는 app/[lang], 한국어는 접두사 없는 주소(proxy가 /ko로 rewrite), 영어는 /en.
  언어 선택 쿠키(NEXT_LOCALE) 우선, 첫 화면(/)만 브라우저 언어로 /en 이동. UI 문구는 lib/i18n 사전
  빌드는 백엔드에 의존하지 않음: 동적 경로는 generateStaticParams가 [] 반환,
  데이터를 쓰는 고정 경로는 connection()으로 요청 시 렌더링 + fetch 데이터 캐시(1h, 태그)
- Backend: Spring Boot 4.1.x / Java 17, 모듈러 모놀리스
  패키지 모듈: catalog(게임/장르 도메인, 장르 조회 API),
  curation(큐레이션 관리 API + 공개 게임 조회 API), ingestion(수집), common
  (공개 사이트의 게임은 곧 큐레이션된 게임이며, Article 조합이 필요하므로 공개 게임 조회는 curation이 담당)
  ingestion은 catalog/curation 서비스를 직접 호출하지 않고 Repository 레벨로만 반영
- DB: PostgreSQL 16, Flyway 마이그레이션. Redis 미사용(MVP)
- 노출 정책(공포게임 허브): Game.status = CANDIDATE | PUBLISHED | HIDDEN. 규칙은 Game.isPubliclyVisible / ExposurePolicy 한 곳
  HIDDEN 비노출, PUBLISHED(큐레이터 공개) 항상 노출, CANDIDATE(수집됨)는 성인 아님 AND (출시 예정 OR 출시 후 new-release-days(10)일 이내 OR 리뷰 ≥ min-reviews(10))면 자동 노출 (AutoExposure)
  큐레이터 글은 선택(있으면 "재일 추천"). 장르 = 큐레이터 지정 장르 ∪ SteamSpy 태그 매핑(genre.steam_tags)
- 장르: 자체 택소노미(curator 관리) + SteamSpy 태그 자동 매핑. Steam genres/categories는 참고용 raw 데이터로만 보관
- 수집: discovery(Steam 스토어 검색: Horror 최신 출시 300 + 인기 출시 예정 200, SteamSpy tag 전체) → enrichment(appdetails, 1.5s throttle, 429→60s backoff)
        → raw_snapshot(JSONB) 저장 → normalize(별도 단계, 재실행 가능)
  enrichment 우선순위: MANUAL → STEAM_SEARCH(신작) → STEAMSPY_TAG → 갱신 → 재시도
  enrichment는 자동 발견(SteamSpy/검색) seed를 먼저 SteamSpy appdetails(1s 간격) 상위 태그로 판정해
  상위 10개 태그 안에 Horror 계열 태그가 없으면 NOT_HORROR로 기록하고 Steam 호출에서 제외 (MANUAL seed는 판정 안 함)
  appdetails에 recommendations(리뷰 수)가 없으면 appreviews로 받아 같은 모양으로 채운다
  영어 텍스트(이름·짧은 소개·출시일)는 appdetails(l=english, filters=basic,release_date)로 받아 같은 스냅샷의 english 키에 붙인다.
  공개 API는 lang=ko|en (영어 값이 없으면 한국어). 큐레이터 글은 한국어만
- 트리거: @Scheduled 일 1회 + POST /api/admin/ingestion/run (X-Admin-Key)
- 관리 화면: Next.js /admin. Admin Key를 httpOnly 쿠키에 두고 서버에서만 백엔드 호출.
  변경 서버 액션은 updateTag("games")로 공개 페이지 캐시를 즉시 만료
- 배포: 서버 1대 Docker Compose(PostgreSQL + Caddy + app-blue/app-green). deploy/README.md
  CI(GitHub Actions)가 테스트 통과 jar를 올리고 deploy.sh가 blue/green 무중단 전환(readiness 기준)
  프론트는 Vercel. SITE_INDEXING=true 전까지 전체 noindex(비공개 운영)
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

## Test Rules
- 개발 중에는 변경한 영역의 테스트만 실행 (./gradlew test --tests '패키지.*')
- 커밋 직전에만 전체 테스트 실행
