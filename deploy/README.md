# 배포 가이드

```
브라우저 ──▶ Vercel (Next.js, 오사카 kix1)
                 │ 서버 컴포넌트에서만 호출
                 ▼
         https://API_DOMAIN ──▶ Caddy (HTTPS 자동) ──▶ app-blue / app-green (Spring Boot)
                                                         │
                                                         ▼
                                                      PostgreSQL 16
         └──────────────── 서버 1대 (Docker Compose) ────────────────┘
```

- **무중단 배포**: `deploy.sh`가 쉬고 있는 슬롯(blue/green)에 새 버전을 띄우고, readiness가 UP이 되면 기존 슬롯을 graceful shutdown 합니다. Caddy는 readiness가 UP인 슬롯으로만 보내고, 종료 중인 슬롯으로 간 요청은 다른 슬롯으로 재시도합니다. 새 버전이 뜨지 않으면 새 슬롯만 내리고 기존 버전이 계속 서비스합니다.
- **CI/CD**: `main`에 push → 백엔드 전체 테스트 + 프론트 lint/타입/빌드 → 통과하면 jar를 서버로 올려 `deploy.sh` 실행.
- **비공개 운영**: 프론트는 `SITE_INDEXING=true`가 아니면 모든 페이지가 noindex이고 robots.txt가 전체를 막습니다.

## 1. 서버 만들기

둘 중 하나. 이후 단계(2~5)는 같습니다. SSH 사용자는 둘 다 `ubuntu`입니다.

### 1-A. AWS Lightsail (권장, 월 $12)

1. [Lightsail 콘솔](https://lightsail.aws.amazon.com/) → **Create instance**
   - Region: **Seoul (ap-northeast-2)**
   - Platform **Linux/Unix** → Blueprint **OS Only** → **Ubuntu 24.04 LTS**
   - SSH key: 기본 키를 쓰면 **Download**로 `.pem` 파일을 받아 보관 (다시 받을 수 없음)
   - Plan: **메모리 2GB** (dual-stack, 월 $12 내외. 서울 리전 가격은 화면에서 확인)
   - 이름: `horrorpond` → Create
2. 인스턴스 → **Networking** 탭
   - **Attach static IP** → 새 고정 IP 만들어 연결 (인스턴스에 붙어 있는 동안 무료. 재시작해도 IP가 안 바뀜)
   - **IPv4 Firewall**에 규칙 추가: **HTTPS (TCP 443)**. SSH(22)와 HTTP(80)는 기본으로 열려 있음
3. 이 고정 IP가 아래 단계의 `<Public IP>`입니다.
4. 요금 알림: AWS 콘솔 → Billing → **Budgets**에서 월 $15 예산 알림을 만들어 두면 예상보다 많이 나올 때 메일이 옵니다.
5. `.env`(2단계)에 **`APP_MEM_LIMIT=640m`** 를 꼭 넣습니다. 메모리 2GB 서버에서 배포 중 두 버전이 잠깐 같이 뜨기 때문입니다. `setup-server.sh`가 스왑 2GB도 만들어 둡니다.

접속: `ssh -i <받은 .pem 파일> ubuntu@<Public IP>` (Windows는 Git Bash나 PowerShell에서 같은 명령. 처음 한 번 `chmod 400 <파일>.pem`)

### 1-B. Oracle Cloud Always Free (무료, 승인되면)

1. 콘솔 → Compute → Instances → **Create instance**
   - Image: **Canonical Ubuntu 24.04** (aarch64)
   - Shape: **VM.Standard.A1.Flex**, OCPU **2**, Memory **12GB** (Always Free 범위)
   - SSH 키: "Generate a key pair"로 만들고 **private key를 내려받아 보관**
   - "Out of host capacity" 오류가 나면 Pay-As-You-Go로 업그레이드 후 다시 시도(무료 범위 안에서는 과금 없음, 예산 알림 $1 설정)
2. 인스턴스의 **Public IP** 확인
3. Networking → 인스턴스의 VCN → Security List → **Ingress Rules 추가**
   - Source `0.0.0.0/0`, TCP, Destination port **80**
   - Source `0.0.0.0/0`, TCP, Destination port **443**

## 2. 서버 초기 설정 (1회)

```bash
# 내 PC에서
ssh -i <내려받은-private-key> ubuntu@<Public IP>

# 서버에서
mkdir -p ~/horrorpond
# 이 저장소의 deploy/setup-server.sh 를 서버로 복사해 실행 (scp 또는 내용 붙여넣기)
bash setup-server.sh
exit   # docker 그룹 적용을 위해 다시 접속
```

`~/horrorpond/.env` 작성 (`deploy/.env.example` 참고):

```bash
cat > ~/horrorpond/.env <<EOF
API_DOMAIN=<Public IP>.sslip.io
POSTGRES_PASSWORD=$(openssl rand -base64 32 | tr -d '/+=')
ADMIN_API_KEY=$(openssl rand -base64 32 | tr -d '/+=')
BACKUP_RCLONE_REMOTE=
# Lightsail 2GB용. Oracle(12GB)은 아래 줄 생략
APP_MEM_LIMIT=640m
EOF
chmod 600 ~/horrorpond/.env
cat ~/horrorpond/.env   # ADMIN_API_KEY 값은 관리자 로그인에 쓰니 비밀번호 관리자에 보관
```

`<IP>.sslip.io`는 별도 설정 없이 그 IP로 연결되는 무료 주소입니다. Caddy가 이 주소로 HTTPS 인증서를 자동 발급합니다. 도메인을 사면 `API_DOMAIN`만 바꾸면 됩니다.

## 3. 배포용 SSH 키와 GitHub 설정 (1회)

배포 전용 키를 따로 만듭니다 (서버 접속용 키와 분리).

```bash
# 내 PC에서
ssh-keygen -t ed25519 -N "" -f horrorpond_deploy -C "github-actions-deploy"
# 공개키를 서버에 등록 (Windows Git Bash에서도 동작)
cat horrorpond_deploy.pub | ssh -i <서버 접속용 private key> ubuntu@<Public IP> 'cat >> ~/.ssh/authorized_keys' 
ssh-keyscan -t ed25519 <Public IP>      # 출력 전체가 DEPLOY_KNOWN_HOSTS 값
```

GitHub 저장소 → Settings → Secrets and variables → Actions

| 종류 | 이름 | 값 |
|---|---|---|
| Secret | `DEPLOY_HOST` | 서버 Public IP |
| Secret | `DEPLOY_USER` | `ubuntu` |
| Secret | `DEPLOY_SSH_KEY` | `horrorpond_deploy` 파일 내용 전체 (private key) |
| Secret | `DEPLOY_KNOWN_HOSTS` | `ssh-keyscan` 출력 |
| Variable | `DEPLOY_ENABLED` | `true` |

이제 `main`에 push(또는 PR merge)하면 테스트 통과 후 자동 배포됩니다. 첫 배포는 Actions 탭에서 CI 워크플로를 **Re-run** 해도 됩니다.

확인:

```bash
curl https://<Public IP>.sslip.io/actuator/health     # {"status":"UP",...}
curl https://<Public IP>.sslip.io/api/genres
```

## 4. Vercel (프론트)

1. Vercel → Add New Project → 이 GitHub 저장소 Import
2. **Root Directory: `frontend`**
3. Environment Variables
   - `API_BASE_URL` = `https://<Public IP>.sslip.io`
   - `SITE_URL` = Vercel이 준 주소 (예: `https://horrorpond.vercel.app`)
   - `SITE_INDEXING` = `false` (오픈할 때 `true`로 바꾸고 Redeploy)
   - (선택) `GOOGLE_SITE_VERIFICATION`, `NAVER_SITE_VERIFICATION` = 서치콘솔·서치어드바이저의 HTML 태그 소유 확인 값 (content 부분만)
4. Deploy. 함수 리전은 `frontend/vercel.json`에서 오사카(`kix1`)로 고정되어 있습니다.
5. `https://<vercel 주소>/admin` 에서 `ADMIN_API_KEY`로 로그인

Hobby(무료) 플랜은 **비상업적 이용**만 허용됩니다. 광고·제휴 링크를 붙이는 시점에 Pro로 올립니다.

### 공개(검색 노출) 체크리스트

1. 도메인 연결: Vercel → Settings → Domains에 `lurkpond.com` 추가, `SITE_URL=https://lurkpond.com`
2. API 도메인: DNS에 `api.lurkpond.com` A 레코드 → 서버 IP, 서버 `.env`의 `API_DOMAIN=api.lurkpond.com` 후 `docker compose up -d caddy`, Vercel의 `API_BASE_URL=https://api.lurkpond.com`
3. 문의 메일: Cloudflare Email Routing으로 `contact@lurkpond.com` → 개인 메일함 전달 (사이트에 표시되는 주소는 `frontend/src/lib/site.ts`의 `CONTACT_EMAIL`)
4. `SITE_INDEXING=true` 후 Redeploy
5. [구글 서치콘솔](https://search.google.com/search-console)과 [네이버 서치어드바이저](https://searchadvisor.naver.com/)에 사이트 등록 → 소유 확인 값을 Vercel 환경변수에 넣고 Redeploy → 두 곳에 `https://lurkpond.com/sitemap.xml` 제출

## 5. 첫 수집

```bash
curl -X POST https://<Public IP>.sslip.io/api/admin/ingestion/run \
  -H "X-Admin-Key: <ADMIN_API_KEY>" -H "Content-Type: application/json" \
  -d '{"steps":["DISCOVERY","ENRICHMENT","NORMALIZE","METRICS"]}'

curl https://<Public IP>.sslip.io/api/admin/ingestion/jobs -H "X-Admin-Key: <ADMIN_API_KEY>"
```

한 번에 최대 1200건을 처리하고(1시간 이상), 이후 매일 04:00(KST)에 자동으로 이어서 수집합니다.
마지막 단계 `METRICS`는 최근 30일 출시작과 리뷰 10개 이상인 게임의 리뷰 수를 하루 최대 400개 기록합니다(약 10분, 트렌드 계산용). 클라우드 IP에서 Steam/SteamSpy 호출이 막히는지 이때 job 결과(`failedCount`, `errorMessage`)로 확인합니다.

## 운영

| 작업 | 명령 (서버의 `~/horrorpond`) |
|---|---|
| 상태 | `docker compose --profile app ps` |
| 로그 | `docker compose --profile app logs -f --tail 100 app-blue` (또는 `app-green`, `cat .deploy-state`로 활성 슬롯 확인) |
| 직전 버전으로 되돌리기 (무중단) | `./rollback.sh` |
| 수동 백업 | `./backup.sh` (매일 03:30 자동, 7일 보관) |
| 백업 복원 | `docker compose exec -T postgres pg_restore -U horrorpond -d horrorpond --clean --if-exists < backups/<파일>.dump` |
| DB 접속 | `docker compose exec postgres psql -U horrorpond` |

**서버 밖 백업 (권장)**: 서버가 사라지면 서버 안의 백업도 같이 사라집니다. [rclone](https://rclone.org/)으로 Google Drive 등을 연결하고 `.env`의 `BACKUP_RCLONE_REMOTE`(예: `gdrive:horrorpond-backup`)를 채우면 매일 백업이 서버 밖으로도 복사됩니다.

**다른 서버로 옮기기**: 새 서버에서 1~3단계를 하고, 백업 파일을 복원한 뒤 GitHub Secrets의 주소만 바꾸면 됩니다. Railway 같은 플랫폼으로 옮길 때는 `backend/Dockerfile`(기본 타깃이 소스에서 빌드)을 그대로 쓸 수 있습니다.

## 로컬에서 배포 흐름 확인

```bash
cd backend && ./gradlew bootJar && cd ..
mkdir -p /tmp/hp/releases/v1/build/libs
cp deploy/{compose.yaml,Caddyfile,deploy.sh,rollback.sh,backup.sh} /tmp/hp/
cp backend/Dockerfile /tmp/hp/releases/v1/ && cp backend/build/libs/app.jar /tmp/hp/releases/v1/build/libs/
printf 'API_DOMAIN=:80\nPOSTGRES_PASSWORD=local\nADMIN_API_KEY=local\n' > /tmp/hp/.env
cd /tmp/hp && ./deploy.sh releases/v1 v1 && curl localhost/api/genres
```
