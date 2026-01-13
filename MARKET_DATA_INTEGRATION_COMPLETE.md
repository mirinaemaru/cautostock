# 시장 데이터 연동 구현 완료

**작성일**: 2025-12-31
**목표**: KIS WebSocket을 통한 실시간 시세 연동
**상태**: ✅ **완료** (Stage 1-4 모두 구현)

---

## 📊 구현 완료 단계

### ✅ **Stage 1: MarketDataService 추가**

**구현 파일**:
- `src/main/java/maru/trading/application/service/MarketDataService.java`

**기능**:
- 설정된 종목 자동 구독 (`@PostConstruct`)
- 틱 수신 후 MarketDataCache + BarAggregator로 라우팅
- 종목 추가/제거 동적 지원
- 재구독 메서드 (`resubscribe()`)

**설정**:
```yaml
trading:
  market-data:
    symbols: "005930,035420,000660,051910,005380"
    mode: STUB  # or LIVE
```

**데이터 흐름**:
```
BrokerStream.subscribeTicks()
  → MarketDataService.onTickReceived()
  → 1. MarketDataCache.put(tick)
  → 2. BarAggregator.onTick(tick)
```

---

### ✅ **Stage 2: KIS WebSocket 파서 구현**

**구현 파일**:
- `src/main/java/maru/trading/broker/kis/ws/dto/KisTickMessage.java` (DTO)
- `src/main/java/maru/trading/broker/kis/ws/KisWebSocketMessageParser.java` (파서)
- `src/main/java/maru/trading/broker/kis/ws/KisWebSocketMessageHandler.java` (업데이트)

**기능**:
- 2가지 메시지 포맷 지원:
  - **JSON 포맷**: `{"header":{...}, "body":{...}}`
  - **Delimited 포맷**: `"0|H0STCNT0|005930|72000|100|153000|..."`
- KIS 시간 형식 파싱 (`HHMMSS` → `LocalDateTime`)
- 거래 상태 코드 매핑 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
- STUB/LIVE 모드 자동 감지

**메시지 예시**:
```json
{
  "header": {
    "tr_id": "H0STCNT0",
    "encrypt": "N"
  },
  "body": {
    "rt_cd": "0",
    "msg_cd": "OPSP0000",
    "msg1": "정상처리",
    "output": {
      "MKSC_SHRN_ISCD": "005930",  // 종목코드
      "STCK_PRPR": "72000",         // 현재가
      "CNTG_VOL": "100",            // 체결량
      "STCK_CNTG_HOUR": "153000",   // 체결시간 (15:30:00)
      "PRDY_VRSS_SIGN": "2"         // 전일대비부호
    }
  }
}
```

---

### ✅ **Stage 3: WebSocket LIVE 연결**

**구현 파일**:
- `src/main/java/maru/trading/broker/kis/ws/WebSocketConnectionManager.java` (완전 재작성)
- `src/main/java/maru/trading/broker/kis/config/KisProperties.java` (중첩 구조로 재구성)
- `src/main/resources/application.yml` (WebSocket URL 추가)
- `src/main/java/maru/trading/broker/kis/ws/KisWebSocketClient.java` (통합)
- `src/main/java/maru/trading/infra/scheduler/TokenRefreshScheduler.java` (업데이트)

**기능**:
- Java 11+ `HttpClient.newWebSocketBuilder()` 사용
- `WebSocket.Listener` 인터페이스 구현
- KIS 인증 메시지 전송 (`approval_key`)
- 실시간 시세 구독 메시지 (`H0STCNT0`)
- 구독/구독취소 메시지 전송
- 연결 상태 추적 및 콜백
- STUB/LIVE 모드 자동 감지

**WebSocket URL 설정**:
```yaml
trading:
  broker:
    kis:
      paper:
        base-url: https://openapivts.koreainvestment.com:29443
        ws-url: wss://ops.koreainvestment.com:31000  # 모의투자
        app-key: ${KIS_PAPER_APP_KEY:}
        app-secret: ${KIS_PAPER_APP_SECRET:}
      live:
        base-url: https://openapi.koreainvestment.com:9443
        ws-url: wss://ops.koreainvestment.com:21000  # 실전투자
        app-key: ${KIS_LIVE_APP_KEY:}
        app-secret: ${KIS_LIVE_APP_SECRET:}
```

**KisProperties 구조**:
```java
@ConfigurationProperties(prefix = "trading.broker.kis")
public class KisProperties {
    private EnvironmentConfig paper;
    private EnvironmentConfig live;
    private TokenConfig token;
    private WebSocketConfig ws;

    public static class EnvironmentConfig {
        private String baseUrl;
        private String wsUrl;
        private String appKey;
        private String appSecret;
    }
}
```

**WebSocket 연결 흐름**:
```
@PostConstruct init() (LIVE 모드만)
  → connect()
  → HttpClient.newWebSocketBuilder().buildAsync()
  → onOpen() callback
  → sendAuthMessage()
  → WebSocket CONNECTED
  → subscribe(symbols)
  → 실시간 시세 수신 시작
```

**메시지 수신 흐름**:
```
onText(webSocket, data, last)
  → KisWebSocketMessageHandler.handleMessage()
  → KisWebSocketMessageParser.parseTickMessage()
  → MarketTick 생성
  → TickSubscription.handler.accept(tick)
  → MarketDataService.onTickReceived()
  → MarketDataCache + BarAggregator
```

---

### ✅ **Stage 4: 재연결 로직 추가**

**구현 파일**:
- `src/main/java/maru/trading/broker/kis/ws/WebSocketReconnectionService.java` (신규)

**기능**:
- **주기적 연결 체크**: `@Scheduled(fixedDelay = 10000)` (10초마다)
- **자동 재연결**: 연결 끊김 감지 시 자동 reconnect 시도
- **재구독**: 재연결 성공 시 `MarketDataService.resubscribe()` 호출
- **상태 변화 로깅**: CONNECTED ↔ DISCONNECTED 전환 시 로그
- **강제 재연결 API**: `forceReconnect()` (관리자/테스트용)
- **LIVE 모드 전용**: STUB 모드에서는 동작 안 함

**재연결 로직**:
```java
@Scheduled(fixedDelay = 10000)
public void checkConnectionHealth() {
    if (!connectionManager.isConnected()) {
        log.warn("WebSocket disconnected, attempting reconnection...");

        connectionManager.connect();

        if (connectionManager.isConnected()) {
            log.info("Reconnection successful");
            marketDataService.resubscribe();  // 재구독
        }
    }
}
```

**Exponential Backoff** (WebSocketConnectionManager 내장):
- 재연결 시도: 1초 → 2초 → 4초 → 8초 → ... → 최대 30초
- 최대 재시도 횟수: 10회 (설정 가능)

---

## 🔍 전체 데이터 흐름

### STUB 모드 (기존)
```
@Scheduled simulateTickEvents() (5초마다)
  → generateSimulatedTick()
  → TickSubscription.handler.accept(tick)
  → MarketDataService.onTickReceived()
  → MarketDataCache.put(tick)
  → BarAggregator.onTick(tick)
```

### LIVE 모드 (신규)
```
[KIS WebSocket 서버]
  ↓ (WebSocket message)
WebSocketConnectionManager.onText()
  ↓
KisWebSocketMessageHandler.handleMessage()
  ↓
KisWebSocketMessageParser.parseTickMessage()
  ↓ (JSON/Delimited 파싱)
MarketTick 생성
  ↓
TickSubscription.handler.accept(tick)
  ↓
MarketDataService.onTickReceived()
  ├─ MarketDataCache.put(tick)
  └─ BarAggregator.onTick(tick)
       ├─ 1분 경계 체크
       ├─ Bar.close()
       ├─ BarRepository.save(bar)
       └─ BarCache.put(bar)
  ↓
StrategyScheduler @Scheduled (1분마다)
  ↓
ExecuteStrategyUseCase.execute()
  ↓
StrategyEngine.evaluate()
  ↓
SignalDecision
  ↓
TradingWorkflow.processSignal()
  ↓
PlaceOrderUseCase
```

---

## 🎯 모드 전환 방법

### STUB 모드 (개발/테스트)
```yaml
trading:
  market-data:
    mode: STUB
```
- 5초마다 랜덤 틱 생성
- WebSocket 연결 안 함
- 재연결 로직 비활성화

### LIVE 모드 (모의투자/실전)
```yaml
trading:
  market-data:
    mode: LIVE

spring:
  profiles:
    active: paper  # or live
```
- 실제 KIS WebSocket 연결
- `wss://ops.koreainvestment.com:31000` (paper)
- `wss://ops.koreainvestment.com:21000` (live)
- 자동 재연결 활성화

---

## 📋 구현 체크리스트

### Stage 1: MarketDataService
- ✅ `MarketDataService.java` 생성
- ✅ Spring Bean 등록 확인
- ✅ 초기화 시 종목 자동 구독
- ✅ application.yml 설정 추가

### Stage 2: KIS WebSocket 파서
- ✅ `KisTickMessage.java` DTO 생성 (nested structure)
- ✅ `KisWebSocketMessageParser.java` 생성
- ✅ JSON + Delimited 포맷 지원
- ✅ `KisWebSocketMessageHandler` LIVE 모드 통합

### Stage 3: WebSocket LIVE 연결
- ✅ `KisProperties` 중첩 구조로 재구성
- ✅ application.yml에 ws-url 추가
- ✅ `WebSocketConnectionManager` LIVE 구현
- ✅ WebSocket.Listener 콜백 구현
- ✅ 인증 메시지 전송
- ✅ 구독/구독취소 메시지 전송
- ✅ `KisWebSocketClient` 통합
- ✅ `TokenRefreshScheduler` 업데이트

### Stage 4: 재연결 및 에러 처리
- ✅ `WebSocketReconnectionService.java` 생성
- ✅ @Scheduled 연결 체크 (10초마다)
- ✅ 자동 재연결 로직
- ✅ 재구독 로직
- ✅ Exponential backoff (내장)

---

## 🚀 실행 방법

### 1. 환경 변수 설정
```bash
export KIS_PAPER_APP_KEY="your_paper_app_key"
export KIS_PAPER_APP_SECRET="your_paper_app_secret"
```

### 2. STUB 모드로 실행 (개발/테스트)
```bash
mvn spring-boot:run
```
- 기본값은 STUB 모드
- 5초마다 시뮬레이션 틱 생성

### 3. LIVE 모드로 실행 (모의투자)
```bash
export TRADING_MARKET_DATA_MODE=LIVE
mvn spring-boot:run
```
- KIS WebSocket 연결 시도
- 실시간 시세 수신 시작

### 4. 로그 확인
```
WebSocket connection manager in LIVE mode
Connecting to KIS WebSocket: wss://ops.koreainvestment.com:31000
WebSocket connection established
Authentication message sent
LIVE: WebSocket subscription sent for symbols: [005930, 035420, ...]
Received WebSocket message: {"header":{...}, "body":{...}}
Parsing tick message in LIVE mode
Processed tick: symbol=005930, price=72000, volume=100
```

---

## 🔧 주요 설정

### application.yml
```yaml
trading:
  broker:
    kis:
      paper:
        ws-url: wss://ops.koreainvestment.com:31000
        app-key: ${KIS_PAPER_APP_KEY:}
        app-secret: ${KIS_PAPER_APP_SECRET:}
      ws:
        reconnect-delay-ms: 5000
        max-reconnect-attempts: 10

  market-data:
    symbols: "005930,035420,000660,051910,005380"
    mode: STUB
```

---

## 🚨 주의사항

1. **모의투자로 먼저 테스트**: 실전 계좌 연동 전 반드시 모의투자 환경에서 검증
2. **Approval Key 보안**: app-key/app-secret를 application.yml에 직접 입력 금지, 환경변수 사용
3. **재연결 간격**: 너무 짧으면 KIS 서버 부하, 10초 이상 권장
4. **에러 처리**: WebSocket 에러 발생 시 자동 재연결, 10회 실패 시 포기
5. **시장 시간 체크**: 장 마감 후에는 구독 해제 권장 (리소스 절약)
6. **Rate Limit**: KIS API는 초당 요청 제한이 있을 수 있음 (확인 필요)

---

## 📊 모니터링 포인트

1. **연결 상태**: `WebSocketReconnectionService.isConnected()`
2. **재연결 시도 횟수**: 로그에서 "Scheduling reconnect attempt N" 확인
3. **틱 수신 빈도**: `MarketDataService.onTickReceived()` 호출 빈도
4. **바 생성**: `BarAggregator` 1분봉 생성 확인
5. **구독 상태**: `MarketDataService.getSubscribedSymbols()` 확인

---

## 🎉 완료!

**총 예상 시간**: 8-12시간
**실제 소요 시간**: Stage 1-4 순차 구현 완료
**컴파일 상태**: ✅ BUILD SUCCESS (175 files)

**다음 단계**:
- KIS 모의투자 계정으로 LIVE 모드 테스트
- 전략 실행과 통합 테스트
- 모니터링 대시보드 추가 (선택)

---

**작성자**: Claude Sonnet 4.5
**프로젝트**: cautostock - KIS Trading System MVP
**완료일**: 2025-12-31
