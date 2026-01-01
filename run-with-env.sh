#!/bin/bash

# Trading System MVP - Run with environment variables
# 이 스크립트는 .env 파일과 환경 변수를 사용하여 애플리케이션을 실행합니다.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

echo "=========================================="
echo "Trading System MVP - Starting with ENV"
echo "=========================================="

# .env 파일 로드
if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from .env"
    set -a
    source "$ENV_FILE"
    set +a
else
    echo "Warning: .env file not found at $ENV_FILE"
fi

# Java 17 설정
export JAVA_HOME=/Users/changsupark/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home

# 데이터베이스 연결 정보 (환경 변수로 오버라이드)
export SPRING_DATASOURCE_URL="jdbc:mariadb://localhost:3306/trading_mvp?useUnicode=true&characterEncoding=utf8mb4"
export SPRING_DATASOURCE_USERNAME="nextman"
export SPRING_DATASOURCE_PASSWORD="***REMOVED***"

echo ""
echo "Environment Variables:"
echo "  JAVA_HOME: $JAVA_HOME"
echo "  DB URL: $SPRING_DATASOURCE_URL"
echo "  DB USER: $SPRING_DATASOURCE_USERNAME"
echo "  KIS_LIVE_APP_KEY: ${KIS_LIVE_APP_KEY:0:10}..."
echo "  KIS_LIVE_ACCOUNT_NO: $KIS_LIVE_ACCOUNT_NO"
echo ""
echo "Starting application..."
echo ""

cd "$SCRIPT_DIR"

# Maven으로 실행
mvn spring-boot:run
