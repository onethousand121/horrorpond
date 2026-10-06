#!/usr/bin/env bash
# 서버 최초 1회 설정. Ubuntu 22.04/24.04 (Oracle Cloud Ampere A1 등) 기준.
# sudo 권한이 있는 사용자(Oracle Ubuntu 이미지의 기본 사용자 ubuntu)로 실행한다. 여러 번 실행해도 안전하다.
set -euo pipefail
APP_DIR="$HOME/horrorpond"

echo "== 1. 시간대 (cron, 로그를 한국 시간으로)"
sudo timedatectl set-timezone Asia/Seoul

echo "== 2. Docker"
if ! command -v docker > /dev/null; then
  curl -fsSL https://get.docker.com | sudo sh
fi
sudo usermod -aG docker "$USER"

echo "== 3. 보안 업데이트 자동 적용"
sudo apt-get update -qq
sudo apt-get install -y -qq unattended-upgrades iptables-persistent
sudo dpkg-reconfigure -f noninteractive unattended-upgrades

echo "== 4. 방화벽: 80/443 허용 (Oracle Ubuntu 이미지는 SSH 외 포트를 iptables로 막아 둔다)"
allow() {
  sudo iptables -C INPUT -p "$1" --dport "$2" -m state --state NEW -j ACCEPT 2> /dev/null \
    || sudo iptables -I INPUT 5 -p "$1" --dport "$2" -m state --state NEW -j ACCEPT
}
allow tcp 80
allow tcp 443
allow udp 443
sudo netfilter-persistent save > /dev/null

echo "== 5. 앱 디렉터리, 매일 03:30 DB 백업"
mkdir -p "$APP_DIR/releases"
(crontab -l 2> /dev/null | grep -v 'horrorpond/backup.sh' || true; \
  echo "30 3 * * * $APP_DIR/backup.sh >> $APP_DIR/backup.log 2>&1") | crontab -

cat <<MSG

완료. 다음 단계:
  1) 로그아웃 후 다시 SSH 접속 (docker 그룹 적용)
  2) $APP_DIR/.env 작성 (deploy/.env.example 참고)
  3) GitHub Actions로 첫 배포 (deploy/README.md 참고)
MSG
