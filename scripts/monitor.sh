#!/bin/bash

###############################################################################
# Trading System - 통합 모니터링 스크립트
#
# 기능:
# - 애플리케이션 상태 모니터링
# - 로그 에러 검사
# - 메모리/CPU 사용량 확인
# - 헬스체크
# - Kill Switch 상태 확인
#
# 사용법:
#   ./monitor.sh [--continuous] [--interval SECONDS]
#
# 옵션:
#   --continuous        : 지속적으로 모니터링 (Ctrl+C로 종료)
#   --interval SECONDS  : 모니터링 간격 (기본값: 60초)
###############################################################################

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

log_section() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

# 프로젝트 루트
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# 옵션 파싱
CONTINUOUS=false
INTERVAL=60

while [[ $# -gt 0 ]]; do
    case $1 in
        --continuous)
            CONTINUOUS=true
            shift
            ;;
        --interval)
            INTERVAL="$2"
            shift 2
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            exit 1
            ;;
    esac
done

# 배포 디렉토리
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/trading-system}"
PID_FILE="$DEPLOY_DIR/trading-system.pid"
LOG_FILE="$DEPLOY_DIR/logs/application.log"

# 모니터링 함수
run_monitoring() {
    clear
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    log_section "Trading System 모니터링 $(date '+%Y-%m-%d %H:%M:%S')"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # 1. 프로세스 상태 확인
    log_section "1. 프로세스 상태"
    if [ ! -f "$PID_FILE" ]; then
        log_error "PID 파일 없음 - 애플리케이션이 실행 중이지 않습니다"
        return 1
    fi

    PID=$(cat "$PID_FILE")
    if ! kill -0 "$PID" 2>/dev/null; then
        log_error "프로세스 없음 (PID: $PID)"
        return 1
    fi

    log_info "실행 중 (PID: $PID)"

    # 실행 시간
    if command -v ps >/dev/null 2>&1; then
        ELAPSED=$(ps -o etime= -p "$PID" 2>/dev/null | tr -d ' ')
        echo "  실행 시간: $ELAPSED"
    fi
    echo ""

    # 2. 리소스 사용량
    log_section "2. 리소스 사용량"
    if command -v ps >/dev/null 2>&1; then
        # 메모리
        MEM=$(ps -o rss= -p "$PID" 2>/dev/null | tr -d ' ')
        if [ -n "$MEM" ]; then
            MEM_MB=$((MEM / 1024))
            echo "  메모리: ${MEM_MB} MB"
        fi

        # CPU
        CPU=$(ps -o %cpu= -p "$PID" 2>/dev/null | tr -d ' ')
        if [ -n "$CPU" ]; then
            echo "  CPU: ${CPU}%"
        fi
    fi
    echo ""

    # 3. 헬스체크
    log_section "3. 헬스체크"
    HEALTH_URL="http://localhost:8080/actuator/health"
    RESPONSE=$(curl -s -w "\n%{http_code}" "$HEALTH_URL" 2>/dev/null)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

    if [ "$HTTP_CODE" = "200" ]; then
        log_info "상태: 정상 (HTTP $HTTP_CODE)"
    elif [ "$HTTP_CODE" = "503" ]; then
        log_warn "상태: 점검 중 (HTTP $HTTP_CODE)"
    elif [ -z "$HTTP_CODE" ]; then
        log_error "상태: 연결 실패"
    else
        log_error "상태: 오류 (HTTP $HTTP_CODE)"
    fi
    echo ""

    # 4. 최근 로그 에러 검사
    log_section "4. 최근 로그 에러 (최근 100줄)"
    if [ -f "$LOG_FILE" ]; then
        ERROR_COUNT=$(tail -n 100 "$LOG_FILE" 2>/dev/null | grep -c "ERROR" || echo 0)
        WARN_COUNT=$(tail -n 100 "$LOG_FILE" 2>/dev/null | grep -c "WARN" || echo 0)

        if [ "$ERROR_COUNT" -gt 0 ]; then
            log_error "ERROR 로그: $ERROR_COUNT 건"
            echo "  최근 ERROR 로그:"
            tail -n 100 "$LOG_FILE" 2>/dev/null | grep "ERROR" | tail -n 5 | sed 's/^/    /'
        else
            log_info "ERROR 로그: 없음"
        fi

        if [ "$WARN_COUNT" -gt 0 ]; then
            log_warn "WARN 로그: $WARN_COUNT 건"
        else
            log_info "WARN 로그: 없음"
        fi
    else
        log_warn "로그 파일 없음: $LOG_FILE"
    fi
    echo ""

    # 5. Kill Switch 상태 확인 (API 호출)
    log_section "5. Kill Switch 상태"
    KILL_SWITCH_URL="http://localhost:8080/api/v1/admin/kill-switch"
    KS_RESPONSE=$(curl -s "$KILL_SWITCH_URL" 2>/dev/null)

    if [ -n "$KS_RESPONSE" ]; then
        KS_ENABLED=$(echo "$KS_RESPONSE" | grep -o '"enabled":[^,}]*' | cut -d: -f2 | tr -d ' ')
        if [ "$KS_ENABLED" = "true" ]; then
            log_error "Kill Switch: 활성화됨 (자동 거래 차단)"
        elif [ "$KS_ENABLED" = "false" ]; then
            log_info "Kill Switch: 비활성화됨 (정상 거래)"
        else
            log_warn "Kill Switch: 상태 확인 불가"
        fi
    else
        log_warn "Kill Switch: API 응답 없음"
    fi
    echo ""

    # 6. 디스크 사용량
    log_section "6. 디스크 사용량"
    if [ -d "$DEPLOY_DIR" ]; then
        DISK_USAGE=$(du -sh "$DEPLOY_DIR" 2>/dev/null | cut -f1)
        echo "  배포 디렉토리: $DISK_USAGE"

        if [ -d "$DEPLOY_DIR/logs" ]; then
            LOG_USAGE=$(du -sh "$DEPLOY_DIR/logs" 2>/dev/null | cut -f1)
            echo "  로그 디렉토리: $LOG_USAGE"
        fi
    fi
    echo ""

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# 메인 로직
if [ "$CONTINUOUS" = "true" ]; then
    log_info "지속적 모니터링 모드 (간격: ${INTERVAL}초, Ctrl+C로 종료)"
    echo ""

    while true; do
        run_monitoring
        sleep "$INTERVAL"
    done
else
    run_monitoring
fi
