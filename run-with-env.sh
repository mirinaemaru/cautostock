#!/bin/bash

# Trading System MVP - Run with environment variables
# 이 스크립트는 환경 변수를 사용하여 DB 연결 정보를 오버라이드합니다.

echo "=========================================="
echo "Trading System MVP - Starting with ENV"
echo "=========================================="

# Java 17 설정
export JAVA_HOME=/Users/changsupark/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home

# 데이터베이스 연결 정보 (환경 변수로 오버라이드)
export SPRING_DATASOURCE_URL="jdbc:mariadb://localhost:3306/trading_mvp?useUnicode=true&characterEncoding=utf8mb4"
export SPRING_DATASOURCE_USERNAME="nextman"
export SPRING_DATASOURCE_PASSWORD="***REMOVED***"

# KIS API 키 (필요시 설정)
export KIS_PAPER_APP_KEY="${KIS_PAPER_APP_KEY:-}"
export KIS_PAPER_APP_SECRET="${KIS_PAPER_APP_SECRET:-}"

echo ""
echo "Environment Variables:"
echo "  JAVA_HOME: $JAVA_HOME"
echo "  DB URL: $SPRING_DATASOURCE_URL"
echo "  DB USER: $SPRING_DATASOURCE_USERNAME"
echo ""
echo "Starting application..."
echo ""

# Maven으로 실행
mvn spring-boot:run
