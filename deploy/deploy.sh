#!/usr/bin/env bash
# 무중단(blue/green) 배포.
#   ./deploy.sh <release-dir> <tag>
#   release-dir: Dockerfile 과 build/libs/app.jar 가 있는 디렉터리 (CI가 업로드)
#   같은 태그의 이미지가 이미 있으면 빌드를 건너뛴다 (rollback.sh가 이용)
#
# 1) prebuilt 이미지를 만든다
# 2) 쉬고 있는 슬롯(blue/green)에 새 버전을 띄우고 readiness UP을 기다린다 (실패하면 새 슬롯만 내리고 종료: 기존 버전은 그대로 서비스)
# 3) Caddy가 새 슬롯을 healthy로 볼 때까지 잠깐 기다린 뒤, 기존 슬롯을 graceful shutdown 한다
set -euo pipefail
cd "$(dirname "$0")"

RELEASE_DIR=${1:?usage: deploy.sh <release-dir> <tag>}
TAG=${2:?usage: deploy.sh <release-dir> <tag>}
READY_TIMEOUT=${READY_TIMEOUT:-180}
STATE=.deploy-state

ACTIVE=none BLUE_TAG=none GREEN_TAG=none
[ -f "$STATE" ] && source "$STATE"
export BLUE_TAG GREEN_TAG

compose() { docker compose --profile app "$@"; }
log() { echo "[deploy $(date '+%H:%M:%S')] $*"; }

if docker image inspect "horrorpond-backend:$TAG" > /dev/null 2>&1; then
  log "image horrorpond-backend:$TAG already exists, skip build"
else
  log "build image horrorpond-backend:$TAG"
  docker build --quiet --target prebuilt -t "horrorpond-backend:$TAG" -f "$RELEASE_DIR/Dockerfile" "$RELEASE_DIR" > /dev/null
fi

# compose.yaml/Caddyfile 변경 반영 (변경이 없으면 아무 일도 하지 않는다. Caddy reload는 무중단)
docker compose up -d --wait postgres caddy
docker compose exec -T caddy caddy reload --config /etc/caddy/Caddyfile --force > /dev/null

if [ "$ACTIVE" = blue ]; then NEXT=green; else NEXT=blue; fi
if [ "$NEXT" = blue ]; then BLUE_TAG=$TAG; else GREEN_TAG=$TAG; fi
export BLUE_TAG GREEN_TAG

log "start app-$NEXT ($TAG), active=$ACTIVE"
compose up -d --no-deps --force-recreate "app-$NEXT"

log "wait for readiness (max ${READY_TIMEOUT}s)"
ready=false
for _ in $(seq 1 "$READY_TIMEOUT"); do
  if docker compose exec -T caddy wget -qO- "http://app-$NEXT:8080/actuator/health/readiness" 2>/dev/null | grep -q '"UP"'; then
    ready=true; break
  fi
  sleep 1
done

if [ "$ready" != true ]; then
  log "app-$NEXT did not become ready. Recent logs:"
  compose logs --tail 80 "app-$NEXT" || true
  compose stop "app-$NEXT" > /dev/null; compose rm -f "app-$NEXT" > /dev/null
  log "rolled back: app-$ACTIVE keeps serving"
  exit 1
fi

# Caddy 능동 상태확인(2s 간격)이 새 슬롯을 healthy로 표시할 시간
sleep 5

if [ "$ACTIVE" != none ]; then
  log "stop app-$ACTIVE (graceful)"
  compose stop "app-$ACTIVE" > /dev/null
  compose rm -f "app-$ACTIVE" > /dev/null
fi

cat > "$STATE" <<STATE_EOF
ACTIVE=$NEXT
BLUE_TAG=$BLUE_TAG
GREEN_TAG=$GREEN_TAG
STATE_EOF
log "done: active=app-$NEXT ($TAG)"

# 정리: 현재/직전 버전 이미지와 최근 릴리스 3개만 남긴다 (직전 버전은 rollback.sh 용)
docker images horrorpond-backend --format '{{.Tag}}' | grep -vxF -e "$BLUE_TAG" -e "$GREEN_TAG" \
  | xargs -r -I{} docker rmi "horrorpond-backend:{}" > /dev/null 2>&1 || true
if [ -d releases ]; then
  ls -1dt releases/*/ 2>/dev/null | tail -n +4 | xargs -r rm -rf
fi
