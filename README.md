# horrorpond (가칭)

> 공포게임 전문 큐레이션 라이브러리

인디 공포게임을 중심으로, 큐레이터가 직접 플레이하고 선별한 게임만 소개하는 사이트입니다.
Steam 데이터를 자동으로 수집하지만, **큐레이터가 검증하고 큐레이션 글을 붙인 게임만 노출**됩니다.

## 컨셉

- **장점 중심 큐레이션**: 점수나 별점 대신 "이 게임의 장점"을 구조화된 데이터(highlights)로 제공합니다. 선별되었다는 사실 자체가 평가입니다.
- **공개 게이트**: 수집된 게임은 `CANDIDATE` 상태로만 저장됩니다. 큐레이션 글이 발행된 게임만 `PUBLISHED`가 될 수 있고, 이 규칙은 도메인 모델에서 강제됩니다.
- **자체 장르 택소노미**: 심리 공포, 서바이벌 호러, 아날로그 호러 등 서브장르를 큐레이터가 직접 정의합니다. Steam의 genre 데이터에는 "Horror"가 없기 때문입니다(유저 태그로만 존재).
- **협찬 투명성**: 협찬 콘텐츠는 표시 문구 없이 저장할 수 없도록 DB 제약으로 막았습니다.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Backend | Java 17, Spring Boot 4.1, Spring Data JPA (Hibernate 7), QueryDSL 7 |
| DB | PostgreSQL 16, Flyway |
| 수집/스케줄링 | RestClient, Spring Framework 7 `@Retryable`, ShedLock |
| 테스트 | JUnit 5, Testcontainers (PostgreSQL) |
| Frontend | Next.js 16 App Router, ISR, Tailwind CSS, hls.js |

## 아키텍처

```
┌──────────────────────────────────────────────┐
│ Next.js (App Router, ISR)                     │
│  서버 컴포넌트에서만 백엔드 호출               │
└───────────────────────┬──────────────────────┘
                        │ REST
┌───────────────────────▼──────────────────────┐
│ Spring Boot (모듈러 모놀리스)                  │
│  catalog   : 게임/장르 조회 (공개 API)         │
│  curation  : 큐레이션 글, 공개 처리            │
│  ingestion : discovery → enrichment → normalize│
└──────┬──────────────────────────┬────────────┘
       │                          │ 단일 스레드, 1.5s 간격
┌──────▼──────┐        ┌──────────▼───────────────┐
│ PostgreSQL  │        │ SteamSpy (태그 기반 시드)  │
│ + raw JSONB │        │ Steam Store appdetails     │
└─────────────┘        └──────────────────────────┘
```

**패키지 의존 방향**: `curation → catalog`, `ingestion → catalog`. catalog는 다른 기능을 알지 못합니다.

## 데이터 수집 파이프라인

| 단계 | 역할 | 비고 |
|---|---|---|
| Discovery | SteamSpy `tag=Horror`로 후보 appid 확보 | `steam_app_seed` |
| Enrichment | Steam Store `appdetails` 호출, 원본 JSON 저장 | 약 200회/5분 제한 → 1.5초 간격, 429 시 60초 대기 |
| Normalize | raw JSONB → `game` 테이블 반영 | 외부 호출 없이 재실행 가능, hash 비교로 변경분만 처리 |

정규화 시에는 Steam이 소유한 필드(제목, 설명, 미디어 등)만 갱신합니다. 큐레이터가 소유한 필드(공개 상태, slug, 장르)는 재수집해도 덮어쓰지 않습니다.

## 설계 결정 기록

### Spring Boot 3.5 → 4.1 전환
프로젝트 생성 직후, 3.5가 2026년 6월 OSS 지원이 종료된 버전임을 확인했습니다. 빈 뼈대 상태여서 전환 비용이 거의 없었기 때문에 4.1로 올렸습니다. 4.0은 2026년 말 지원이 끝나므로 4.1을 선택했습니다. 이 과정에서 Boot 4 모듈화로 바뀐 스타터 구성(Flyway 전용 스타터 등)을 반영했습니다.

### Resilience4j 제거
Boot 4용 스타터는 단일 버전뿐이었고, 4.1에서 동작이 검증되지 않은 상태였습니다. 수집 작업은 단일 스레드로 순차 실행되므로 rate limiter 대신 고정 간격 대기로 충분하다고 판단했습니다. 재시도는 프레임워크에 내장된 Spring Framework 7 `@Retryable`로 대체했습니다.

### 원본 스냅샷과 정규화 분리
정규화 로직은 반복해서 수정됩니다. 그때마다 수천 건을 재호출하면 rate limit 때문에 수 시간이 걸리므로, 원본 응답을 JSONB로 보관하고 정규화를 독립된 단계로 분리했습니다.

### H2 대신 Testcontainers
`text[]`와 `jsonb` 컬럼은 H2에서 재현되지 않습니다. 운영 DB와 같은 PostgreSQL 16 컨테이너에서 테스트합니다.

### Redis 미도입 (MVP)
Next.js ISR이 1차 캐시 역할을 합니다. 성능을 측정해 병목이 확인된 뒤 도입할 예정입니다.

## 로컬 실행

**요구 사항**: Docker, JDK 17 이상 (Gradle toolchain이 17을 사용)

```bash
# DB
docker compose up -d

# 백엔드
cd backend
./gradlew bootRun

# 헬스 체크
curl http://localhost:8080/actuator/health
```

비밀값(Steam API Key, Admin Key 등)은 환경변수로만 주입합니다.

## 프론트엔드 실행

**요구 사항**: Node.js 20 이상, 백엔드 실행 중 (위 "로컬 실행")

```bash
cd frontend
cp .env.example .env.local   # API_BASE_URL, SITE_URL (서버 전용 환경변수)
npm install
npm run dev                  # http://localhost:3000
```

- 백엔드는 서버 컴포넌트에서만 호출합니다. 환경변수에 `NEXT_PUBLIC_` 접두사를 쓰지 않아 브라우저 번들에 노출되지 않습니다.
- 캐시: Cache Components를 쓰지 않는 방식(fetch `next.revalidate`/`next.tags` + 세그먼트 `revalidate = 3600`)입니다. 없는 게임/장르는 실제 404 상태 코드를 돌려줍니다.
- `npm run build`는 **백엔드 없이** 동작합니다. 빌드 때 백엔드를 호출하는 페이지가 없습니다.
  - 게임 상세(`/games/[slug]`), 장르(`/genres/[slug]`): ISR. 빌드 때는 만들지 않고 첫 방문 때 생성한 뒤 1시간마다 갱신합니다.
  - 홈, 협동(`/coop`), 전체 목록(`/games`), `sitemap.xml`: 요청 시 렌더링합니다. 백엔드 응답은 fetch 데이터 캐시(1시간)에 저장되므로 백엔드 호출 빈도는 ISR과 같습니다.

## 프로젝트 구조

```
.
├── CLAUDE.md            # 아키텍처 결정 및 코드 규칙
├── docker-compose.yml   # PostgreSQL 16
├── http/                # IntelliJ HTTP Client 예시 요청 (관리자 큐레이션)
├── frontend/            # Next.js 공개 사이트 (src/app, src/components, src/lib)
└── backend/
    └── src/main/java/com/horrorpond/
        ├── catalog/     # 게임, 장르, 개발사
        ├── curation/    # 큐레이션 글, 공개 처리
        ├── ingestion/   # Steam 수집 파이프라인
        └── common/      # 설정, 공통 도메인, 유틸
```

## 로드맵

**MVP**
- [x] 프로젝트 뼈대 (Spring Boot 4.1, PostgreSQL, Flyway)
- [x] 초기 스키마 (V1)
- [x] 도메인 엔티티 및 Repository
- [x] Steam 수집 파이프라인 (스케줄러 + 수동 트리거)
- [x] 게임 목록/상세/장르 필터 API
- [x] 큐레이션 글 작성 및 공개 처리
- [x] Next.js 프론트엔드

**이후**
- [ ] AI 기반 취향 추천 (pgvector)
- [ ] 회원 시스템
- [ ] 관리자 페이지
- [ ] 스토어 제휴 링크
