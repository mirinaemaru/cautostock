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

# Java 17 설정 (.env에서 JAVA_HOME 설정 가능)
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"

# 데이터베이스 연결 정보 (.env에서 로드)
export SPRING_DATASOURCE_URL="${DB_URL:-jdbc:mariadb://localhost:3306/trading_mvp?useUnicode=true&characterEncoding=utf8mb4}"
export SPRING_DATASOURCE_USERNAME="${DB_USER:-trading_user}"
export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD:?DB_PASSWORD must be set in .env file}"
# application.yml에서 사용하는 변수
export DB_USERNAME="${DB_USER:-trading_user}"

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

# Maven으로 실행 (테스트 건너뛰기)
mvn spring-boot:run -Dmaven.test.skip=true
