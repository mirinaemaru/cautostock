# Quick Start Guide

## 빠른 시작 (MariaDB 없이 테스트)

### 1. 프로젝트 빌드

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew clean build
```

### 2. MariaDB 준비

```bash
# MariaDB 설치 (macOS)
brew install mariadb
brew services start mariadb

# 데이터베이스 생성
mysql -u root -p

CREATE DATABASE trading_mvp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nextman'@'localhost' IDENTIFIED BY '***REMOVED***';
GRANT ALL PRIVILEGES ON trading_mvp.* TO 'nextman'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
java -jar build/libs/trading-system-0.1.0-SNAPSHOT.jar
```

### 4. Health Check

```bash
curl http://localhost:8080/health
```

## API 테스트

### 계좌 등록

```bash
curl -X POST http://localhost:8080/api/v1/admin/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "broker": "KIS",
    "environment": "PAPER",
    "cano": "12345678",
    "acntPrdtCd": "01",
    "alias": "paper-main"
  }'
```

### 계좌 목록 조회

```bash
curl http://localhost:8080/api/v1/admin/accounts
```

### 전략 생성

```bash
curl -X POST http://localhost:8080/api/v1/admin/strategies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "DEMO_MA_CROSS_1M",
    "description": "Moving Average Cross 1 minute",
    "mode": "PAPER",
    "params": {
      "fast_ma": 5,
      "slow_ma": 20,
      "symbol": "005930"
    }
  }'
```

### 전략 활성화

```bash
curl -X POST http://localhost:8080/api/v1/admin/strategies/{strategyId}/activate
```

### Kill Switch 조회

```bash
curl http://localhost:8080/api/v1/admin/kill-switch
```

### Kill Switch 토글

```bash
curl -X POST http://localhost:8080/api/v1/admin/kill-switch \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ON",
    "reason": "MANUAL"
  }'
```

### 주문 조회

```bash
curl "http://localhost:8080/api/v1/query/orders?accountId={accountId}&limit=10"
```

## Phase 1 완료 상태

✅ 프로젝트 스캐폴딩
✅ DB 스키마 (Flyway 마이그레이션 6개)
✅ JPA Entity & Repository (6개 핵심 엔티티)
✅ Global Exception Handler
✅ Admin/Query API (계좌, 전략, Kill Switch, 주문)
✅ Event Outbox Pattern

## 다음 단계 (Phase 2)

- TradingWorkflow Skeleton
- Risk Engine + Kill Switch 로직
- Demo API
- KIS Broker Adapter (Stub)

## 문제 해결

### MariaDB 연결 오류

application.yml에서 데이터베이스 연결 정보를 확인하세요:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/trading_mvp
    username: trading_user
    password: trading_pass
```

### Flyway 마이그레이션 오류

데이터베이스를 초기화하고 다시 시도:

```bash
mysql -u trading_user -p trading_mvp < /dev/null
# 또는
DROP DATABASE trading_mvp;
CREATE DATABASE trading_mvp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
