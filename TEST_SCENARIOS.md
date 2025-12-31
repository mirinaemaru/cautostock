# 테스트 시나리오

## 목차
1. [환경 설정](#환경-설정)
2. [단위 테스트 시나리오](#단위-테스트-시나리오)
3. [통합 테스트 시나리오](#통합-테스트-시나리오)
4. [E2E 테스트 시나리오](#e2e-테스트-시나리오)
5. [성능 테스트 시나리오](#성능-테스트-시나리오)

---

## 환경 설정

### 테스트 데이터베이스 설정
```bash
# 테스트용 DB 생성
CREATE DATABASE trading_mvp_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'trading_test'@'localhost' IDENTIFIED BY 'test_pass';
GRANT ALL PRIVILEGES ON trading_mvp_test.* TO 'trading_test'@'localhost';
FLUSH PRIVILEGES;
```

### 테스트 실행
```bash
# Java 17 설정
export JAVA_HOME=/usr/libexec/java_home -v 17

# 전체 테스트
mvn clean test

# 특정 테스트 클래스
mvn test -Dtest=AccountServiceTest

# 통합 테스트
mvn verify
```

---

## 단위 테스트 시나리오

### 1. 계좌 관리 (Account Management)

#### TC-ACC-001: 계좌 등록 - 정상
- **Given**: 유효한 KIS 계좌 정보 (PAPER)
- **When**: POST `/api/v1/admin/accounts` 호출
- **Then**:
  - HTTP 201 Created
  - accountId 생성됨
  - status = ACTIVE
  - DB에 저장 확인

#### TC-ACC-002: 계좌 등록 - 중복
- **Given**: 이미 등록된 계좌번호
- **When**: 동일한 계좌로 POST 재시도
- **Then**:
  - HTTP 409 Conflict
  - ErrorResponse 반환

#### TC-ACC-003: 계좌 상태 변경
- **Given**: ACTIVE 상태 계좌
- **When**: PUT `/api/v1/admin/accounts/{accountId}/status` with INACTIVE
- **Then**:
  - HTTP 200 OK
  - status = INACTIVE로 변경
  - updatedAt 갱신

#### TC-ACC-004: 계좌 권한 설정
- **Given**: 등록된 계좌
- **When**: PUT `/api/v1/admin/accounts/{accountId}/permissions`
  ```json
  {
    "permissions": [
      {"code": "TRADE_BUY", "enabled": true},
      {"code": "TRADE_SELL", "enabled": true},
      {"code": "AUTO_TRADE", "enabled": true}
    ]
  }
  ```
- **Then**:
  - HTTP 200 OK
  - 권한 설정 완료

---

### 2. 전략 관리 (Strategy Management)

#### TC-STR-001: 전략 생성 - 정상
- **Given**: 유효한 전략 파라미터
- **When**: POST `/api/v1/admin/strategies`
  ```json
  {
    "name": "DEMO_MA_CROSS_1M",
    "description": "1분봉 이동평균 크로스 전략",
    "mode": "PAPER",
    "params": {
      "shortPeriod": 5,
      "longPeriod": 20,
      "symbol": "005930"
    }
  }
  ```
- **Then**:
  - HTTP 201 Created
  - strategyId, activeVersionId 생성
  - status = INACTIVE (기본값)
  - params 저장 확인

#### TC-STR-002: 전략 활성화
- **Given**: INACTIVE 상태의 전략
- **When**: POST `/api/v1/admin/strategies/{strategyId}/activate`
- **Then**:
  - HTTP 200 OK
  - status = ACTIVE
  - 신호 생성 가능 상태

#### TC-STR-003: 전략 파라미터 업데이트
- **Given**: 활성화된 전략
- **When**: PUT `/api/v1/admin/strategies/{strategyId}/params`
  ```json
  {
    "params": {
      "shortPeriod": 10,
      "longPeriod": 30
    }
  }
  ```
- **Then**:
  - HTTP 200 OK
  - 새로운 버전 생성
  - activeVersionId 갱신

#### TC-STR-004: 전략 비활성화
- **Given**: ACTIVE 상태의 전략
- **When**: POST `/api/v1/admin/strategies/{strategyId}/deactivate`
- **Then**:
  - HTTP 200 OK
  - status = INACTIVE
  - 신호 생성 중지

---

### 3. 리스크 관리 (Risk Management)

#### TC-RISK-001: 리스크 룰 설정
- **Given**: 등록된 계좌
- **When**: PUT `/api/v1/admin/risk/rules`
  ```json
  {
    "accountId": "acct_01H...",
    "rules": {
      "maxPositionValuePerSymbol": 100000,
      "maxOpenOrders": 1,
      "maxOrdersPerMinute": 2,
      "dailyLossLimit": 3000,
      "consecutiveOrderFailuresLimit": 3
    }
  }
  ```
- **Then**:
  - HTTP 200 OK
  - 룰 저장 완료

#### TC-RISK-002: 리스크 상태 조회
- **Given**: 거래 중인 계좌
- **When**: GET `/api/v1/admin/risk/state?accountId=acct_01H...`
- **Then**:
  - HTTP 200 OK
  - dailyPnl, exposure, killSwitchStatus 반환

#### TC-RISK-003: Kill Switch - 수동 ON
- **Given**: 정상 운영 중
- **When**: POST `/api/v1/admin/kill-switch`
  ```json
  {
    "accountId": "acct_01H...",
    "status": "ON",
    "reason": "MANUAL"
  }
  ```
- **Then**:
  - HTTP 200 OK
  - killSwitchStatus = ON
  - 모든 신규 주문 차단

#### TC-RISK-004: Kill Switch - 일일 손실 한도 초과
- **Given**: dailyLossLimit = 3000
- **When**: 실현손실이 -3500 도달
- **Then**:
  - Kill Switch 자동 ON
  - reason = "DAILY_LOSS_LIMIT"
  - Alert 발행

#### TC-RISK-005: Kill Switch - OFF 복구
- **Given**: Kill Switch = ON
- **When**: POST `/api/v1/admin/kill-switch` with status=OFF
- **Then**:
  - HTTP 200 OK
  - killSwitchStatus = OFF
  - 거래 재개 가능

---

### 4. 주문 관리 (Order Management)

#### TC-ORD-001: 지정가 주문 - 정상
- **Given**: 활성화된 전략, Kill Switch OFF
- **When**: 전략이 BUY 신호 생성 → 주문 생성
- **Then**:
  - Order 엔티티 생성 (status=NEW)
  - idempotencyKey 생성
  - Outbox에 OrderCreated 이벤트 저장
  - KIS Adapter 호출 (stub)
  - status → SENT → ACCEPTED

#### TC-ORD-002: 시장가 주문
- **Given**: 긴급 청산 필요
- **When**: 시장가 SELL 주문
- **Then**:
  - orderType = MARKET
  - price = null or 0
  - 즉시 체결 시뮬레이션

#### TC-ORD-003: 주문 중복 방지 (Idempotency)
- **Given**: 이미 생성된 주문 (idempotencyKey="key123")
- **When**: 동일한 idempotencyKey로 재시도
- **Then**:
  - 신규 주문 생성 없음
  - 기존 주문 상태 반환
  - HTTP 200 OK

#### TC-ORD-004: Kill Switch 활성화 시 주문 거부
- **Given**: Kill Switch = ON
- **When**: 신규 주문 시도
- **Then**:
  - 주문 생성 차단
  - ErrorResponse: "KILL_SWITCH_ON"
  - HTTP 403 Forbidden

#### TC-ORD-005: 리스크 룰 위반 - maxOpenOrders
- **Given**: maxOpenOrders = 1, 이미 1개 오픈 주문 존재
- **When**: 추가 주문 시도
- **Then**:
  - 주문 거부
  - ErrorResponse: "MAX_OPEN_ORDERS_EXCEEDED"

#### TC-ORD-006: 리스크 룰 위반 - maxOrdersPerMinute
- **Given**: maxOrdersPerMinute = 2
- **When**: 1분 내 3번째 주문 시도
- **Then**:
  - 주문 거부
  - ErrorResponse: "ORDER_RATE_LIMIT_EXCEEDED"

#### TC-ORD-007: 주문 조회
- **Given**: 여러 주문 존재
- **When**: GET `/api/v1/query/orders?accountId=acct_01H&symbol=005930&status=FILLED`
- **Then**:
  - HTTP 200 OK
  - 필터링된 주문 목록 반환

---

### 5. 체결 및 포지션 (Execution & Position)

#### TC-EXEC-001: 체결 반영 - 전체 체결
- **Given**: ACCEPTED 상태 주문 (qty=10)
- **When**: KIS WS로부터 체결 이벤트 수신 (fillQty=10)
- **Then**:
  - Fill 엔티티 생성
  - Order.status = FILLED
  - Position 업데이트 (qty +10 or -10)
  - PnL 계산
  - Outbox에 FillReceived 이벤트

#### TC-EXEC-002: 체결 반영 - 부분 체결
- **Given**: ACCEPTED 상태 주문 (qty=10)
- **When**: 체결 이벤트 (fillQty=5)
- **Then**:
  - Fill 엔티티 생성 (fillQty=5)
  - Order.status = PART_FILLED
  - Position 업데이트 (qty +5)

#### TC-EXEC-003: 포지션 조회
- **Given**: 여러 종목 포지션 보유
- **When**: GET `/api/v1/query/positions?accountId=acct_01H...`
- **Then**:
  - HTTP 200 OK
  - symbol별 포지션 목록 반환
  - qty, avgPrice, realizedPnl 포함

#### TC-EXEC-004: 손익 스냅샷 조회
- **Given**: 포트폴리오 운영 중
- **When**: GET `/api/v1/query/pnl/snapshot?accountId=acct_01H...`
- **Then**:
  - HTTP 200 OK
  - totalValue, cash, unrealizedPnl, realizedPnl 반환

---

### 6. Health Check & Monitoring

#### TC-HEALTH-001: Health Check - 정상
- **Given**: 모든 컴포넌트 정상
- **When**: GET `/health`
- **Then**:
  - HTTP 200 OK
  - status = UP
  - components.db = UP
  - components.kisRest = UP
  - components.kisWs = UP

#### TC-HEALTH-002: Health Check - DB 장애
- **Given**: DB 연결 끊김
- **When**: GET `/health`
- **Then**:
  - HTTP 503 Service Unavailable
  - status = DOWN
  - components.db = DOWN

---

## 통합 테스트 시나리오

### Integration-001: 전체 플로우 - BUY 주문 → 체결 → 포지션

```
Step 1. 계좌 등록
  POST /api/v1/admin/accounts
  → accountId = "acct_001"

Step 2. 전략 생성 및 활성화
  POST /api/v1/admin/strategies
  → strategyId = "str_001"
  POST /api/v1/admin/strategies/str_001/activate

Step 3. 리스크 룰 설정
  PUT /api/v1/admin/risk/rules
  → dailyLossLimit = 5000

Step 4. Kill Switch 확인
  GET /api/v1/admin/kill-switch
  → status = OFF

Step 5. 신호 생성 (Demo API)
  POST /api/v1/demo/signal
  {
    "accountId": "acct_001",
    "symbol": "005930",
    "side": "BUY",
    "targetType": "QTY",
    "targetValue": 10
  }

Step 6. TradingWorkflow 실행
  → Signal 생성
  → RiskEngine 승인
  → Order 생성 (status=NEW)
  → KIS Adapter 호출 (stub)
  → Order.status = SENT → ACCEPTED

Step 7. 체결 이벤트 시뮬레이션
  → Fill 생성 (fillQty=10, fillPrice=72000)
  → Order.status = FILLED
  → Position 생성 (qty=10, avgPrice=72000)

Step 8. 포지션 확인
  GET /api/v1/query/positions?accountId=acct_001
  → symbol=005930, qty=10, avgPrice=72000

Step 9. 손익 확인
  GET /api/v1/query/pnl/snapshot?accountId=acct_001
  → unrealizedPnl 계산
```

**검증 포인트**:
- 모든 상태 전이가 Outbox를 통해 기록됨
- 트랜잭션 원자성 보장
- 이벤트 순서 보장

---

### Integration-002: Kill Switch 자동 발동 플로우

```
Step 1. 계좌/전략/리스크 룰 설정
  dailyLossLimit = 3000

Step 2. BUY 주문 체결
  → Position: qty=10, avgPrice=70000

Step 3. SELL 주문 체결 (손실)
  → fillPrice=66500 (손실 -3500 per 1qty)
  → realizedPnl = -35000 (10주 전량 매도 가정)

Step 4. PnL 업데이트
  → dailyPnl = -35000 (가정: 1주당 -3500)

Step 5. RiskEngine 평가
  → dailyPnl < -dailyLossLimit
  → Kill Switch 자동 ON
  → reason = "DAILY_LOSS_LIMIT"
  → Alert 발행

Step 6. 추가 주문 시도
  → 주문 차단 (403 Forbidden)

Step 7. Kill Switch 조회
  GET /api/v1/admin/kill-switch?accountId=acct_001
  → status = ON
  → reason = DAILY_LOSS_LIMIT
```

**검증 포인트**:
- 손실 한도 초과 시 자동 Kill Switch 발동
- 모든 신규 주문 차단
- Alert 정상 발행

---

### Integration-003: Outbox Pattern 검증

```
Step 1. 주문 생성
  → orders 테이블에 INSERT
  → outbox 테이블에 OrderCreated 이벤트 INSERT
  → 동일 트랜잭션 커밋

Step 2. Outbox Publisher
  → outbox 테이블 polling
  → OrderCreated 이벤트 발행
  → outbox.published_at 갱신

Step 3. 체결 반영
  → fills 테이블에 INSERT
  → orders 테이블 UPDATE (status=FILLED)
  → positions 테이블 UPSERT
  → outbox 테이블에 FillReceived 이벤트 INSERT
  → 동일 트랜잭션 커밋

Step 4. 검증
  → outbox 테이블에 모든 이벤트 존재
  → published_at이 null이 아닌 이벤트만 발행됨
  → 중복 발행 방지 (published_at 체크)
```

**검증 포인트**:
- At-least-once 이벤트 전달 보장
- 트랜잭션 원자성 (DB 저장 + Outbox)
- 이벤트 순서 보장 (created_at 기준)

---

## E2E 테스트 시나리오

### E2E-001: MVP Demo 시나리오

**목표**: 계좌 등록부터 전략 실행, 체결, 손익 확인까지 전체 플로우 검증

```
1. 사전 준비
  - MariaDB 실행
  - Flyway 마이그레이션 완료
  - 애플리케이션 시작 (mvn spring-boot:run)

2. Health Check
  curl http://localhost:8080/health
  → status = UP

3. 계좌 등록 (PAPER)
  curl -X POST http://localhost:8080/api/v1/admin/accounts \
    -H "Content-Type: application/json" \
    -d '{
      "broker": "KIS",
      "environment": "PAPER",
      "cano": "50068923",
      "acntPrdtCd": "01",
      "alias": "paper-demo"
    }'
  → accountId 획득

4. 계좌 권한 설정
  curl -X PUT http://localhost:8080/api/v1/admin/accounts/{accountId}/permissions \
    -H "Content-Type: application/json" \
    -d '{
      "permissions": [
        {"code": "TRADE_BUY", "enabled": true},
        {"code": "TRADE_SELL", "enabled": true},
        {"code": "AUTO_TRADE", "enabled": true}
      ]
    }'

5. 전략 생성
  curl -X POST http://localhost:8080/api/v1/admin/strategies \
    -H "Content-Type: application/json" \
    -d '{
      "name": "DEMO_MA_CROSS",
      "mode": "PAPER",
      "params": {"shortPeriod": 5, "longPeriod": 20, "symbol": "005930"}
    }'
  → strategyId 획득

6. 전략 활성화
  curl -X POST http://localhost:8080/api/v1/admin/strategies/{strategyId}/activate

7. 리스크 룰 설정
  curl -X PUT http://localhost:8080/api/v1/admin/risk/rules \
    -H "Content-Type: application/json" \
    -d '{
      "accountId": "{accountId}",
      "rules": {
        "maxPositionValuePerSymbol": 1000000,
        "maxOpenOrders": 2,
        "maxOrdersPerMinute": 5,
        "dailyLossLimit": 10000,
        "consecutiveOrderFailuresLimit": 3
      }
    }'

8. Kill Switch 확인
  curl http://localhost:8080/api/v1/admin/kill-switch?accountId={accountId}
  → status = OFF

9. BUY 신호 주입 (Demo)
  curl -X POST http://localhost:8080/api/v1/demo/signal \
    -H "Content-Type: application/json" \
    -d '{
      "accountId": "{accountId}",
      "symbol": "005930",
      "side": "BUY",
      "targetType": "QTY",
      "targetValue": 5
    }'

10. 주문 확인
  curl "http://localhost:8080/api/v1/query/orders?accountId={accountId}"
  → status = FILLED (stub 환경이므로 즉시 체결 가정)

11. 체결 확인
  curl "http://localhost:8080/api/v1/query/fills?accountId={accountId}"
  → fillQty=5

12. 포지션 확인
  curl "http://localhost:8080/api/v1/query/positions?accountId={accountId}"
  → symbol=005930, qty=5

13. SELL 신호 주입
  curl -X POST http://localhost:8080/api/v1/demo/signal \
    -H "Content-Type: application/json" \
    -d '{
      "accountId": "{accountId}",
      "symbol": "005930",
      "side": "SELL",
      "targetType": "QTY",
      "targetValue": 5
    }'

14. 포지션 확인 (청산)
  curl "http://localhost:8080/api/v1/query/positions?accountId={accountId}"
  → qty=0 (청산 완료)

15. 손익 확인
  curl "http://localhost:8080/api/v1/query/pnl/snapshot?accountId={accountId}"
  → realizedPnl 확인
```

**예상 결과**:
- 전체 플로우 정상 동작
- 모든 상태 전이가 DB에 기록
- Outbox 이벤트 발행 확인

---

## 성능 테스트 시나리오

### PERF-001: 주문 처리 성능

**목표**: 초당 주문 처리량 측정

- **부하**: 초당 10개 주문 생성
- **기간**: 60초
- **측정**: 평균 응답 시간, 처리량, 에러율

**기대값** (MVP):
- 평균 응답 시간 < 500ms
- 처리량 >= 10 TPS
- 에러율 < 1%

---

### PERF-002: 조회 API 성능

**목표**: 대량 데이터 조회 성능

- **데이터**: 10,000개 주문 사전 생성
- **쿼리**: GET /api/v1/query/orders?limit=50
- **측정**: 응답 시간, 페이지네이션 정확성

**기대값**:
- 평균 응답 시간 < 200ms
- 페이지네이션 정상 동작

---

### PERF-003: Outbox 발행 지연

**목표**: Outbox 이벤트 발행 지연 측정

- **시나리오**: 100개 주문 생성 → Outbox 이벤트 발행까지 시간
- **측정**: created_at vs published_at 차이

**기대값** (MVP):
- 평균 발행 지연 < 5초
- 최대 발행 지연 < 30초

---

## 테스트 데이터 준비

### 계좌 데이터
```sql
INSERT INTO accounts (account_id, broker, environment, cano, acnt_prdt_cd, status, created_at, updated_at)
VALUES
  ('acct_test_001', 'KIS', 'PAPER', '50068923', '01', 'ACTIVE', NOW(3), NOW(3)),
  ('acct_test_002', 'KIS', 'PAPER', '50068924', '01', 'ACTIVE', NOW(3), NOW(3));
```

### 전략 데이터
```sql
INSERT INTO strategies (strategy_id, name, description, status, mode, created_at, updated_at)
VALUES
  ('str_test_001', 'TEST_STRATEGY', 'Test Strategy', 'ACTIVE', 'PAPER', NOW(3), NOW(3));

INSERT INTO strategy_versions (version_id, strategy_id, version_seq, params_json, created_at)
VALUES
  ('strv_test_001', 'str_test_001', 1, '{"shortPeriod":5,"longPeriod":20}', NOW(3));

UPDATE strategies SET active_version_id = 'strv_test_001' WHERE strategy_id = 'str_test_001';
```

### 리스크 룰 데이터
```sql
INSERT INTO risk_states (state_id, account_id, kill_switch_status, kill_switch_reason, daily_pnl, exposure, updated_at)
VALUES
  ('risk_test_001', 'acct_test_001', 'OFF', NULL, 0, 0, NOW(3));
```

---

## 테스트 체크리스트

### Phase 1: Core Infrastructure
- [ ] DB 마이그레이션 정상 실행
- [ ] JPA 엔티티 CRUD 동작
- [ ] Outbox 테이블 생성 및 조회
- [ ] Health Check 엔드포인트 200 응답

### Phase 2: Admin API
- [ ] 계좌 등록/조회/수정/권한 설정
- [ ] 전략 생성/활성화/비활성화/파라미터 업데이트
- [ ] 리스크 룰 설정/조회
- [ ] Kill Switch 토글/조회

### Phase 3: Query API
- [ ] 주문 조회 (필터링, 페이지네이션)
- [ ] 체결 조회
- [ ] 포지션 조회
- [ ] 손익 스냅샷 조회

### Phase 4: Business Logic
- [ ] TradingWorkflow 동작
- [ ] Signal 생성 및 필터링
- [ ] RiskEngine 평가
- [ ] Kill Switch 자동 발동
- [ ] 주문 생성 및 멱등성
- [ ] 체결 반영 및 포지션 업데이트

### Phase 5: Integration
- [ ] Outbox Pattern 검증
- [ ] 전체 플로우 E2E 테스트
- [ ] KIS Adapter stub 동작
- [ ] Alert 발행 확인

### Phase 6: Non-Functional
- [ ] 주문 처리 성능 (>= 10 TPS)
- [ ] 조회 성능 (< 200ms)
- [ ] Outbox 발행 지연 (< 5초)
- [ ] 동시성 테스트 (Idempotency)

---

## 테스트 실행 가이드

### 1. 로컬 환경 테스트
```bash
# Java 17 설정
export JAVA_HOME=/Library/Java/JavaVirtualMachines/corretto-17.0.5/Contents/Home

# DB 시작 (MariaDB)
brew services start mariadb

# 테스트 DB 초기화
mysql -u root -p < test-data/init-test-db.sql

# 테스트 실행
mvn clean test

# 통합 테스트
mvn verify
```

### 2. E2E 테스트
```bash
# 애플리케이션 시작
mvn spring-boot:run

# 별도 터미널에서 E2E 스크립트 실행
./test-scripts/e2e-demo.sh
```

### 3. 성능 테스트
```bash
# JMeter 또는 Gatling 사용
# (추후 추가 예정)
```

---

## 버그 리포팅 템플릿

```markdown
### Bug Report: [제목]

**TC ID**: TC-XXX-000
**Severity**: Critical | High | Medium | Low
**Environment**: PAPER | LIVE | Local

**Steps to Reproduce**:
1.
2.
3.

**Expected Result**:
-

**Actual Result**:
-

**Logs**:
\```
(로그 붙여넣기)
\```

**DB State** (해당 시):
\```sql
SELECT * FROM orders WHERE order_id = '...';
\```
```

---

## 참고 문서
- [API 명세서](md/docs/04_API_OPENAPI.md)
- [DB 스키마](md/docs/06_DB_SCHEMA_MARIADB.md)
- [MVP 시나리오](md/docs/07_MVP_SCENARIOS.md)
- [아키텍처](md/docs/01_ARCHITECTURE.md)
