#!/bin/bash

###############################################################################
# Trading System - 배포 자동화 스크립트
#
# 기능:
# - 전체 빌드 및 배포 프로세스 자동화
# - 환경 변수 검증
# - JAR 파일 빌드
# - 애플리케이션 배포
#
# 사용법:
#   ./deploy.sh [환경]
#
# 예시:
#   ./deploy.sh local    # 로컬 배포
#   ./deploy.sh prod     # 프로덕션 배포
###############################################################################

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 프로젝트 루트 디렉토리
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# 환경 설정 (기본값: local)
ENV="${1:-local}"
log_info "배포 환경: $ENV"

# 환경 변수 파일 확인
ENV_FILE="$PROJECT_ROOT/.env.$ENV"
if [ ! -f "$ENV_FILE" ]; then
    log_error "환경 변수 파일이 없습니다: $ENV_FILE"
    log_info "다음 명령으로 생성하세요:"
    echo "cp .env.example .env.$ENV"
    exit 1
fi

# 환경 변수 로드
log_info "환경 변수 로드 중..."
set -a  # Export all variables
source "$ENV_FILE"
set +a

# 필수 환경 변수 검증
log_info "필수 환경 변수 검증 중..."

check_env_var() {
    local var_name=$1
    local var_value=${!var_name}

    if [ -z "$var_value" ]; then
        log_error "필수 환경 변수 누락: $var_name"
        return 1
    else
        log_info "  ✓ $var_name: 설정됨"
        return 0
    fi
}

# 필수 변수 체크
ENV_CHECK_FAILED=0

check_env_var "JAVA_HOME" || ENV_CHECK_FAILED=1
check_env_var "DB_PASSWORD" || ENV_CHECK_FAILED=1
check_env_var "KIS_PAPER_APP_KEY" || ENV_CHECK_FAILED=1
check_env_var "KIS_PAPER_APP_SECRET" || ENV_CHECK_FAILED=1

if [ $ENV_CHECK_FAILED -eq 1 ]; then
    log_error "환경 변수 검증 실패. 스크립트를 종료합니다."
    exit 1
fi

# Java 버전 확인
log_info "Java 버전 확인..."
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
log_info "Java 버전: $JAVA_VERSION"

if [[ ! "$JAVA_VERSION" =~ ^17\. ]] && [[ ! "$JAVA_VERSION" =~ ^1\.17\. ]]; then
    log_warn "Java 17이 권장됩니다. 현재 버전: $JAVA_VERSION"
fi

# 이전 빌드 정리
log_info "이전 빌드 정리 중..."
mvn clean -q || {
    log_error "빌드 정리 실패"
    exit 1
}

# 테스트 실행 여부 (기본값: true)
RUN_TESTS="${RUN_TESTS:-true}"

if [ "$RUN_TESTS" = "true" ]; then
    log_info "테스트 포함 빌드 시작..."
    mvn package -Dmaven.test.failure.ignore=false || {
        log_error "빌드 실패 (테스트 포함)"
        exit 1
    }
else
    log_warn "테스트 제외 빌드 시작..."
    mvn package -DskipTests || {
        log_error "빌드 실패 (테스트 제외)"
        exit 1
    }
fi

# JAR 파일 확인
JAR_FILE="$PROJECT_ROOT/target/trading-system-0.1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    log_error "JAR 파일이 생성되지 않았습니다: $JAR_FILE"
    exit 1
fi

log_info "JAR 파일 크기: $(du -h "$JAR_FILE" | cut -f1)"

# 배포 디렉토리 설정
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/trading-system}"
log_info "배포 디렉토리: $DEPLOY_DIR"

# 배포 디렉토리 생성
mkdir -p "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR/config"
mkdir -p "$DEPLOY_DIR/logs"
mkdir -p "$DEPLOY_DIR/backup"

# 기존 JAR 백업 (존재하는 경우)
if [ -f "$DEPLOY_DIR/trading-system.jar" ]; then
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    log_info "기존 JAR 파일 백업 중..."
    cp "$DEPLOY_DIR/trading-system.jar" "$DEPLOY_DIR/backup/trading-system_$TIMESTAMP.jar"
    log_info "백업 완료: trading-system_$TIMESTAMP.jar"
fi

# JAR 파일 복사
log_info "JAR 파일 배포 중..."
cp "$JAR_FILE" "$DEPLOY_DIR/trading-system.jar"

# 설정 파일 복사 (존재하는 경우)
if [ -f "$PROJECT_ROOT/src/main/resources/application-$ENV.yml" ]; then
    log_info "설정 파일 복사 중..."
    cp "$PROJECT_ROOT/src/main/resources/application-$ENV.yml" "$DEPLOY_DIR/config/"
fi

# 환경 변수 파일 복사
log_info "환경 변수 파일 복사 중..."
cp "$ENV_FILE" "$DEPLOY_DIR/config/env.conf"
chmod 600 "$DEPLOY_DIR/config/env.conf"

# 권한 설정
log_info "권한 설정 중..."
chmod 500 "$DEPLOY_DIR/trading-system.jar"
chmod 700 "$DEPLOY_DIR/logs"

# 애플리케이션 재시작 여부 확인
if [ "$AUTO_RESTART" = "true" ]; then
    log_info "애플리케이션 재시작 중..."

    # 중지
    if [ -f "$PROJECT_ROOT/scripts/stop.sh" ]; then
        "$PROJECT_ROOT/scripts/stop.sh"
        sleep 3
    fi

    # 시작
    if [ -f "$PROJECT_ROOT/scripts/start.sh" ]; then
        "$PROJECT_ROOT/scripts/start.sh" "$ENV"
    fi
else
    log_warn "AUTO_RESTART가 false로 설정되어 자동 재시작하지 않습니다."
    log_info "수동으로 재시작하려면: ./scripts/restart.sh"
fi

log_info "배포 완료!"
log_info ""
log_info "다음 명령어로 상태를 확인하세요:"
log_info "  ./scripts/status.sh"
log_info "  ./scripts/health-check.sh"
