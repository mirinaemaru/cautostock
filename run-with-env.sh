#!/bin/bash

# Trading System MVP - Run with environment variables
# 이 스크립트는 .env 파일과 환경 변수를 사용하여 애플리케이션을 실행합니다.
# 8099 포트에서 실행 중인 프로세스가 있으면 종료 후 재시작합니다.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
APP_PORT=8099

echo "=========================================="
echo "Trading System MVP - Restart with ENV"
echo "=========================================="

# 8099 포트에서 실행 중인 프로세스 종료
echo ""
echo "1. 기존 프로세스 확인 중 (포트: $APP_PORT)..."
EXISTING_PID=$(lsof -ti:$APP_PORT 2>/dev/null)

if [ -n "$EXISTING_PID" ]; then
    echo "   기존 프로세스 발견: PID=$EXISTING_PID"
    echo "   프로세스 종료 중..."
    kill $EXISTING_PID 2>/dev/null

    # 프로세스가 완전히 종료될 때까지 대기 (최대 10초)
    WAIT_COUNT=0
    while [ $WAIT_COUNT -lt 10 ]; do
        if ! kill -0 $EXISTING_PID 2>/dev/null; then
            echo "   프로세스 종료 완료"
            break
        fi
        sleep 1
        WAIT_COUNT=$((WAIT_COUNT + 1))
    done

    # 10초 후에도 종료되지 않으면 강제 종료
    if kill -0 $EXISTING_PID 2>/dev/null; then
        echo "   강제 종료 중 (SIGKILL)..."
        kill -9 $EXISTING_PID 2>/dev/null
        sleep 1
    fi
else
    echo "   실행 중인 프로세스 없음"
fi

echo ""
echo "2. 환경 변수 로드 중..."

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
export SPRING_DATASOURCE_USERNAME="${DB_USERNAME:-trading_user}"
export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD:?DB_PASSWORD must be set in .env file}"

echo ""
echo "   Environment Variables:"
echo "     JAVA_HOME: $JAVA_HOME"
echo "     DB URL: $SPRING_DATASOURCE_URL"
echo "     DB USER: $SPRING_DATASOURCE_USERNAME"
echo "     KIS_LIVE_APP_KEY: ${KIS_LIVE_APP_KEY:0:10}..."
echo "     KIS_LIVE_ACCOUNT_NO: $KIS_LIVE_ACCOUNT_NO"

echo ""
echo "3. 애플리케이션 시작 중 (포트: $APP_PORT)..."
echo ""

cd "$SCRIPT_DIR"

# Maven으로 실행 (테스트 건너뛰기)
mvn spring-boot:run -Dmaven.test.skip=true
