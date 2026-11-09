# 인수인계 (새 Claude 세션용)

새 세션은 이 문서와 루트 `CLAUDE.md`를 먼저 읽는다. `CLAUDE.md`는 설계 규칙,
이 문서는 **운영자와 합의한 작업 방식·진행 상황·주의점**이다. (2026-11-01 기준)

이 저장소는 **공개 저장소**다. 이 문서에 키·비밀번호·개인 연락처를 적지 않는다.
운영자 개인 메모(개발사 연락, 받은 키 등)는 비공개 아티팩트 "lurkpond 작업 노트"에 있다.

---

## 1. 서비스 한눈에

| 항목 | 내용 |
|---|---|
| 사이트 | https://lurkpond.com (한국어 기본, 영어 `/en`) · 관리 화면 `/admin` |
| 백엔드 | https://api.lurkpond.com — Spring Boot, AWS Lightsail 서버 1대, Docker Compose blue/green (`deploy/README.md`) |
| 프론트 | Next.js 16 on Vercel (`frontend/`, `frontend/AGENTS.md`: 이 Next.js는 학습 데이터와 다름, `node_modules/next/dist/docs/` 참고) |
| 운영자 | 한국 공포게임 유튜버 (채널 @ryujaeil), 연락처 contact@lurkpond.com |
| 스팀 큐레이터 | https://store.steampowered.com/curator/46592504/ |
| 공개 | 2026-10-14 색인 공개 (`SITE_INDEXING=true`), 구글 서치콘솔·네이버 서치어드바이저 등록됨 |

---

## 2. 운영자와의 작업 약속 (반드시 지킨다)

- **항상 한국어로 답한다.** 영어로 답했다가 불편해한 적이 있다. 쉬운 말로, 결론 먼저.
- **비밀값을 채팅에 붙여 달라고 하지 않는다.** 서버·Vercel 작업은 운영자의 로컬 Claude Code가 하고,
  그쪽에도 "키는 출력하지 마"라고 프롬프트에 넣는다.
- **Perplexity는 조사 전용**이다. 관리 화면·서버 권한을 주는 일을 시키지 않는다.
- **머지는 운영자에게 한 번 확인받고** 한다 ("다 끝나면 확인 한 번만 받고 머지"). 운영자가 "진행해줘"라고
  범위를 정해 준 작은 수정은 CI 통과 후 바로 머지해도 된다.
- **04:00–05:30 KST에는 머지·배포하지 않는다** (매일 수집 파이프라인 시간대).
- 커밋·PR에 모델 이름을 넣지 않는다. 커밋 끝에는 세션이 알려 주는 attribution 줄을 붙인다.
- 운영자는 로컬 Claude Code(Windows, `C:\pond\pondweb` 저장소 클론, `C:\whisper` 영상 도구)와
  Perplexity에 일을 맡기므로, 그쪽에 줄 **복사해서 붙여넣을 프롬프트**를 자주 요청한다.

---

## 3. 개발 절차

### 브랜치·PR·머지
1. 세션이 정해 준 작업 브랜치에서 개발한다.
2. PR 생성: GitHub MCP 도구, 또는 `gh api repos/onethousand121/horrorpond/pulls -f title=... -f head=<branch> -f base=main -f body=...`
   (이 환경은 `gh`의 GraphQL이 막혀 있어 `gh pr create`가 안 될 수 있다).
3. CI(`.github/workflows`): `backend`, `frontend`(tsc·lint·build·e2e), `deploy`(main에서만).
   - 머지: `gh api -X PUT repos/onethousand121/horrorpond/pulls/<N>/merge -f merge_method=merge`
4. 머지 뒤 작업 브랜치를 main으로 다시 맞춘다:
   `git fetch origin main && git checkout -B <branch> origin/main && git push -f origin <branch>`
5. main 배포 job 확인 후 실제 사이트/API를 curl로 확인하고 결과를 보고한다.

**CI 주의:** `npx playwright install --with-deps chromium` 단계가 GitHub 러너에서 가끔 20분 넘게 멈춘다.
테스트 시작 전 멈춘 것이므로 run을 취소하고 한 번 재실행한다.

### 로컬 테스트
- 백엔드는 Testcontainers(PostgreSQL 16)를 쓴다. 클라우드 컨테이너에서 Docker가 꺼져 있으면 아래 스크립트로 켠다.
  ```bash
  # dockerd 시작 + postgres:16 이미지 준비 (GCR 미러)
  docker info >/dev/null 2>&1 || {
    for p in $(pgrep -x containerd); do kill $p; done; sleep 1
    pkill -x dockerd; sleep 1
    rm -f /var/run/docker.sock /var/run/docker.pid /var/run/docker/containerd/containerd.pid /var/run/docker/containerd/*.sock
    (dockerd >/tmp/dockerd.log 2>&1 &)
    for i in $(seq 1 40); do docker info >/dev/null 2>&1 && break; sleep 1; done
  }
  docker image inspect postgres:16 >/dev/null 2>&1 || { docker pull -q mirror.gcr.io/library/postgres:16 && docker tag mirror.gcr.io/library/postgres:16 postgres:16; }
  ```
- Maven Central이 429(요청 과다)로 막히면 Gradle에 Google 미러를 쓰는 init 스크립트를 임시로 준다
  (`maven-central.storage-download.googleapis.com/maven2/`를 저장소 맨 앞에 추가, 저장소에는 커밋하지 않음).
- 개발 중에는 바뀐 영역만: `./gradlew test --tests 'com.horrorpond.curation.*'`. 작은 수정은 그대로 커밋하고 전체는 CI에 맡긴다
  (운영자가 머지 속도를 원함). 마이그레이션·큰 기능·수집 변경만 커밋 전 전체 `./gradlew test` (현재 299개).
- 새 Spring 테스트 컨텍스트(설정이 다른 `@SpringBootTest`)를 늘리면 Postgres 연결 수가 넘친다. 기존 테스트 클래스에 추가한다.
- 프론트 (`frontend/`):
  ```bash
  npm ci            # 처음 한 번
  API_BASE_URL=http://127.0.0.1:4010 npm run build   # 라우트 타입도 이때 생성됨 (tsc 전에 빌드)
  npx tsc --noEmit && npm run lint
  PW_CHROMIUM_PATH=/opt/pw-browsers/chromium CI=1 npm run test:e2e   # 현재 42개, mock API는 e2e/mock-api.mjs
  ```
  e2e가 부하로 한 개 시간 초과나면 그 파일만 다시 돌려 확인한다.
- 마이그레이션 최신은 **V25** (오늘의 게임 후보). 다음은 V26. 추가만, 적용된 파일 수정 금지.

### 비밀값·환경변수 (값은 운영자만 안다)
- 서버 `~/horrorpond/.env`: `POSTGRES_PASSWORD`, `ADMIN_API_KEY`, `API_DOMAIN`, `APP_MEM_LIMIT`, `DEEPL_API_KEY`,
  `RESEND_API_KEY`, `ALERT_EMAIL_TO`, `BACKUP_RCLONE_REMOTE` (자세히: `deploy/README.md`)
- GitHub Actions Secrets: 배포용 (`deploy/README.md` "Secrets" 절)
- Vercel: `API_BASE_URL`, `SITE_URL`, `SITE_INDEXING`, `GOOGLE_SITE_VERIFICATION`, `NAVER_SITE_VERIFICATION`

---

## 4. CLAUDE.md 이후 추가된 기능 (2026-10 하순)

| 기능 | 위치 |
|---|---|
| 큐레이터 글 영어 번역(선택). 영어 화면은 번역이 있을 때만 영어 글, 없으면 한국어 + "한국어로 쓴 글" 안내. 한국어를 고치면 `translationStale` | `CurationArticle.translate`, `PUT/DELETE /api/admin/games/{id}/article/translation`, 관리 화면 `TranslationForm` |
| 색인용 모음 페이지 5개 (`/collections/trending·upcoming·free·korean·picks`, korean은 한국어만) | `frontend/src/lib/collections.ts`, `app/[lang]/collections/[slug]` |
| 사이트맵 전용 API (공개·비성인 slug + 업적 여부 한 번에) | `GET /api/sitemap/games`, `frontend/src/app/sitemap.ts` (없으면 목록 API 페이징으로 대체) |
| 띄어쓰기·문장부호 무시 검색 ("redorblue" → Red or Blue) | `CuratedGameQueryRepository.searchMatches` (관리자 검색도 공유) |
| 오늘 출시 게임 카드는 "오늘 출시 예정"만 | `GameCard.tsx` |
| 코드 점검 수정: 수동 재요청 seed의 NOT_HORROR 초기화, 빈 SteamSpy 태그 7일 뒤 재조회·반영, 무료 필터를 "출시 예정" 표시 규칙과 통일, 성인 카드에 큐레이터 문구 숨김, itch 출시일 KST, 로케일 경로 슬래시 정리, "지금 뜨는" 반영 액션은 성공했을 때만 캐시 갱신 | PR #40 |
| 게임 대표 이미지 alt = 게임 이름 | PR #39 |
| 푸터에 Steam 큐레이터 링크 | PR #38 |

---

## 5. 정기 운영 (운영자가 하는 일, Claude는 돕는다)

- **월·목 09:00 "지금 뜨는 게임"**: Perplexity 예약 작업이 조사 → 운영자가 JSON을 `/admin/trending`에 붙여넣기.
  결과를 보여 주면 검토해 준다. Perplexity 지침의 핵심 규칙:
  최근 7일 영상만 / 하루 평균 조회수(조회수 ÷ 공개 후 일수) 정렬 / 복수 채널은 조회수 1만 이상 영상만 인정 /
  상위 5개 중 3개 이상 한국 채널 근거 / 해외 근거만 있는 게임은 하루 평균 5만 이상일 때만 상위 5 /
  지난번 게임은 새 영상이 없으면 제외 / 데모는 본편 스팀 주소로 포함 / 같은 게임에 한국 영상이 있으면 그걸 참고 영상으로.
- **고정 규칙**: 조사 반영으로 생긴 "자동 고정"은 다음 반영(결과 5개 이상) 때 풀린다. 고정 순서 화면에서 순서만 바꿔도
  자동 고정이다. 계속 두고 싶은 게임은 게임 관리 화면에서 "지금 뜨는 = 고정"으로 저장(직접 고정)한다. 운영자는
  "직접 고정은 내가 알아서 한다"고 했으므로 이 화면을 바꾸자고 다시 제안하지 않는다.
- **검색 성과 조사 (2주마다, 11/15부터)**: 운영자의 로컬 Claude Code가 서치콘솔·서치어드바이저를 읽어 보고서를 만들고
  "Claude에게 보낼 요약"을 가져온다. 받으면 다음 2주 할 일 3~5개(큐레이션 우선순위, 페이지 제목·설명 개선,
  모음 페이지 아이디어, 코드 수정)를 정하고, 지난 보고서와 비교한다. 프롬프트 원문은 작업 노트에 있다.
- **노출 규칙으로 생기는 404는 정상**: 자동 노출은 출시 10일 이내 또는 리뷰 10개 이상이라, 기간이 끝난 저리뷰 게임은
  404가 되고 사이트맵에서 빠진다. 서치콘솔의 "찾을 수 없음(404)", "발견됨/크롤링됨 - 색인 안 됨"은 새 사이트에서 정상.
  (대안 "페이지는 남기고 목록·검색에서만 빼기"는 제안만 했고 운영자가 고르지 않았다.)

---

## 6. 큐레이션·콘텐츠 흐름

- 녹화 → 로컬 `C:\whisper` 도구로 자막(Whisper) → 검수 에이전트 → 편집 지도 → 운영자 편집(다빈치 리졸브 무료)
  → 완성본 검사(+쇼츠 추천) → 업로드 → 큐레이션 초안(운영자의 "큐레이션 써주는" Claude 프로젝트) → 관리 화면 등록 → 스팀 큐레이터 짧은 평.
- 영어 번역은 Claude가 운영자의 말투를 살려 써 주고, 운영자 로컬 Claude Code가 관리자 API로 넣거나 운영자가 관리 화면에 붙인다.
- **협찬 표시**: 개발사에게 무료 키를 받은 게임은 lurkpond 글에서 "협찬·제공받은 콘텐츠"를 체크
  (문구 예: "개발사로부터 리뷰용 키를 제공받았습니다."), 영상 설명·스팀 평가에도 표시하도록 안내한다.
- 리뷰가 적은 게임은 자동 노출이 안 되므로, 글을 쓰고 관리 화면 **공개** 버튼으로 고정 노출해야 사이트에 뜬다.
- 개발사 컨택 메일 판단 기준: 한국어 지원 / 정식 출시 여부 / 공포 장르 적합성 / 엠바고 시간(KST로 환산해 알려 준다).

---

## 7. 알려진 한계·보류 항목

- 관리자가 itch.io 주소로 직접 추가한 성인 게임은 자동으로 성인 표시가 안 된다 (`Game.applyItchData`는 adult=false).
- 리뷰 수 출처가 두 가지(appdetails `recommendations` / appreviews `total_reviews`)라 갱신 날 값이 흔들릴 수 있음 — 미확인.
- 비어 있지 않은 SteamSpy 태그는 다시 받지 않는다 (빈 태그만 7일 뒤 재조회).
- 사이트맵 URL이 5만 개에 가까워지면 사이트맵 분할(`generateSitemaps`)이 필요하다 (현재 약 1.4만).
- 네이버 색인은 공개 직후 0이었다. 시간이 필요.
- Resend 알림 메일은 `onboarding@resend.dev` 발신이라 스팸함에 갈 수 있다 (도메인 인증은 선택 과제).
- AWS Lightsail 유료 플랜 전환: **2027-04-08 전**.

---

## 8. 다음에 할 일 (운영자 쪽 진행 상황 포함)

- [ ] 운영자 포트폴리오에 lurkpond 정리 (새 세션에서 시작 예정): 아키텍처, 수집 파이프라인, 노출 정책, blue/green 배포,
      SEO·다국어, 테스트 전략, AI 협업 방식 등을 저장소 기준으로 정리
- [ ] 11/15쯤 첫 검색 성과 보고서 받기 → 방향 정리
- [ ] lurkpond 큐레이션 10개 → 스팀 큐레이터 평가 10개(현재 5) → 큐레이터 커넥트 설정 도와주기
- [ ] 개발사 키로 받은 게임 2개(이름은 작업 노트) 플레이 후 글 공개·협찬 표시·개발사에 링크 보내기
- [ ] 완성본 검사의 화면 자막 오타 검사 개선(띄어쓰기 제외, 후보는 원본 해상도로 재확인) — 운영자 로컬에서 진행
- [ ] (선택) Resend 도메인 인증, 노출 끝난 게임 404 대신 "목록에서만 빼기" 방식 검토

---

## 9. 참고 링크 (비공개 아티팩트, 운영자 계정)

- lurkpond 작업 노트 (할 일·명령어·운영 절차·프롬프트): https://claude.ai/artifact/NGi45U6XRnHYgqH88LcTbe
- lurkpond 진행 보고서 (10/14 공개 시점 정리): https://claude.ai/artifact/71YccUCFbNNyY8qBUiuRM3
