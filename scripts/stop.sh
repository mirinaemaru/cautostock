#!/bin/bash

###############################################################################
# Trading System - 중지 스크립트
#
# 기능:
# - 애플리케이션 종료
# - Graceful shutdown (SIGTERM)
# - 강제 종료 옵션 (SIGKILL)
#
# 사용법:
#   ./stop.sh [--force]
###############################################################################

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 프로젝트 루트
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# 배포 디렉토리
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/trading-system}"
PID_FILE="$DEPLOY_DIR/trading-system.pid"

# PID 파일 확인
if [ ! -f "$PID_FILE" ]; then
    log_warn "PID 파일이 없습니다. 애플리케이션이 실행 중이지 않을 수 있습니다."
    exit 0
fi

# PID 읽기
PID=$(cat "$PID_FILE")

# 프로세스 확인
if ! kill -0 "$PID" 2>/dev/null; then
    log_warn "프로세스가 실행 중이지 않습니다 (PID: $PID)"
    rm -f "$PID_FILE"
    exit 0
fi

# 강제 종료 플래그 확인
FORCE=false
if [ "$1" = "--force" ] || [ "$1" = "-f" ]; then
    FORCE=true
fi

if [ "$FORCE" = "true" ]; then
    log_warn "강제 종료 모드 (SIGKILL)"
    kill -9 "$PID" || true
    log_info "애플리케이션이 강제 종료되었습니다 (PID: $PID)"
else
    log_info "애플리케이션 종료 중 (PID: $PID)..."

    # Graceful shutdown (SIGTERM)
    kill -TERM "$PID"

    # 종료 대기 (최대 30초)
    WAIT_TIME=0
    MAX_WAIT=30

    while kill -0 "$PID" 2>/dev/null; do
        if [ $WAIT_TIME -ge $MAX_WAIT ]; then
            log_warn "Graceful shutdown 실패. 강제 종료합니다..."
            kill -9 "$PID" || true
            break
        fi

        sleep 1
        WAIT_TIME=$((WAIT_TIME + 1))
        echo -n "."
    done

    echo ""
    log_info "애플리케이션이 종료되었습니다"
fi

# PID 파일 삭제
rm -f "$PID_FILE"

log_info "종료 완료"
