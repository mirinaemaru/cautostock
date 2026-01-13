#!/bin/bash

###############################################################################
# Trading System - 헬스체크 스크립트
#
# 기능:
# - HTTP health check 엔드포인트 호출
# - 응답 시간 측정
# - 상태 코드 확인
#
# 사용법:
#   ./health-check.sh [포트]
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

# 포트 설정 (기본값: 8080)
PORT="${1:-8080}"
HEALTH_URL="http://localhost:$PORT/actuator/health"

echo "=== Trading System 헬스체크 ==="
echo ""
echo "URL: $HEALTH_URL"
echo ""

# curl 명령어 확인
if ! command -v curl >/dev/null 2>&1; then
    log_error "curl 명령어가 없습니다."
    exit 1
fi

# 헬스체크 실행
START_TIME=$(date +%s%N)
RESPONSE=$(curl -s -w "\n%{http_code}" "$HEALTH_URL" 2>/dev/null)
END_TIME=$(date +%s%N)

# 응답 시간 계산 (밀리초)
RESPONSE_TIME=$(( (END_TIME - START_TIME) / 1000000 ))

# 응답 파싱
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | head -n-1)

# 상태 코드 확인
if [ "$HTTP_CODE" = "200" ]; then
    log_info "상태: 정상 (HTTP $HTTP_CODE)"
    log_info "응답 시간: ${RESPONSE_TIME}ms"
    echo ""
    echo "응답 본문:"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    echo ""
    exit 0
elif [ "$HTTP_CODE" = "503" ]; then
    log_warn "상태: 점검 중 (HTTP $HTTP_CODE)"
    log_info "응답 시간: ${RESPONSE_TIME}ms"
    echo ""
    echo "응답 본문:"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    echo ""
    exit 1
elif [ -z "$HTTP_CODE" ]; then
    log_error "상태: 연결 실패"
    echo ""
    echo "애플리케이션이 실행 중이 아니거나 포트가 다를 수 있습니다."
    echo "  - 실행 상태 확인: ./scripts/status.sh"
    echo "  - 애플리케이션 시작: ./scripts/start.sh"
    echo ""
    exit 1
else
    log_error "상태: 오류 (HTTP $HTTP_CODE)"
    log_info "응답 시간: ${RESPONSE_TIME}ms"
    echo ""
    echo "응답 본문:"
    echo "$BODY"
    echo ""
    exit 1
fi
