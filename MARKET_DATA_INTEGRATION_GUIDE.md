# 시장 데이터 연동 구현 가이드

**작성일**: 2025-12-31
**목표**: KIS WebSocket을 통한 실시간 시세 연동
**현재 상태**: STUB 모드 → LIVE 모드 전환 필요

---

## 📊 현재 구현 상태

### ✅ 이미 구현된 컴포넌트

1. **MarketTick** (`domain/market/MarketTick.java`)
   - 틱 데이터 도메인 모델
   - 가격, 거래량, 타임스탬프, 거래 상태 포함

2. **MarketDataCache** (`infra/cache/MarketDataCache.java`)
   - 인메모리 틱 캐시 (ConcurrentHashMap)
   - 최신 틱 저장 및 조회
   - 1시간 이상 오래된 데이터는 stale 처리

3. **BarAggregator** (`application/orchestration/BarAggregator.java`)
   - 틱 → 1분봉 자동 집계
   - 분 경계 감지 및 바 닫기
   - DB 저장 + BarCache 캐싱

4. **KisWebSocketClient** (`broker/kis/ws/KisWebSocketClient.java`)
   - BrokerStream 포트 구현
   - **현재: STUB 모드** (5초마다 랜덤 틱 생성)
   - subscribeTicks(), subscribeFills() 메서드 제공

5. **BrokerStream** (`application/ports/broker/BrokerStream.java`)
   - 실시간 데이터 스트림 포트 인터페이스

---

## 🚀 구현 방안

### **방안 A: MarketDataService 추가 (권장)**

전략에 등록된 종목을 자동으로 구독하고 관리하는 서비스

#### **1. MarketDataService 생성**

**위치**: `src/main/java/maru/trading/application/service/MarketDataService.java`

**역할**:
- 전략에 등록된 모든 종목 자동 구독
- KisWebSocketClient를 통해 실시간 시세 수신
- MarketDataCache에 틱 저장
- BarAggregator로 틱 전달

**구현**:
```java
@Service
public class MarketDataService {

    private final BrokerStream brokerStream;
    private final MarketDataCache marketDataCache;
    private final BarAggregator barAggregator;
    private final StrategyRepository strategyRepository;

    private String activeSubscriptionId;

    @PostConstruct
    public void init() {
        // 전략에서 사용 중인 모든 종목 조회
        Set<String> symbols = getAllActiveSymbols();

        // 실시간 시세 구독
        subscribeToMarketData(symbols);
    }

    private void subscribeToMarketData(Set<String> symbols) {
        log.info("Subscribing to market data for {} symbols", symbols.size());

        activeSubscriptionId = brokerStream.subscribeTicks(
            new ArrayList<>(symbols),
            this::onTickReceived
        );
    }

    private void onTickReceived(MarketTick tick) {
        // 1. 캐시에 저장
        marketDataCache.put(tick);

        // 2. 바 집계기로 전달
        barAggregator.onTick(tick);

        log.trace("Processed tick: symbol={}, price={}",
            tick.getSymbol(), tick.getPrice());
    }

    private Set<String> getAllActiveSymbols() {
        // 모든 활성 전략의 종목 수집
        List<Strategy> strategies = strategyRepository.findActiveStrategies();
        return strategies.stream()
            .flatMap(s -> s.getSymbols().stream())
            .collect(Collectors.toSet());
    }
}
```

#### **2. Strategy에 symbol 목록 추가**

**위치**: `src/main/java/maru/trading/domain/strategy/Strategy.java`

```java
public class Strategy {
    private String strategyId;
    private String name;
    private StrategyStatus status;
    private List<String> symbols; // 추가: 전략이 거래할 종목 목록
    // ...
}
```

#### **3. StrategyRepository에 메서드 추가**

```java
public interface StrategyRepository {
    List<Strategy> findActiveStrategies();
    // ...
}
```

---

### **방안 B: KIS WebSocket LIVE 모드 구현**

현재 STUB 모드를 실제 KIS API 연동으로 전환

#### **1. KIS WebSocket 메시지 파서 생성**

**위치**: `src/main/java/maru/trading/broker/kis/ws/KisWebSocketMessageParser.java`

**KIS 실시간 시세 메시지 형식**:
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
      "STCK_CNTG_HOUR": "153000",   // 체결시간
      "ASKP1": "72100",             // 매도호가1
      "BIDP1": "72000"              // 매수호가1
    }
  }
}
```

**구현**:
```java
@Component
public class KisWebSocketMessageParser {

    private final ObjectMapper objectMapper;

    public MarketTick parseTickMessage(String message) {
        try {
            KisTickMessage kisMsg = objectMapper.readValue(message, KisTickMessage.class);

            String symbol = kisMsg.getBody().getOutput().getMKSC_SHRN_ISCD();
            BigDecimal price = new BigDecimal(kisMsg.getBody().getOutput().getSTCK_PRPR());
            long volume = Long.parseLong(kisMsg.getBody().getOutput().getCNTG_VOL());
            LocalDateTime timestamp = parseKisTime(kisMsg.getBody().getOutput().getSTCK_CNTG_HOUR());

            return new MarketTick(symbol, price, volume, timestamp, "NORMAL");

        } catch (Exception e) {
            log.error("Failed to parse KIS tick message", e);
            return null;
        }
    }

    private LocalDateTime parseKisTime(String kisTime) {
        // "153000" -> LocalDateTime with current date + 15:30:00
        LocalDate today = LocalDate.now();
        int hour = Integer.parseInt(kisTime.substring(0, 2));
        int minute = Integer.parseInt(kisTime.substring(2, 4));
        int second = Integer.parseInt(kisTime.substring(4, 6));
        return LocalDateTime.of(today, LocalTime.of(hour, minute, second));
    }
}
```

#### **2. WebSocketConnectionManager LIVE 구현**

**위치**: `src/main/java/maru/trading/broker/kis/ws/WebSocketConnectionManager.java`

**KIS WebSocket 엔드포인트**:
- 실전투자: `wss://ops.koreainvestment.com:21000`
- 모의투자: `wss://ops.koreainvestment.com:31000`

**구현**:
```java
@Component
public class WebSocketConnectionManager {

    private final KisProperties kisProperties;
    private WebSocketClient webSocketClient;
    private boolean connected = false;

    @PostConstruct
    public void connect() {
        String wsUrl = kisProperties.getWebSocketUrl();

        try {
            webSocketClient = new WebSocketClient(URI.create(wsUrl));
            webSocketClient.setConnectionLostTimeout(30);

            webSocketClient.addMessageListener(this::onMessage);
            webSocketClient.connectBlocking();

            // KIS 인증 메시지 전송
            sendAuthMessage();

            connected = true;
            log.info("Connected to KIS WebSocket: {}", wsUrl);

        } catch (Exception e) {
            log.error("Failed to connect to KIS WebSocket", e);
            connected = false;
        }
    }

    private void sendAuthMessage() {
        // KIS 인증 메시지 구성
        Map<String, Object> authMsg = Map.of(
            "header", Map.of(
                "approval_key", kisProperties.getApprovalKey(),
                "custtype", "P",
                "tr_type", "1",
                "content-type", "utf-8"
            )
        );

        send(objectMapper.writeValueAsString(authMsg));
    }

    public void subscribe(List<String> symbols) {
        for (String symbol : symbols) {
            Map<String, Object> subMsg = Map.of(
                "header", Map.of(
                    "tr_id", "H0STCNT0",  // 실시간 시세 TR_ID
                    "tr_key", symbol
                ),
                "body", Map.of(
                    "tr_cd", "1"  // 등록
                )
            );

            send(objectMapper.writeValueAsString(subMsg));
            log.info("Subscribed to symbol: {}", symbol);
        }
    }

    public void send(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
        }
    }

    private void onMessage(String message) {
        messageHandler.handleMessage(message);
    }

    public boolean isConnected() {
        return connected && webSocketClient != null && webSocketClient.isOpen();
    }

    @PreDestroy
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            connected = false;
            log.info("Disconnected from KIS WebSocket");
        }
    }
}
```

#### **3. KisWebSocketMessageHandler LIVE 구현**

**위치**: `src/main/java/maru/trading/broker/kis/ws/KisWebSocketMessageHandler.java`

```java
@Component
public class KisWebSocketMessageHandler {

    private final KisWebSocketMessageParser parser;
    private final Map<String, Consumer<MarketTick>> tickHandlers = new ConcurrentHashMap<>();

    public void registerTickHandler(String subscriptionId, Consumer<MarketTick> handler) {
        tickHandlers.put(subscriptionId, handler);
    }

    public void handleMessage(String message) {
        try {
            MarketTick tick = parser.parseTickMessage(message);

            if (tick != null) {
                tick.validate();

                // 모든 구독자에게 틱 전달
                tickHandlers.values().forEach(handler -> {
                    try {
                        handler.accept(tick);
                    } catch (Exception e) {
                        log.error("Error in tick handler", e);
                    }
                });
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }
}
```

---

### **방안 C: 재연결 로직 추가**

WebSocket 연결이 끊겼을 때 자동 재연결

#### **WebSocketReconnectionService**

```java
@Service
public class WebSocketReconnectionService {

    private final WebSocketConnectionManager connectionManager;
    private final MarketDataService marketDataService;

    @Scheduled(fixedDelay = 10000) // 10초마다 체크
    public void checkConnection() {
        if (!connectionManager.isConnected()) {
            log.warn("WebSocket disconnected, attempting reconnection...");

            try {
                connectionManager.disconnect();
                Thread.sleep(2000);
                connectionManager.connect();

                // 재구독
                marketDataService.resubscribe();

                log.info("WebSocket reconnection successful");

            } catch (Exception e) {
                log.error("WebSocket reconnection failed", e);
            }
        }
    }
}
```

---

## 📋 구현 체크리스트

### Phase 1: MarketDataService 추가
- [ ] `MarketDataService.java` 생성
- [ ] `Strategy`에 symbols 필드 추가
- [ ] `StrategyRepository.findActiveStrategies()` 구현
- [ ] Spring Bean 등록 확인
- [ ] 초기화 시 종목 자동 구독 테스트

### Phase 2: KIS WebSocket LIVE 모드
- [ ] `KisWebSocketMessageParser.java` 생성
- [ ] `WebSocketConnectionManager.java` LIVE 구현
- [ ] `KisWebSocketMessageHandler.java` LIVE 구현
- [ ] KIS approval key 발급 및 설정
- [ ] WebSocket 연결 테스트 (모의투자)

### Phase 3: 재연결 및 에러 처리
- [ ] `WebSocketReconnectionService.java` 생성
- [ ] 연결 끊김 감지 로직
- [ ] 자동 재연결 로직
- [ ] 재구독 로직
- [ ] 에러 알림 (Outbox 이벤트)

### Phase 4: 모니터링 및 디버깅
- [ ] 틱 수신 로그 확인
- [ ] 바 생성 로그 확인
- [ ] 전략 실행 로그 확인
- [ ] MarketDataCache 상태 확인 API
- [ ] BarAggregator 상태 확인 API

---

## 🔍 데이터 흐름

### STUB 모드 (현재)
```
KisWebSocketClient.simulateTickEvents() (5초마다)
  → MarketTick 랜덤 생성
  → TickSubscription.handler.accept(tick)
  → (구독자 없음)
```

### LIVE 모드 (목표)
```
KIS WebSocket 서버 → WebSocket 메시지 수신
  ↓
WebSocketConnectionManager.onMessage()
  ↓
KisWebSocketMessageHandler.handleMessage()
  ↓
KisWebSocketMessageParser.parseTickMessage()
  ↓
MarketTick 객체 생성
  ↓
MarketDataService.onTickReceived()
  ├─ MarketDataCache.put(tick) ✓
  └─ BarAggregator.onTick(tick)
       ├─ 1분 경계 체크
       ├─ 바 닫기 (필요 시)
       ├─ DB 저장 (BarRepository)
       └─ 캐시 저장 (BarCache)
  ↓
StrategyScheduler (1분마다)
  → ExecuteStrategyUseCase
  → LoadStrategyContextUseCase (최근 N개 바 조회)
  → StrategyEngine.evaluate()
  → SignalDecision
  → GenerateSignalUseCase
  → TradingWorkflow.processSignal()
  → PlaceOrderUseCase
```

---

## 🎯 우선순위 제안

### 1단계: MarketDataService 추가 (1-2시간)
- 가장 간단하고 효과적
- STUB 모드에서도 동작 확인 가능
- 전략-종목 연결 구조 확립

### 2단계: KIS WebSocket 파서 구현 (2-3시간)
- KIS 메시지 형식 파악
- 파서 구현 및 단위 테스트
- MarketTick 변환 검증

### 3단계: WebSocket 연결 LIVE 전환 (3-4시간)
- 모의투자 계정으로 테스트
- 인증 및 구독 메시지 구현
- 실시간 시세 수신 확인

### 4단계: 재연결 및 안정화 (2-3시간)
- 재연결 로직 구현
- 에러 처리 강화
- 모니터링 대시보드 추가

**총 예상 시간**: 8-12시간

---

## 📚 참고 자료

### KIS OpenAPI 문서
- 실시간 시세 가이드: `https://apiportal.koreainvestment.com/websocket/overview`
- TR_ID 목록: `H0STCNT0` (실시간 체결), `H0STASP0` (실시간 호가)
- Approval Key 발급: API 관리 > 승인키 발급

### 현재 코드 위치
- `MarketTick`: `src/main/java/maru/trading/domain/market/MarketTick.java`
- `MarketDataCache`: `src/main/java/maru/trading/infra/cache/MarketDataCache.java`
- `BarAggregator`: `src/main/java/maru/trading/application/orchestration/BarAggregator.java`
- `KisWebSocketClient`: `src/main/java/maru/trading/broker/kis/ws/KisWebSocketClient.java`

---

## 🚨 주의사항

1. **모의투자로 먼저 테스트**: 실전 계좌 연동 전 반드시 모의투자 환경에서 검증
2. **Approval Key 보안**: application.yml에 직접 입력 금지, 환경변수 사용
3. **재연결 간격**: 너무 짧으면 KIS 서버 부하, 10초 이상 권장
4. **에러 처리**: WebSocket 에러 발생 시 로그 + Outbox 이벤트 발행
5. **시장 시간 체크**: 장 마감 시 구독 해제 필요 (리소스 절약)

---

**작성자**: Claude Sonnet 4.5
**다음 단계**: MarketDataService 구현 시작
