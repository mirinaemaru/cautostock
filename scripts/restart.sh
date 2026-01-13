#!/bin/bash

###############################################################################
# Trading System - 재시작 스크립트
#
# 기능:
# - 애플리케이션 재시작
#
# 사용법:
#   ./restart.sh [환경]
###############################################################################

set -e

# 프로젝트 루트
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# 환경 설정
ENV="${1:-local}"

echo "=== Trading System 재시작 ==="
echo ""

# 중지
echo "1. 애플리케이션 중지 중..."
"$PROJECT_ROOT/scripts/stop.sh"
echo ""

# 3초 대기
echo "2. 대기 중 (3초)..."
sleep 3
echo ""

# 시작
echo "3. 애플리케이션 시작 중..."
"$PROJECT_ROOT/scripts/start.sh" "$ENV"
echo ""

echo "=== 재시작 완료 ==="
