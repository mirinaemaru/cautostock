#!/bin/bash

###############################################################################
# Trading System - 상태 확인 스크립트
#
# 기능:
# - 애플리케이션 실행 상태 확인
# - 프로세스 정보 표시
# - 메모리 사용량 표시
#
# 사용법:
#   ./status.sh
###############################################################################

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

echo "=== Trading System 상태 ==="
echo ""

# PID 파일 확인
if [ ! -f "$PID_FILE" ]; then
    log_error "상태: 중지됨 (PID 파일 없음)"
    echo ""
    echo "시작하려면: ./scripts/start.sh"
    exit 1
fi

# PID 읽기
PID=$(cat "$PID_FILE")

# 프로세스 확인
if ! kill -0 "$PID" 2>/dev/null; then
    log_error "상태: 중지됨 (프로세스 없음, PID: $PID)"
    echo ""
    echo "PID 파일은 존재하지만 프로세스가 실행 중이지 않습니다."
    echo "정리하려면: rm $PID_FILE"
    echo "시작하려면: ./scripts/start.sh"
    exit 1
fi

# 실행 중
log_info "상태: 실행 중"
echo ""

# 프로세스 정보
echo "프로세스 정보:"
echo "  PID: $PID"

# 실행 시간 확인
if command -v ps >/dev/null 2>&1; then
    ELAPSED=$(ps -o etime= -p "$PID" 2>/dev/null | tr -d ' ')
    echo "  실행 시간: $ELAPSED"
fi

# 메모리 사용량 확인
if command -v ps >/dev/null 2>&1; then
    MEM=$(ps -o rss= -p "$PID" 2>/dev/null | tr -d ' ')
    if [ -n "$MEM" ]; then
        MEM_MB=$((MEM / 1024))
        echo "  메모리 사용량: ${MEM_MB} MB"
    fi
fi

# 포트 확인
echo ""
echo "포트 정보:"
if command -v lsof >/dev/null 2>&1; then
    PORTS=$(lsof -Pan -p "$PID" -i 2>/dev/null | grep LISTEN | awk '{print $9}' | cut -d: -f2 | sort -u)
    if [ -n "$PORTS" ]; then
        for PORT in $PORTS; do
            echo "  - $PORT (LISTEN)"
        done
    else
        echo "  (포트 정보 없음)"
    fi
else
    echo "  (lsof 명령어 없음)"
fi

# 로그 파일 정보
echo ""
echo "로그 파일:"
LOG_FILE="$DEPLOY_DIR/logs/application.log"
if [ -f "$LOG_FILE" ]; then
    LOG_SIZE=$(du -h "$LOG_FILE" 2>/dev/null | cut -f1)
    LOG_LINES=$(wc -l < "$LOG_FILE" 2>/dev/null | tr -d ' ')
    echo "  위치: $LOG_FILE"
    echo "  크기: $LOG_SIZE"
    echo "  라인 수: $LOG_LINES"
    echo ""
    echo "최근 로그 (10줄):"
    tail -n 10 "$LOG_FILE" 2>/dev/null || echo "  (로그 읽기 실패)"
else
    echo "  (로그 파일 없음)"
fi

echo ""
echo "=== 상태 확인 완료 ==="
