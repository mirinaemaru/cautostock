#!/bin/bash

###############################################################################
# Trading System - 시작 스크립트
#
# 기능:
# - 애플리케이션 시작
# - PID 파일 생성
# - 로그 파일 생성
#
# 사용법:
#   ./start.sh [환경]
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

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 프로젝트 루트
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# 환경 설정
ENV="${1:-local}"
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/trading-system}"
PID_FILE="$DEPLOY_DIR/trading-system.pid"
LOG_FILE="$DEPLOY_DIR/logs/application.log"

# JAR 파일 확인
JAR_FILE="$DEPLOY_DIR/trading-system.jar"
if [ ! -f "$JAR_FILE" ]; then
    log_error "JAR 파일이 없습니다: $JAR_FILE"
    log_info "먼저 배포를 실행하세요: ./scripts/deploy.sh"
    exit 1
fi

# 이미 실행 중인지 확인
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
        log_error "애플리케이션이 이미 실행 중입니다 (PID: $PID)"
        exit 1
    else
        log_info "PID 파일이 존재하지만 프로세스가 없습니다. PID 파일 삭제 중..."
        rm -f "$PID_FILE"
    fi
fi

# 환경 변수 로드
ENV_FILE="$DEPLOY_DIR/config/env.conf"
if [ -f "$ENV_FILE" ]; then
    log_info "환경 변수 로드 중..."
    set -a
    source "$ENV_FILE"
    set +a
else
    log_error "환경 변수 파일이 없습니다: $ENV_FILE"
    exit 1
fi

# JVM 옵션 설정
JVM_OPTS="${JVM_OPTS:--Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200}"

# Spring Profile 설정
SPRING_PROFILE="${SPRING_PROFILE:-$ENV}"

log_info "애플리케이션 시작 중..."
log_info "  환경: $ENV"
log_info "  JVM 옵션: $JVM_OPTS"
log_info "  로그 파일: $LOG_FILE"

# 애플리케이션 시작
nohup "$JAVA_HOME/bin/java" $JVM_OPTS \
    -jar "$JAR_FILE" \
    --spring.profiles.active="$SPRING_PROFILE" \
    > "$LOG_FILE" 2>&1 &

# PID 저장
echo $! > "$PID_FILE"

log_info "애플리케이션이 시작되었습니다 (PID: $(cat "$PID_FILE"))"
log_info ""
log_info "로그 확인: tail -f $LOG_FILE"
log_info "상태 확인: ./scripts/status.sh"
log_info "중지: ./scripts/stop.sh"
