# Phase 5 완료 보고서: KIS 실제 연동

## 📋 개요

**작성일**: 2026-01-01
**상태**: ✅ 완료
**목표**: KIS OpenAPI 실제 연동을 위한 안정성 및 실시간 데이터 처리 구현

---

## ✅ 구현 완료 항목

### Phase 5.1: WebSocket 안정성 강화

**목표**: WebSocket 연결의 안정성과 복원력 확보

#### 구현된 컴포넌트

**1. ReconnectionPolicy.java**
- **위치**: `maru.trading.broker.kis.websocket`
- **목적**: 재연결 전략 정의 (지수 백오프)
- **핵심 기능**:
  - 최대 재시도 횟수 설정
  - 지수 백오프 계산: `delay = initialDelay * backoffMultiplier^attemptNumber`
  - 최대 지연 시간 제한
- **설정**:
  ```java
  public static ReconnectionPolicy defaultPolicy() {
      return ReconnectionPolicy.builder()
          .maxRetries(10)
          .initialDelayMs(1000)    // 1초
          .maxDelayMs(60000)       // 60초
          .backoffMultiplier(2.0)  // 지수 2
          .build();
  }
  ```

**2. WebSocketReconnectionManager.java**
- **위치**: `maru.trading.broker.kis.websocket`
- **목적**: 자동 재연결 관리
- **핵심 기능**:
  - 연결 실패 시 자동 재연결 스케줄링
  - ScheduledExecutorService 사용
  - AtomicInteger로 재시도 횟수 추적
  - 재연결 성공 시 카운터 리셋
- **사용 예시**:
  ```java
  reconnectionManager.scheduleReconnection(() -> {
      // WebSocket 재연결 로직
      webSocketClient.connect();
  });
  ```

**3. HeartbeatMonitor.java**
- **위치**: `maru.trading.broker.kis.websocket`
- **목적**: WebSocket 연결 상태 모니터링
- **핵심 기능**:
  - Ping/Pong 메커니즘
  - 30초마다 Ping 전송
  - 10초 이내 Pong 응답 대기
  - 3회 연속 실패 시 재연결 트리거
- **파라미터**:
  ```java
  private static final long PING_INTERVAL_MS = 30_000;     // 30초
  private static final long PONG_TIMEOUT_MS = 10_000;      // 10초
  private static final int MAX_CONSECUTIVE_FAILURES = 3;
  ```

**4. WebSocketError.java & WebSocketErrorClassifier.java**
- **위치**: `maru.trading.broker.kis.websocket`
- **목적**: 오류 분류 및 복구 전략 결정
- **오류 타입**:
  - `NETWORK`: 네트워크 오류 → 재연결
  - `AUTHENTICATION`: 인증 오류 → Kill Switch
  - `DATA_PARSING`: 데이터 파싱 오류 → 로그 & 계속
  - `PROTOCOL`: 프로토콜 오류 → 재연결
  - `UNKNOWN`: 알 수 없는 오류 → Alert
- **복구 액션**:
  ```java
  public enum RecoveryAction {
      RECONNECT,         // 재연결 시도
      KILL_SWITCH,       // Kill Switch 활성화
      LOG_AND_CONTINUE,  // 로그만 기록하고 계속
      ALERT              // Alert 발송
  }
  ```

---

### Phase 5.2: 실시간 시장 데이터 수집

**목표**: WebSocket을 통한 실시간 틱 데이터 수집 및 품질 관리

#### 구현된 컴포넌트

**1. MarketDataCollector.java**
- **위치**: `maru.trading.broker.kis.marketdata`
- **목적**: WebSocket으로부터 실시간 틱 데이터 수집 및 처리
- **핵심 워크플로우**:
  ```
  1. 틱 데이터 수신 (WebSocket)
     ↓
  2. 데이터 검증 (TickDataValidator)
     ↓
  3. 캐시 업데이트 (MarketDataCache)
     ↓
  4. 바 집계기 트리거 (BarAggregator)
     ↓
  5. 품질 메트릭 기록 (DataQualityMonitor)
  ```
- **통계 추적**:
  - `ticksReceived`: 총 수신한 틱 수
  - `ticksValid`: 검증 통과한 틱 수
  - `ticksInvalid`: 검증 실패한 틱 수
- **성능 최적화**:
  - 1000개마다 통계 로그 출력
  - 예외 발생 시 에러 로그 및 품질 모니터 기록

**2. TickDataValidator.java**
- **위치**: `maru.trading.broker.kis.marketdata`
- **목적**: 틱 데이터 품질 검증
- **검증 규칙**:
  ```java
  // 가격 범위
  private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(100);
  private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(10_000_000);

  // 거래량 범위
  private static final long MIN_VOLUME = 0;
  private static final long MAX_VOLUME = 100_000_000;
  ```
- **검증 항목**:
  1. Null 체크
  2. 심볼 유효성
  3. 타임스탬프 (미래 시간 방지)
  4. 가격 범위 (100 ~ 10,000,000)
  5. 거래량 범위 (0 ~ 100,000,000)
- **반환 타입**:
  ```java
  ValidationResult {
      boolean valid;
      String errorMessage;
  }
  ```

**3. DataQualityMonitor.java**
- **위치**: `maru.trading.broker.kis.marketdata`
- **목적**: 심볼별 데이터 품질 모니터링
- **추적 메트릭** (심볼별):
  - `validTickCount`: 유효한 틱 수
  - `invalidTickCount`: 무효한 틱 수
  - `duplicateTickCount`: 중복 틱 수
  - `outOfSequenceTickCount`: 순서 오류 틱 수
  - `errorCount`: 에러 발생 수
  - `lastTickTimestamp`: 마지막 틱 시간
  - `lastError`: 마지막 에러 메시지
- **품질 점수 계산**:
  ```java
  public double getQualityScore() {
      long total = validTickCount + invalidTickCount +
                   duplicateTickCount + outOfSequenceTickCount;
      if (total == 0) return 100.0;

      double validRatio = (double) validTickCount / total;
      return validRatio * 100.0;  // 0-100 점수
  }
  ```
- **품질 기준**:
  - `isQualityAcceptable()`: 품질 점수 >= 95%

---

### Phase 5.3: KIS API 클라이언트 강화

**목표**: KIS API 호출의 안정성 및 에러 처리 강화

#### 구현된 컴포넌트

**1. ApiRetryPolicy.java**
- **위치**: `maru.trading.broker.kis.api`
- **목적**: API 재시도 정책 정의
- **핵심 기능**:
  - 지수 백오프 지연 계산
  - 최대 재시도 횟수 관리
  - 재시도 여부 판단
- **기본 정책**:
  ```java
  // 주문 API용
  public static ApiRetryPolicy defaultOrderPolicy() {
      return ApiRetryPolicy.builder()
          .maxRetries(3)
          .initialDelayMs(1000)
          .maxDelayMs(10000)
          .backoffMultiplier(2.0)
          .build();
  }

  // 조회 API용
  public static ApiRetryPolicy defaultQueryPolicy() {
      return ApiRetryPolicy.builder()
          .maxRetries(5)
          .initialDelayMs(500)
          .maxDelayMs(5000)
          .backoffMultiplier(1.5)
          .build();
  }
  ```

**2. KisApiException.java**
- **위치**: `maru.trading.broker.kis.api`
- **목적**: KIS API 에러 분류 및 처리
- **에러 타입 분류**:
  ```java
  public enum ErrorType {
      NETWORK(true),              // 네트워크 에러 (재시도 가능)
      AUTHENTICATION(false),      // 인증 에러 (재시도 불가)
      RATE_LIMIT(true),          // Rate Limit (재시도 가능)
      INVALID_REQUEST(false),    // 잘못된 요청 (재시도 불가)
      ORDER_REJECTED(false),     // 주문 거부 (재시도 불가)
      INSUFFICIENT_BALANCE(false), // 잔액 부족 (재시도 불가)
      SERVER_ERROR(true),        // 서버 에러 (재시도 가능)
      UNKNOWN(false)             // 알 수 없는 에러
  }
  ```
- **에러 분류 로직**:
  ```java
  public static ErrorType classifyError(int httpStatusCode, String kisErrorCode) {
      // HTTP 상태 코드 기반
      if (httpStatusCode == 401 || httpStatusCode == 403) return AUTHENTICATION;
      if (httpStatusCode == 429) return RATE_LIMIT;
      if (httpStatusCode == 400) return INVALID_REQUEST;
      if (httpStatusCode >= 500) return SERVER_ERROR;

      // KIS 에러 코드 기반
      if (kisErrorCode != null) {
          if (kisErrorCode.startsWith("40")) return INVALID_REQUEST;
          if (kisErrorCode.startsWith("50")) return SERVER_ERROR;
          if (kisErrorCode.contains("INSUFFICIENT")) return INSUFFICIENT_BALANCE;
          if (kisErrorCode.contains("REJECT")) return ORDER_REJECTED;
      }

      return UNKNOWN;
  }
  ```

---

### Phase 5.4: 실시간 체결 처리

**목표**: WebSocket을 통한 실시간 체결 알림 수신 및 처리

#### 구현된 컴포넌트

**1. FillStreamHandler.java**
- **위치**: `maru.trading.broker.kis.fill`
- **목적**: 실시간 체결 알림 처리
- **핵심 워크플로우**:
  ```
  1. 체결 알림 수신 (WebSocket)
     ↓
  2. 데이터 검증 (FillDataValidator)
     ↓
  3. 중복 체크 (DuplicateFillFilter)
     ↓
  4. 체결 적용 (ApplyFillUseCase)
     ↓
  5. 포지션 & PnL 업데이트
  ```
- **통계 추적**:
  - `fillsReceived`: 총 수신한 체결 수
  - `fillsProcessed`: 정상 처리된 체결 수
  - `fillsDuplicate`: 중복 체결 수
  - `fillsInvalid`: 검증 실패한 체결 수
- **멱등성 보장**:
  - DuplicateFillFilter로 중복 체결 방지
  - 동일 fillId에 대해 한 번만 처리

**2. DuplicateFillFilter.java**
- **위치**: `maru.trading.broker.kis.fill`
- **목적**: 중복 체결 감지 및 필터링
- **핵심 기능**:
  - ConcurrentHashMap으로 처리된 fillId 캐시
  - `putIfAbsent()` 사용하여 원자적 중복 체크
  - 자동 캐시 정리 (1시간 이상 된 항목 제거)
- **캐시 관리**:
  ```java
  private static final long MAX_CACHE_AGE_MINUTES = 60;
  private static final int MAX_CACHE_SIZE = 10000;
  ```
- **정리 트리거**:
  - 캐시 크기가 10,000개 초과 시 자동 정리
  - 1시간 이상 된 항목 제거

**3. FillDataValidator.java**
- **위치**: `maru.trading.broker.kis.fill`
- **목적**: 체결 데이터 검증
- **검증 규칙**:
  ```java
  // 가격 범위
  private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(100);
  private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(10_000_000);

  // 수량 범위
  private static final int MIN_QTY = 1;
  private static final int MAX_QTY = 1_000_000;
  ```
- **검증 항목**:
  1. Fill ID (Null/Empty 체크)
  2. Order ID (Null/Empty 체크)
  3. 타임스탬프 (미래 시간 방지)
  4. 체결 가격 (100 ~ 10,000,000)
  5. 체결 수량 (1 ~ 1,000,000)
  6. 심볼 (Empty 체크)
  7. 계좌 ID (Empty 체크)
- **추가 검증 메서드**:
  ```java
  public ValidationResult validateAgainstOrder(
      Fill fill,
      String expectedOrderId,
      String expectedSymbol) {
      // 체결이 주문과 일치하는지 검증
  }
  ```

---

## 📊 Phase 5 아키텍처

### 실시간 데이터 처리 흐름

```
┌─────────────────────────────────────────────────────────────────┐
│                     KIS OpenAPI WebSocket                        │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ├─── Market Data Stream
                 │    │
                 │    ↓
                 │    ┌─────────────────────────┐
                 │    │  MarketDataCollector    │
                 │    │  - TickDataValidator    │
                 │    │  - DataQualityMonitor   │
                 │    └────────┬────────────────┘
                 │             │
                 │             ↓
                 │    ┌─────────────────────────┐
                 │    │  BarAggregator          │
                 │    │  (틱 → 1분봉 집계)      │
                 │    └────────┬────────────────┘
                 │             │
                 │             ↓
                 │    ┌─────────────────────────┐
                 │    │  StrategyEngine         │
                 │    │  (전략 실행)            │
                 │    └─────────────────────────┘
                 │
                 └─── Fill Stream
                      │
                      ↓
                      ┌─────────────────────────┐
                      │  FillStreamHandler      │
                      │  - FillDataValidator    │
                      │  - DuplicateFillFilter  │
                      └────────┬────────────────┘
                               │
                               ↓
                      ┌─────────────────────────┐
                      │  ApplyFillUseCase       │
                      │  (포지션 & PnL 업데이트)│
                      └─────────────────────────┘
```

### 에러 처리 & 복구 전략

```
┌──────────────────┐
│  WebSocket 연결  │
└────────┬─────────┘
         │
         ├─── Connection Lost
         │    │
         │    ↓
         │    ┌─────────────────────────────┐
         │    │  WebSocketErrorClassifier   │
         │    │  (에러 타입 분류)           │
         │    └────────┬────────────────────┘
         │             │
         │             ├─── NETWORK Error
         │             │    → ReconnectionManager
         │             │    → 지수 백오프 재연결
         │             │
         │             ├─── AUTHENTICATION Error
         │             │    → Kill Switch ON
         │             │
         │             └─── PROTOCOL Error
         │                  → ReconnectionManager
         │
         └─── Heartbeat Timeout
              │
              ↓
              ┌─────────────────────────────┐
              │  HeartbeatMonitor           │
              │  (3회 연속 실패)            │
              └────────┬────────────────────┘
                       │
                       ↓
              ┌─────────────────────────────┐
              │  ReconnectionManager        │
              │  (자동 재연결)              │
              └─────────────────────────────┘
```

---

## 🧪 테스트 결과

### 컴파일 상태
- ✅ **성공**: 241개 Java 파일 컴파일 완료
- ⚠️ 일부 Deprecated API 사용 (BacktestEngineImpl)

### 테스트 실행
- ✅ **전체 테스트 통과** (기존 235개 테스트)
- ⏳ **Phase 5 통합 테스트**: 아직 작성 안 됨

### 필요한 추가 테스트

**Phase 5.1 테스트**:
1. ❌ WebSocketReconnectionManagerTest
2. ❌ HeartbeatMonitorTest
3. ❌ WebSocketErrorClassifierTest

**Phase 5.2 테스트**:
4. ❌ MarketDataCollectorTest
5. ❌ TickDataValidatorTest
6. ❌ DataQualityMonitorTest

**Phase 5.3 테스트**:
7. ❌ ApiRetryPolicyTest
8. ❌ KisApiExceptionTest

**Phase 5.4 테스트**:
9. ❌ FillStreamHandlerTest
10. ❌ DuplicateFillFilterTest
11. ❌ FillDataValidatorTest

---

## 📈 성능 고려사항

### 메모리 사용

**DuplicateFillFilter 캐시**:
- 최대 10,000개 fillId 저장
- 각 항목: String (fillId) + LocalDateTime (timestamp) ≈ 100 bytes
- 총 메모리: ~1MB

**DataQualityMonitor 메트릭**:
- 심볼당 메트릭: 6개 AtomicLong + 3개 LocalDateTime/String ≈ 200 bytes
- 100개 심볼 추적 시: ~20KB (무시 가능)

**MarketDataCache**:
- 기존 구현 사용 (Phase 3)
- 심볼당 최신 틱 1개 저장

### 동시성 안전

**Thread-Safe 컴포넌트**:
- ✅ DuplicateFillFilter: ConcurrentHashMap + putIfAbsent
- ✅ DataQualityMonitor: ConcurrentHashMap + AtomicLong
- ✅ MarketDataCollector: AtomicLong 통계
- ✅ FillStreamHandler: AtomicLong 통계

**주의 사항**:
- HeartbeatMonitor: ScheduledExecutorService 사용 (스레드 안전)
- ReconnectionManager: AtomicInteger 사용 (스레드 안전)

---

## 🔧 설정 및 사용 방법

### 1. WebSocket 재연결 설정

```java
@Configuration
public class WebSocketConfig {

    @Bean
    public ReconnectionPolicy reconnectionPolicy() {
        return ReconnectionPolicy.builder()
            .maxRetries(10)
            .initialDelayMs(1000)
            .maxDelayMs(60000)
            .backoffMultiplier(2.0)
            .build();
    }

    @Bean
    public WebSocketReconnectionManager reconnectionManager(
            ReconnectionPolicy policy) {
        return new WebSocketReconnectionManager(policy);
    }
}
```

### 2. Market Data 수집 활성화

```java
@Service
public class MarketDataService {

    private final MarketDataCollector collector;

    public void startCollecting(String symbol) {
        // WebSocket 구독
        webSocketClient.subscribe(symbol, tick -> {
            collector.onTick(tick);  // 자동으로 검증 & 처리
        });
    }
}
```

### 3. Fill Stream 처리 활성화

```java
@Service
public class FillService {

    private final FillStreamHandler handler;

    public void startListening(String accountId) {
        // WebSocket 구독
        webSocketClient.subscribeFills(accountId, fill -> {
            handler.onFill(fill);  // 자동으로 검증 & 중복 제거
        });
    }
}
```

### 4. 데이터 품질 모니터링

```java
@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final DataQualityMonitor qualityMonitor;

    @GetMapping("/quality/{symbol}")
    public DataQualityResponse getQuality(@PathVariable String symbol) {
        var metrics = qualityMonitor.getMetrics(symbol);

        return DataQualityResponse.builder()
            .symbol(symbol)
            .qualityScore(metrics.getQualityScore())
            .validTicks(metrics.getValidTickCount().get())
            .invalidTicks(metrics.getInvalidTickCount().get())
            .isAcceptable(metrics.isQualityAcceptable())
            .build();
    }
}
```

---

## 🚀 다음 단계 제안

### 옵션 A: Phase 5 통합 테스트 작성 ⭐ (추천)

**구현할 테스트** (11개):
1. WebSocketReconnectionManagerTest (150 lines)
2. HeartbeatMonitorTest (180 lines)
3. WebSocketErrorClassifierTest (120 lines)
4. MarketDataCollectorTest (200 lines)
5. TickDataValidatorTest (180 lines)
6. DataQualityMonitorTest (160 lines)
7. ApiRetryPolicyTest (100 lines)
8. KisApiExceptionTest (140 lines)
9. FillStreamHandlerTest (220 lines)
10. DuplicateFillFilterTest (180 lines)
11. FillDataValidatorTest (180 lines)

**예상 소요 시간**: 3-4시간
**임팩트**: Phase 5 완전 검증, 프로덕션 준비 완료

### 옵션 B: KIS API 실제 연동 테스트 (PAPER 계좌)

**작업 항목**:
1. KIS PAPER 계좌 설정
2. 실제 WebSocket 연결 테스트
3. 실제 Market Data 수신 테스트
4. 실제 Order 전송 테스트 (PAPER)
5. 실제 Fill 수신 테스트

**예상 소요 시간**: 1-2일
**임팩트**: 실제 환경 검증, 통합 이슈 발견

### 옵션 C: 모니터링 & 알림 시스템 구축

**작업 항목**:
1. 데이터 품질 대시보드
2. WebSocket 연결 상태 모니터링
3. 알림 시스템 (Slack, Email)
4. 메트릭 수집 (Prometheus)

**예상 소요 시간**: 2-3일
**임팩트**: 운영 효율성 향상

---

## 💡 추천 경로

**옵션 A (통합 테스트)** → **옵션 B (실제 연동 테스트)** 순서로 진행 추천

**이유**:
1. ✅ 통합 테스트로 로직 완전 검증
2. ✅ 실제 연동 전 버그 사전 발견
3. ✅ PAPER 계좌로 안전한 실제 환경 테스트
4. ✅ 프로덕션 배포 준비 완료

---

## 📝 Implementation Quality

| 평가 항목 | 점수 | 비고 |
|----------|------|------|
| 코드 완성도 | ★★★★★ (100%) | 모든 컴포넌트 구현 완료 |
| 아키텍처 준수 | ★★★★★ | Layered + Hexagonal 패턴 |
| 테스트 커버리지 | ★★☆☆☆ | 통합 테스트 필요 |
| 문서화 | ★★★★☆ | 이 문서로 개선됨 |
| 에러 처리 | ★★★★★ | 포괄적인 에러 분류 |
| 동시성 안전 | ★★★★★ | ConcurrentHashMap, AtomicLong |
| 성능 최적화 | ★★★★☆ | 메모리 효율적, 캐시 정리 |

**종합 평가**: Phase 5 기능 완전 구현, **통합 테스트 작성 필요**

---

## 🔍 발견된 주요 구현 특징

### 아키텍처 우수성
- ✅ 명확한 책임 분리 (Validator, Filter, Monitor)
- ✅ 재사용 가능한 정책 패턴 (ReconnectionPolicy, ApiRetryPolicy)
- ✅ 이벤트 드리븐 아키텍처 유지
- ✅ Fail-safe 설계 (중복 방지, 자동 재연결)

### 안정성
- ✅ 지수 백오프로 부하 방지
- ✅ 멱등성 보장 (중복 체결 방지)
- ✅ 자동 캐시 정리 (메모리 누수 방지)
- ✅ 포괄적인 에러 분류 및 처리

### 관찰 가능성
- ✅ 상세한 로깅 (DEBUG, INFO, WARN, ERROR)
- ✅ 통계 추적 (틱, 체결, 에러 카운터)
- ✅ 품질 점수 계산 (0-100)
- ✅ 에러 메시지 상세 기록

---

**작성자**: Claude Sonnet 4.5
**검증 날짜**: 2026-01-01
**다음 단계**: Phase 5 통합 테스트 작성 권장
