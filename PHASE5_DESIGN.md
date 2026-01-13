# Phase 5 Design: KIS 실제 연동

## 📋 목차
1. [개요](#개요)
2. [요구사항](#요구사항)
3. [아키텍처](#아키텍처)
4. [구현 계획](#구현-계획)
5. [API 명세](#api-명세)
6. [데이터베이스](#데이터베이스)
7. [테스트 계획](#테스트-계획)

---

## 개요

### 목표
Phase 4에서 구현한 백테스팅 엔진을 기반으로, **실제 KIS API와 연동하여 PAPER 계좌에서 실전 트레이딩**을 수행합니다.

### 범위
- ✅ **실시간 시장 데이터 수집** - KIS WebSocket으로 틱/호가 데이터 수신
- ✅ **PAPER 계좌 실전 테스트** - 실제 KIS PAPER API 호출
- ✅ **WebSocket 안정성 강화** - 재연결, 에러 핸들링, 하트비트
- ✅ **실시간 체결 처리** - WebSocket으로 체결 알림 수신 및 처리

### 제외 사항
- ❌ LIVE 계좌 거래 (Phase 6 이후)
- ❌ 다계좌 동시 운용 (Phase 6 이후)
- ❌ 고급 주문 타입 (IOC/FOK/스톱) (Phase 6 이후)

---

## 요구사항

### 1. 실시간 시장 데이터 수집

**FR-5.1: WebSocket 틱 데이터 수신**
- KIS WebSocket API로 실시간 틱 데이터 수신
- 수신한 틱 데이터를 MarketDataCache에 저장
- BarAggregator로 1분봉 집계
- 종목별 독립적인 스트림 관리

**FR-5.2: 호가 데이터 수신 (선택)**
- 실시간 호가창 데이터 수신
- 10호가 데이터 저장
- 주문 시 최우선 호가 활용

**FR-5.3: 데이터 품질 관리**
- 데이터 유실 감지 및 로깅
- 중복 데이터 필터링
- 타임스탬프 정합성 검증

### 2. PAPER 계좌 실전 테스트

**FR-5.4: KIS PAPER API 연동**
- 실제 KIS PAPER API 호출 (stub 모드 제거)
- 주문 발송 및 응답 처리
- 계좌 잔고 조회
- 포지션 조회

**FR-5.5: 체결 확인 및 반영**
- WebSocket으로 체결 알림 수신
- 체결 데이터를 DB에 저장
- 포지션/PnL 자동 업데이트

**FR-5.6: 오류 처리**
- API 호출 실패 시 재시도 (exponential backoff)
- 오류 로깅 및 알림
- Kill Switch 자동 활성화 (특정 오류)

### 3. WebSocket 안정성 강화

**FR-5.7: 자동 재연결**
- 연결 끊김 감지
- 지수 백오프로 재연결
- 재연결 시 구독 복원

**FR-5.8: 하트비트 및 핑/퐁**
- 주기적 핑 메시지 전송
- 퐁 응답 타임아웃 모니터링
- 무응답 시 재연결

**FR-5.9: 에러 핸들링**
- WebSocket 에러 분류 (네트워크/인증/데이터)
- 에러별 적절한 대응 (재연결/Kill Switch/알림)
- 에러 통계 수집

### 4. 실시간 체결 처리

**FR-5.10: 체결 알림 수신**
- KIS WebSocket 체결 알림 구독
- 체결 데이터 파싱 및 검증
- 중복 체결 필터링 (idempotency)

**FR-5.11: 체결 반영**
- ApplyFillUseCase 호출
- 포지션 업데이트
- PnL 계산 및 저장
- 이벤트 발행 (FILL_APPLIED)

**FR-5.12: 미체결 주문 모니터링**
- 주문 체결 상태 추적
- 타임아웃된 주문 처리
- 부분 체결 처리

---

## 아키텍처

### 시스템 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                    Trading Application                       │
│                                                              │
│  ┌──────────────┐     ┌──────────────┐    ┌──────────────┐ │
│  │   Strategy   │────▶│TradingWorkflow│───▶│PlaceOrderUC │ │
│  │   Scheduler  │     └──────────────┘    └──────────────┘ │
│  └──────────────┘              │                  │         │
│         │                      │                  ▼         │
│         ▼                      │           ┌──────────────┐ │
│  ┌──────────────┐              │           │ KisOrderClient│ │
│  │BarAggregator │              │           └──────────────┘ │
│  └──────────────┘              │                  │         │
│         ▲                      ▼                  │         │
│         │               ┌──────────────┐          │         │
│  ┌──────────────┐       │MarketDataCache│         │         │
│  │  MarketData  │       └──────────────┘          │         │
│  │    Cache     │              ▲                  │         │
│  └──────────────┘              │                  │         │
│         ▲                      │                  │         │
└─────────┼──────────────────────┼──────────────────┼─────────┘
          │                      │                  │
          │                      │                  ▼
┌─────────┴──────────────────────┴──────────────────────────┐
│              KIS WebSocket Client                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Tick Stream  │  │ Fill Stream  │  │ OrderResponse│    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│         │                 │                    │           │
│         │    Reconnection │   Heartbeat        │           │
│         │    Handler      │   Monitor          │           │
│         │                 │                    │           │
└─────────┼─────────────────┼────────────────────┼───────────┘
          │                 │                    │
          ▼                 ▼                    ▼
    ┌─────────────────────────────────────────────────┐
    │          KIS OpenAPI (PAPER)                    │
    │  - WebSocket: ws://ops.koreainvestment.com      │
    │  - REST API: https://openapi.koreainvestment... │
    └─────────────────────────────────────────────────┘
```

### 핵심 컴포넌트

#### 1. KisWebSocketClient (강화)

**책임**:
- KIS WebSocket 연결 관리
- 자동 재연결 (exponential backoff)
- 하트비트 관리
- 스트림 구독 관리

**주요 메서드**:
```java
public interface KisWebSocketClient {
    // 연결 관리
    void connect();
    void disconnect();
    boolean isConnected();

    // 구독 관리
    void subscribeTicks(String symbol);
    void unsubscribeTicks(String symbol);
    void subscribeFills(String accountId);
    void unsubscribeFills(String accountId);

    // 이벤트 핸들러
    void onTick(Consumer<MarketTick> handler);
    void onFill(Consumer<Fill> handler);
    void onError(Consumer<WebSocketError> handler);
    void onReconnect(Runnable handler);
}
```

#### 2. WebSocketReconnectionManager

**책임**:
- 재연결 로직 관리
- 지수 백오프 계산
- 최대 재시도 제한

**재연결 정책**:
```java
public class ReconnectionPolicy {
    private int maxRetries = 10;
    private long initialDelayMs = 1000;
    private long maxDelayMs = 60000;
    private double backoffMultiplier = 2.0;

    public long calculateDelay(int attemptNumber) {
        long delay = initialDelayMs * (long)Math.pow(backoffMultiplier, attemptNumber);
        return Math.min(delay, maxDelayMs);
    }
}
```

#### 3. HeartbeatMonitor

**책임**:
- 주기적 핑 전송
- 퐁 응답 모니터링
- 무응답 시 재연결 트리거

**설정**:
- Ping 주기: 30초
- Pong 타임아웃: 10초
- 연속 실패 허용: 3회

#### 4. MarketDataCollector

**책임**:
- WebSocket으로 수신한 틱 데이터 처리
- MarketDataCache 업데이트
- BarAggregator 트리거
- 데이터 품질 검증

#### 5. FillStreamHandler

**책임**:
- WebSocket 체결 알림 수신
- 체결 데이터 검증
- ApplyFillUseCase 호출
- 중복 체결 필터링

---

## 구현 계획

### Phase 5.1: WebSocket 안정성 강화

**구현 항목**:
1. `WebSocketReconnectionManager.java`
2. `HeartbeatMonitor.java`
3. `WebSocketErrorClassifier.java`
4. `KisWebSocketClientImpl` 강화
5. 테스트 코드 (6 tests)

**예상 소요**: 2일

---

### Phase 5.2: 실시간 시장 데이터 수집

**구현 항목**:
1. `MarketDataCollector.java`
2. `TickDataValidator.java`
3. `DataQualityMonitor.java`
4. KIS WebSocket 틱 스트림 구독
5. 테스트 코드 (8 tests)

**예상 소요**: 2일

---

### Phase 5.3: PAPER 계좌 실전 테스트

**구현 항목**:
1. `KisOrderClientImpl` - stub 제거, 실제 API 호출
2. `KisAccountClient` - 계좌 조회 API
3. `KisPositionClient` - 포지션 조회 API
4. API 호출 재시도 로직
5. 오류 처리 및 로깅
6. 테스트 코드 (10 tests)

**예상 소요**: 3일

---

### Phase 5.4: 실시간 체결 처리

**구현 항목**:
1. `FillStreamHandler.java`
2. `FillDataValidator.java`
3. `DuplicateFillFilter.java`
4. WebSocket 체결 알림 구독
5. 테스트 코드 (6 tests)

**예상 소요**: 2일

---

### Phase 5.5: 통합 테스트 및 문서화

**구현 항목**:
1. E2E 통합 테스트 (5 tests)
2. PHASE5_COMPLETE.md
3. KIS_INTEGRATION_GUIDE.md
4. README 업데이트

**예상 소요**: 1일

---

## API 명세

### KIS WebSocket API

#### 1. 실시간 체결가 (H0STCNT0)

**요청**:
```json
{
  "header": {
    "approval_key": "YOUR_APPROVAL_KEY",
    "custtype": "P",
    "tr_type": "1",
    "content-type": "utf-8"
  },
  "body": {
    "input": {
      "tr_id": "H0STCNT0",
      "tr_key": "005930"
    }
  }
}
```

**응답**:
```json
{
  "header": {
    "tr_id": "H0STCNT0"
  },
  "body": {
    "rt_cd": "0",
    "msg_cd": "MCA00000",
    "msg1": "정상처리 되었습니다.",
    "output": {
      "MKSC_SHRN_ISCD": "005930",
      "STCK_CNTG_HOUR": "153000",
      "STCK_PRPR": "70000",
      "PRDY_VRSS": "500",
      "CNTG_VOL": "1000"
    }
  }
}
```

#### 2. 실시간 체결 알림 (H0STCNI9)

**요청**:
```json
{
  "header": {
    "approval_key": "YOUR_APPROVAL_KEY",
    "custtype": "P",
    "tr_type": "1"
  },
  "body": {
    "input": {
      "tr_id": "H0STCNI9",
      "tr_key": "YOUR_ACCOUNT_NO"
    }
  }
}
```

**응답** (체결 발생 시):
```json
{
  "header": {
    "tr_id": "H0STCNI9"
  },
  "body": {
    "output": {
      "CANO": "50123456",
      "ORD_NO": "0000001234",
      "ORGN_ORD_NO": "0000001234",
      "SLL_BUY_DVSN_CD": "02",
      "PDNO": "005930",
      "ORD_QTY": "10",
      "ORD_UNPR": "70000",
      "CCLD_QTY": "10",
      "CCLD_UNPR": "70000",
      "CCLD_AMT": "700000"
    }
  }
}
```

### KIS REST API

#### 1. 주문 발송 (실제 구현)

**Endpoint**: `POST /uapi/domestic-stock/v1/trading/order-cash`

**Request**:
```json
{
  "CANO": "50123456",
  "ACNT_PRDT_CD": "01",
  "PDNO": "005930",
  "ORD_DVSN": "00",
  "ORD_QTY": "10",
  "ORD_UNPR": "70000"
}
```

**Response**:
```json
{
  "rt_cd": "0",
  "msg_cd": "MCA00000",
  "msg1": "주문이 완료되었습니다.",
  "output": {
    "KRX_FWDG_ORD_ORGNO": "91252",
    "ODNO": "0000001234",
    "ORD_TMD": "153000"
  }
}
```

#### 2. 계좌 잔고 조회

**Endpoint**: `GET /uapi/domestic-stock/v1/trading/inquire-balance`

**Response**:
```json
{
  "output1": [
    {
      "pdno": "005930",
      "hldg_qty": "10",
      "pchs_avg_pric": "69500",
      "evlu_amt": "700000",
      "evlu_pfls_amt": "5000"
    }
  ],
  "output2": [
    {
      "tot_evlu_amt": "10700000",
      "nass_amt": "10700000",
      "fncg_gld_auto_rdpt_yn": "N"
    }
  ]
}
```

---

## 데이터베이스

### 신규 테이블

#### 1. market_data_quality

시장 데이터 품질 모니터링

```sql
CREATE TABLE market_data_quality (
    quality_id VARCHAR(26) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    collected_at DATETIME(3) NOT NULL,
    tick_count INT NOT NULL DEFAULT 0,
    duplicate_count INT NOT NULL DEFAULT 0,
    missing_count INT NOT NULL DEFAULT 0,
    out_of_sequence_count INT NOT NULL DEFAULT 0,
    avg_latency_ms INT,
    max_latency_ms INT,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_symbol_collected (symbol, collected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 2. websocket_connections

WebSocket 연결 이력

```sql
CREATE TABLE websocket_connections (
    connection_id VARCHAR(26) PRIMARY KEY,
    connection_type VARCHAR(20) NOT NULL, -- TICK, FILL
    connected_at DATETIME(3) NOT NULL,
    disconnected_at DATETIME(3),
    disconnect_reason VARCHAR(200),
    reconnect_count INT NOT NULL DEFAULT 0,
    total_messages_received BIGINT NOT NULL DEFAULT 0,
    total_errors INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_type_connected (connection_type, connected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 테스트 계획

### 단위 테스트 (35 tests)

**WebSocket 안정성 (6 tests)**:
1. `WebSocketReconnectionManagerTest`
   - 재연결 지수 백오프 계산
   - 최대 재시도 제한
   - 재연결 성공 후 카운터 리셋

2. `HeartbeatMonitorTest`
   - 주기적 핑 전송
   - 퐁 타임아웃 감지
   - 재연결 트리거

**시장 데이터 수집 (8 tests)**:
3. `MarketDataCollectorTest`
   - 틱 데이터 수신 및 저장
   - 중복 데이터 필터링
   - BarAggregator 트리거

4. `TickDataValidatorTest`
   - 타임스탬프 검증
   - 가격 범위 검증
   - 필수 필드 검증

5. `DataQualityMonitorTest`
   - 데이터 유실 감지
   - 레이턴시 측정
   - 품질 통계 수집

**PAPER 계좌 연동 (10 tests)**:
6. `KisOrderClientImplTest`
   - 실제 주문 발송 (mocked)
   - API 응답 파싱
   - 재시도 로직

7. `KisAccountClientTest`
   - 계좌 잔고 조회
   - 응답 파싱

8. `KisPositionClientTest`
   - 포지션 조회
   - 응답 파싱

**체결 처리 (6 tests)**:
9. `FillStreamHandlerTest`
   - WebSocket 체결 알림 파싱
   - ApplyFillUseCase 호출
   - 중복 체결 필터링

10. `DuplicateFillFilterTest`
    - 중복 체결 감지
    - 캐시 관리

### 통합 테스트 (5 tests)

11. `KisWebSocketIntegrationTest`
    - 실제 KIS WebSocket 연결 (테스트 계정)
    - 틱 데이터 수신
    - 재연결 시나리오

12. `RealTimeTradingIntegrationTest`
    - 실시간 데이터 수신 → 전략 실행 → 주문 발송
    - E2E 파이프라인

13. `FillProcessingIntegrationTest`
    - 주문 발송 → 체결 알림 수신 → 포지션 업데이트

14. `WebSocketReconnectionIntegrationTest`
    - 강제 연결 끊기 → 자동 재연결 → 구독 복원

15. `DataQualityIntegrationTest`
    - 1시간 데이터 수집 → 품질 통계 검증

---

## 비기능 요구사항

### 성능
- WebSocket 메시지 처리 지연: < 100ms (P95)
- 틱 데이터 → BarAggregator 지연: < 50ms (P95)
- 체결 알림 → 포지션 업데이트: < 200ms (P95)

### 가용성
- WebSocket 재연결 성공률: > 99%
- 데이터 유실률: < 0.1%
- 중복 데이터 필터링률: 100%

### 보안
- API 키 환경변수 관리
- WebSocket 인증 토큰 갱신
- 민감 데이터 로깅 금지

---

## 위험 요소 및 대응

### 1. KIS API 호출 제한
**위험**: API 호출 횟수 제한 초과 (분당 20회)
**대응**:
- 호출 빈도 모니터링
- Rate Limiter 구현
- 캐시 활용

### 2. WebSocket 연결 불안정
**위험**: 네트워크 불안정으로 빈번한 재연결
**대응**:
- 지수 백오프 재연결
- 재연결 중 주문 차단
- Kill Switch 자동 활성화

### 3. 체결 데이터 유실
**위험**: WebSocket 체결 알림 유실
**대응**:
- 주기적 체결 조회 API 호출 (fallback)
- 미체결 주문 타임아웃 모니터링
- 알림 발송

---

## 일정

| Phase | 기간 | 산출물 |
|-------|------|--------|
| Phase 5.1 | 2일 | WebSocket 안정성 (6 tests) |
| Phase 5.2 | 2일 | 시장 데이터 수집 (8 tests) |
| Phase 5.3 | 3일 | PAPER 계좌 연동 (10 tests) |
| Phase 5.4 | 2일 | 체결 처리 (6 tests) |
| Phase 5.5 | 1일 | 통합 테스트 + 문서 (5 tests) |
| **총계** | **10일** | **35 tests + 문서** |

---

## 참고 문서

- [KIS OpenAPI 개발가이드](https://apiportal.koreainvestment.com/)
- [WebSocket 프로토콜](https://datatracker.ietf.org/doc/html/rfc6455)
- PHASE3_COMPLETE.md
- PHASE4_COMPLETE.md

---

**작성일**: 2026-01-01
**작성자**: Claude Sonnet 4.5
**버전**: 1.0
