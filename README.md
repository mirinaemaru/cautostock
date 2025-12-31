# Trading System MVP (B안)

KIS OpenAPI 기반 자동매매 시스템 MVP 구현

## 프로젝트 개요

- **목적**: KIS (한국투자증권) OpenAPI를 활용한 자동매매 시스템 구축
- **범위**: MVP 1차 구현 - 모의투자(PAPER) 기준
- **아키텍처**: Layered + Hexagonal (Ports & Adapters), Event-Driven (Outbox Pattern)
- **기술 스택**: Java 17, Spring Boot 3.2.1, MariaDB, Flyway

## 기술 스택

- Java 17
- Spring Boot 3.2.1
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Actuator
  - Spring WebFlux (KIS API 호출용)
- MariaDB 10.x
- Flyway (Database Migration)
- Lombok
- Gradle 8.5

## 프로젝트 구조

```
maru.trading/
├─ api/              # REST Controllers (Admin, Query, Demo, Health)
├─ application/      # Use Cases & Workflows
├─ domain/           # Domain Models (순수 비즈니스 로직)
├─ infra/            # Infrastructure (DB, Cache, Scheduler)
└─ broker/kis/       # KIS Adapter (REST/WebSocket)
```

## 사전 요구사항

1. **Java 17** 이상
2. **MariaDB 10.x** 이상
3. **Gradle 8.x**

## 데이터베이스 설정

### MariaDB 설치 및 설정

```bash
# MariaDB 설치 (macOS)
brew install mariadb
brew services start mariadb

# 데이터베이스 생성
mysql -u root -p

CREATE DATABASE trading_mvp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'trading_user'@'localhost' IDENTIFIED BY 'trading_pass';
GRANT ALL PRIVILEGES ON trading_mvp.* TO 'trading_user'@'localhost';
FLUSH PRIVILEGES;
```

### Flyway 마이그레이션

애플리케이션 시작 시 자동으로 Flyway 마이그레이션이 실행됩니다.

수동 실행:
```bash
./gradlew flywayMigrate
```

## 빌드 및 실행

### 빌드

```bash
# Java 17 설정
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 빌드
./gradlew clean build
```

### 실행

```bash
# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/trading-system-0.1.0-SNAPSHOT.jar
```

### 환경 변수 설정

KIS API 키를 환경 변수로 설정:

```bash
export KIS_PAPER_APP_KEY=your_paper_app_key
export KIS_PAPER_APP_SECRET=your_paper_app_secret
```

또는 `application-local.yml` 파일 생성:

```yaml
trading:
  broker:
    kis:
      paper:
        app-key: your_paper_app_key
        app-secret: your_paper_app_secret
```

## API 엔드포인트

애플리케이션 실행 후:

- **Health Check**: http://localhost:8080/health
- **Actuator**: http://localhost:8080/actuator
- **Swagger UI** (예정): http://localhost:8080/swagger-ui.html

### 주요 API

- `POST /api/v1/admin/accounts` - 계좌 등록
- `POST /api/v1/admin/strategies` - 전략 생성
- `POST /api/v1/admin/kill-switch` - Kill Switch 토글
- `GET /api/v1/query/orders` - 주문 조회
- `GET /api/v1/query/positions` - 포지션 조회

## 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "maru.trading.*"
```

## 개발 가이드

### 설계 문서

모든 구현은 `/md/docs/` 디렉토리의 설계 문서를 기준으로 합니다:

- `00_README.md` - 프로젝트 개요
- `01_ARCHITECTURE.md` - 아키텍처 설계
- `02_PACKAGE_STRUCTURE.md` - 패키지 구조
- `04_API_OPENAPI.md` - API 명세
- `06_DB_SCHEMA_MARIADB.md` - DB 스키마

### 핵심 원칙

1. **멱등성**: 모든 주문은 `idempotency_key`로 중복 방지
2. **Outbox Pattern**: 이벤트는 DB 트랜잭션과 함께 Outbox에 저장 후 발행
3. **Kill Switch**: 리스크 한도 초과 시 자동으로 거래 차단
4. **KIS Safety**: MVP는 PAPER(모의투자)만 지원

## MVP 범위

### 포함 기능
- ✅ 계좌 관리 (모의/실전 분리)
- ✅ 전략 관리 (등록/활성화)
- ✅ 신호 생성 (BUY/SELL/HOLD)
- ✅ 리스크 관리 + Kill Switch
- ✅ 주문 관리 (지정가/시장가)
- ✅ 체결/포지션/손익 관리
- ✅ 운영/모니터링/알림
- ✅ Event Outbox

### 제외 기능 (2차 이후)
- IOC/FOK/스톱 주문
- 다계좌/다전략 동시 운용
- 백테스팅
- 운영 UI 대시보드

## 라이선스

내부 프로젝트

## 문의

- 설계 문서: `/md/docs/`
- 이슈 리포팅: GitHub Issues
