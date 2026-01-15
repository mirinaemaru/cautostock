#!/bin/bash

# Trading System 실행 스크립트
# 사용법: ./scripts/trading.sh {start|stop|restart|status|log}

APP_NAME="trading-system"
APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR_FILE="$APP_DIR/build/libs/trading-system-*-SNAPSHOT.jar"
PID_FILE="$APP_DIR/trading.pid"
LOG_FILE="/var/logs/trading/trading-system.log"

# 환경변수 파일 로드 (있으면)
if [ -f "$APP_DIR/.env" ]; then
    while IFS='=' read -r key value; do
        # 주석과 빈줄 건너뛰기
        [[ "$key" =~ ^#.*$ ]] && continue
        [[ -z "$key" ]] && continue
        # 따옴표 제거
        value="${value%\"}"
        value="${value#\"}"
        export "$key=$value"
    done < "$APP_DIR/.env"
fi

# Java 17 설정 (.env 로드 후)
export JAVA_HOME="${JAVA_HOME:-/Users/changsupark/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home}"
JAVA_CMD="$JAVA_HOME/bin/java"

# JAR 파일 찾기 (plain.jar 제외)
find_jar() {
    local jar=$(ls $JAR_FILE 2>/dev/null | grep -v plain | head -1)
    if [ -z "$jar" ]; then
        echo "JAR 파일을 찾을 수 없습니다. 먼저 빌드하세요: ./gradlew bootJar"
        exit 1
    fi
    echo "$jar"
}

# PID 확인
get_pid() {
    if [ -f "$PID_FILE" ]; then
        cat "$PID_FILE"
    fi
}

# 프로세스 실행 여부 확인
is_running() {
    local pid=$(get_pid)
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
        return 0
    fi
    return 1
}

# 시작
start() {
    if is_running; then
        echo "$APP_NAME 이미 실행 중입니다. (PID: $(get_pid))"
        exit 1
    fi

    local jar=$(find_jar)
    echo "$APP_NAME 시작 중..."

    # 로그 디렉토리 확인
    local log_dir=$(dirname "$LOG_FILE")
    if [ ! -d "$log_dir" ]; then
        echo "로그 디렉토리 생성: $log_dir"
        sudo mkdir -p "$log_dir"
        sudo chown $(whoami) "$log_dir"
    fi

    # 백그라운드 실행
    nohup "$JAVA_CMD" -jar "$jar" \
        --spring.profiles.active=${SPRING_PROFILE:-local} \
        > /dev/null 2>&1 &

    local pid=$!
    echo $pid > "$PID_FILE"

    sleep 2
    if is_running; then
        echo "$APP_NAME 시작됨 (PID: $pid)"
        echo "로그 확인: tail -f $LOG_FILE"
    else
        echo "$APP_NAME 시작 실패. 로그를 확인하세요."
        rm -f "$PID_FILE"
        exit 1
    fi
}

# 중지
stop() {
    if ! is_running; then
        echo "$APP_NAME 실행 중이 아닙니다."
        rm -f "$PID_FILE"
        exit 0
    fi

    local pid=$(get_pid)
    echo "$APP_NAME 중지 중... (PID: $pid)"

    # Graceful shutdown
    kill "$pid"

    local count=0
    while is_running && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
        echo -n "."
    done
    echo ""

    if is_running; then
        echo "강제 종료 중..."
        kill -9 "$pid"
    fi

    rm -f "$PID_FILE"
    echo "$APP_NAME 중지됨"
}

# 재시작
restart() {
    stop
    sleep 2
    start
}

# 상태
status() {
    if is_running; then
        echo "$APP_NAME 실행 중 (PID: $(get_pid))"
        # 헬스체크
        local health=$(curl -s http://localhost:8099/actuator/health 2>/dev/null | grep -o '"status":"[^"]*"' | head -1)
        if [ -n "$health" ]; then
            echo "Health: $health"
        fi
    else
        echo "$APP_NAME 중지됨"
    fi
}

# 로그 보기
log() {
    if [ -f "$LOG_FILE" ]; then
        tail -f "$LOG_FILE"
    else
        echo "로그 파일이 없습니다: $LOG_FILE"
    fi
}

# 빌드
build() {
    echo "빌드 중..."
    cd "$APP_DIR"
    ./gradlew bootJar
    echo "빌드 완료"
}

# 메인
case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    log)
        log
        ;;
    build)
        build
        ;;
    *)
        echo "사용법: $0 {start|stop|restart|status|log|build}"
        echo ""
        echo "  start   - 애플리케이션 시작"
        echo "  stop    - 애플리케이션 중지"
        echo "  restart - 애플리케이션 재시작"
        echo "  status  - 실행 상태 확인"
        echo "  log     - 로그 실시간 확인"
        echo "  build   - JAR 빌드"
        exit 1
        ;;
esac

exit 0
