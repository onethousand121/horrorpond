#!/usr/bin/env bash
# 직전 버전으로 무중단으로 되돌린다. 직전 버전 이미지는 deploy.sh가 쉬는 슬롯의 태그로 남겨 둔다.
set -euo pipefail
cd "$(dirname "$0")"
source .deploy-state
if [ "$ACTIVE" = blue ]; then PREV_TAG=$GREEN_TAG; else PREV_TAG=$BLUE_TAG; fi
if [ "$PREV_TAG" = none ] || ! docker image inspect "horrorpond-backend:$PREV_TAG" > /dev/null 2>&1; then
  echo "no previous version to roll back to" >&2
  exit 1
fi
exec ./deploy.sh - "$PREV_TAG"
