# cautostock 프로젝트 전체 테스트 계획

**작성일**: 2026-01-01
**프로젝트**: KIS 자동매매 시스템 (MVP)
**테스트 전략**: 단위 → 통합 → E2E

---

## 📋 목차

1. [테스트 전략 개요](#테스트-전략-개요)
2. [테스트 환경 설정](#테스트-환경-설정)
3. [단위 테스트 (Unit Tests)](#단위-테스트)
4. [통합 테스트 (Integration Tests)](#통합-테스트)
5. [E2E 테스트 (End-to-End Tests)](#e2e-테스트)
6. [성능 테스트](#성능-테스트)
7. [테스트 우선순위](#테스트-우선순위)
8. [테스트 자동화 및 CI/CD](#테스트-자동화-및-cicd)

---

## 테스트 전략 개요

### 테스트 피라미드

```
         /\
        /  \  E2E Tests (5%)
       /____\
      /      \
     / Integration \ (25%)
    /____________\
   /              \
  /  Unit Tests    \ (70%)
 /________________\
```

### 테스트 목표

| 목표 | 설명 | 목표 수치 |
|------|------|----------|
| **코드 커버리지** | 라인 커버리지 | 80% 이상 |
| **핵심 기능 커버리지** | 비즈니스 로직 | 95% 이상 |
| **리스크 관리 커버리지** | 7개 리스크 체크 | 100% |
| **성공률** | 테스트 통과율 | 100% |
| **실행 시간** | 전체 테스트 | 5분 이내 |

### 테스트 원칙

1. **격리성 (Isolation)**: 각 테스트는 독립적으로 실행
2. **반복성 (Repeatability)**: 같은 입력 → 같은 결과
3. **자동화 (Automation)**: 모든 테스트는 자동 실행 가능
4. **빠른 피드백 (Fast Feedback)**: 단위 테스트는 1초 이내
5. **실패 시 명확한 원인**: assertion 메시지 포함

---

## 테스트 환경 설정

### 1. 테스트 프로필 구성

**`src/test/resources/application-test.yml`**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MariaDB
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  flyway:
    enabled: false  # 테스트에서는 Hibernate가 스키마 생성

trading:
  broker:
    kis:
      paper:
        base-url: http://localhost:8080/mock-kis
        ws-url: ws://localhost:8080/mock-kis-ws
        app-key: TEST_KEY
        app-secret: TEST_SECRET

  market-data:
    mode: STUB

  market:
    check-enabled: false  # 테스트 시 거래시간 체크 비활성화

  risk:
    global:
      max-position-value-per-symbol: 1000000
      max-open-orders: 5
      max-orders-per-minute: 10
      daily-loss-limit: 50000
```

### 2. 테스트 의존성 (pom.xml)

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- H2 Database (in-memory) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Testcontainers (선택) -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mariadb</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3. 테스트 베이스 클래스

**`src/test/java/maru/trading/TestBase.java`**:
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // 공통 설정
    }

    @AfterEach
    void tearDown() {
        // 공통 정리
    }
}
```

---

## 단위 테스트

### Phase 1: Domain Models (도메인 모델)

#### 1.1 Order 도메인 테스트

**파일**: `src/test/java/maru/trading/domain/order/OrderTest.java`

**테스트 케이스**:
```java
@DisplayName("Order 도메인 테스트")
class OrderTest {

    @Test
    @DisplayName("주문 생성 시 필수 필드 검증")
    void testOrderCreation() {
        // Given
        Order order = Order.builder()
            .orderId("01234567890123456789012345")
            .accountId("ACC001")
            .symbol("005930")
            .side(Side.BUY)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .build();

        // Then
        assertThat(order.getOrderId()).isNotNull();
        assertThat(order.getSide()).isEqualTo(Side.BUY);
    }

    @Test
    @DisplayName("취소 가능 상태 검증 - SENT 상태")
    void testIsCancellable_Sent() {
        // Given
        Order order = createOrder(OrderStatus.SENT);

        // When & Then
        assertThat(order.isCancellable()).isTrue();
    }

    @Test
    @DisplayName("취소 불가능 상태 검증 - FILLED 상태")
    void testIsCancellable_Filled() {
        // Given
        Order order = createOrder(OrderStatus.FILLED);

        // When & Then
        assertThat(order.isCancellable()).isFalse();
    }

    @Test
    @DisplayName("주문 취소 검증 실패 시 예외 발생")
    void testValidateCancellable_ThrowsException() {
        // Given
        Order order = createOrder(OrderStatus.FILLED);

        // When & Then
        assertThatThrownBy(() -> order.validateCancellable())
            .isInstanceOf(OrderCancellationException.class)
            .hasMessageContaining("cannot be cancelled");
    }
}
```

#### 1.2 RiskEngine 도메인 테스트

**파일**: `src/test/java/maru/trading/domain/risk/RiskEngineTest.java`

**테스트 케이스**:
```java
@DisplayName("RiskEngine 도메인 테스트")
class RiskEngineTest {

    private RiskEngine riskEngine;

    @BeforeEach
    void setUp() {
        riskEngine = new RiskEngine();
    }

    @Test
    @DisplayName("Kill Switch ON 시 주문 거부")
    void testKillSwitch_Reject() {
        // Given
        Order order = createTestOrder();
        RiskRule rule = RiskRule.defaultGlobalRule();
        RiskState state = RiskState.defaultState();
        state.toggleKillSwitch(KillSwitchStatus.ON, "Manual trigger");

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(order, rule, state);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("KILL_SWITCH");
    }

    @Test
    @DisplayName("일일 손실 한도 초과 시 거부")
    void testDailyLossLimit_Reject() {
        // Given
        Order order = createTestOrder();
        RiskRule rule = RiskRule.builder()
            .dailyLossLimit(BigDecimal.valueOf(50000))
            .build();
        RiskState state = RiskState.defaultState();
        state.updateDailyPnl(BigDecimal.valueOf(-60000)); // 손실 6만원

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(order, rule, state);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("DAILY_LOSS_LIMIT");
    }

    @Test
    @DisplayName("주문 빈도 제한 초과 시 거부")
    void testOrderFrequency_Reject() {
        // Given
        Order order = createTestOrder();
        RiskRule rule = RiskRule.builder()
            .maxOrdersPerMinute(3)
            .build();
        RiskState state = RiskState.defaultState();

        // 1분 내 3개 주문 기록
        LocalDateTime now = LocalDateTime.now();
        state.recordOrderTimestamp(now.minusSeconds(30));
        state.recordOrderTimestamp(now.minusSeconds(20));
        state.recordOrderTimestamp(now.minusSeconds(10));

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(order, rule, state);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("ORDER_FREQUENCY_LIMIT");
    }

    @Test
    @DisplayName("포지션 노출 한도 초과 시 거부")
    void testPositionExposure_Reject() {
        // Given
        Order order = Order.builder()
            .symbol("005930")
            .side(Side.BUY)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(80000))
            .build();

        RiskRule rule = RiskRule.builder()
            .maxPositionValuePerSymbol(BigDecimal.valueOf(1000000))
            .build();

        Position currentPosition = Position.builder()
            .symbol("005930")
            .qty(10) // 기존 10주
            .avgPrice(BigDecimal.valueOf(70000))
            .build();

        // 기존: 10 * 70,000 = 700,000
        // 신규: 10 * 80,000 = 800,000
        // 합계: 1,500,000 > 1,000,000 (한도 초과)

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(
            order, rule, RiskState.defaultState(), currentPosition);

        // Then
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRuleViolated()).isEqualTo("POSITION_EXPOSURE_LIMIT");
    }

    @Test
    @DisplayName("모든 체크 통과 시 승인")
    void testAllChecks_Approve() {
        // Given
        Order order = createTestOrder();
        RiskRule rule = RiskRule.defaultGlobalRule();
        RiskState state = RiskState.defaultState();

        // When
        RiskDecision decision = riskEngine.evaluatePreTrade(order, rule, state);

        // Then
        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).contains("approved");
    }
}
```

#### 1.3 MarketHoursPolicy 도메인 테스트

**파일**: `src/test/java/maru/trading/domain/market/MarketHoursPolicyTest.java`

**테스트 케이스**:
```java
@DisplayName("MarketHoursPolicy 도메인 테스트")
class MarketHoursPolicyTest {

    private MarketHoursPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MarketHoursPolicy();
    }

    @Test
    @DisplayName("정규장 시간 내 - 개장")
    void testRegularSession_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 10, 30); // 목요일 10:30
        Set<TradingSession> sessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, sessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @Test
    @DisplayName("정규장 시간 외 - 폐장")
    void testRegularSession_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 16, 0); // 목요일 16:00
        Set<TradingSession> sessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, sessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("주말 - 폐장")
    void testWeekend_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 4, 10, 0); // 토요일 10:00
        Set<TradingSession> sessions = Set.of(TradingSession.REGULAR);

        // When
        boolean isOpen = policy.isMarketOpen(time, sessions, Set.of());

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("공휴일 - 폐장")
    void testPublicHoliday_Closed() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 10, 0); // 신정
        Set<TradingSession> sessions = Set.of(TradingSession.REGULAR);
        Set<LocalDate> holidays = Set.of(LocalDate.of(2025, 1, 1));

        // When
        boolean isOpen = policy.isMarketOpen(time, sessions, holidays);

        // Then
        assertThat(isOpen).isFalse();
    }

    @Test
    @DisplayName("시간외 종가 세션 - 개장")
    void testAfterHoursClosing_Open() {
        // Given
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 15, 50); // 목요일 15:50
        Set<TradingSession> sessions = Set.of(TradingSession.AFTER_HOURS_CLOSING);

        // When
        boolean isOpen = policy.isMarketOpen(time, sessions, Set.of());

        // Then
        assertThat(isOpen).isTrue();
    }

    @ParameterizedTest
    @MethodSource("provideSessionTimes")
    @DisplayName("각 세션별 경계값 테스트")
    void testSessionBoundaries(LocalTime time, TradingSession session, boolean expected) {
        // When
        boolean result = policy.isWithinSession(time, session);

        // Then
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> provideSessionTimes() {
        return Stream.of(
            // REGULAR (09:00 - 15:30)
            Arguments.of(LocalTime.of(8, 59), TradingSession.REGULAR, false),
            Arguments.of(LocalTime.of(9, 0), TradingSession.REGULAR, true),
            Arguments.of(LocalTime.of(12, 0), TradingSession.REGULAR, true),
            Arguments.of(LocalTime.of(15, 30), TradingSession.REGULAR, true),
            Arguments.of(LocalTime.of(15, 31), TradingSession.REGULAR, false),

            // PRE_MARKET (08:30 - 08:40)
            Arguments.of(LocalTime.of(8, 29), TradingSession.PRE_MARKET, false),
            Arguments.of(LocalTime.of(8, 30), TradingSession.PRE_MARKET, true),
            Arguments.of(LocalTime.of(8, 40), TradingSession.PRE_MARKET, true),
            Arguments.of(LocalTime.of(8, 41), TradingSession.PRE_MARKET, false)
        );
    }
}
```

---

### Phase 2: Use Cases (유스케이스)

#### 2.1 PlaceOrderUseCase 테스트

**파일**: `src/test/java/maru/trading/application/usecase/trading/PlaceOrderUseCaseTest.java`

**테스트 케이스**:
```java
@DisplayName("PlaceOrderUseCase 테스트")
class PlaceOrderUseCaseTest extends IntegrationTestBase {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private OrderJpaRepository orderRepository;

    @MockBean
    private BrokerClient brokerClient;

    @MockBean
    private EvaluateRiskUseCase evaluateRiskUseCase;

    @Test
    @DisplayName("주문 생성 성공")
    void testPlaceOrder_Success() {
        // Given
        Order order = createTestOrder();

        given(evaluateRiskUseCase.evaluate(any()))
            .willReturn(RiskDecision.approve());

        given(brokerClient.placeOrder(any()))
            .willReturn(BrokerAck.success("BROKER-123"));

        // When
        Order result = placeOrderUseCase.execute(order);

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(result.getBrokerOrderNo()).isEqualTo("BROKER-123");

        // DB 검증
        Optional<OrderEntity> saved = orderRepository.findByOrderId(order.getOrderId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(OrderStatus.SENT);
    }

    @Test
    @DisplayName("멱등성 체크 - 중복 주문 방지")
    void testPlaceOrder_Idempotency() {
        // Given
        Order order = createTestOrder("IDEMPOTENCY-KEY-123");

        // 첫 번째 주문 실행
        given(evaluateRiskUseCase.evaluate(any()))
            .willReturn(RiskDecision.approve());
        given(brokerClient.placeOrder(any()))
            .willReturn(BrokerAck.success("BROKER-123"));

        Order first = placeOrderUseCase.execute(order);

        // When - 같은 idempotency key로 재시도
        Order second = placeOrderUseCase.execute(order);

        // Then
        assertThat(second.getOrderId()).isEqualTo(first.getOrderId());
        assertThat(second.getBrokerOrderNo()).isEqualTo(first.getBrokerOrderNo());

        // 브로커 호출은 1번만
        verify(brokerClient, times(1)).placeOrder(any());
    }

    @Test
    @DisplayName("리스크 체크 실패 시 주문 거부")
    void testPlaceOrder_RiskRejected() {
        // Given
        Order order = createTestOrder();

        given(evaluateRiskUseCase.evaluate(any()))
            .willReturn(RiskDecision.reject("Daily loss limit exceeded", "DAILY_LOSS_LIMIT"));

        // When & Then
        assertThatThrownBy(() -> placeOrderUseCase.execute(order))
            .isInstanceOf(RiskLimitExceededException.class)
            .hasMessageContaining("Daily loss limit exceeded");

        // 브로커 호출 안 됨
        verify(brokerClient, never()).placeOrder(any());
    }

    @Test
    @DisplayName("브로커 거부 시 주문 상태 REJECTED")
    void testPlaceOrder_BrokerRejected() {
        // Given
        Order order = createTestOrder();

        given(evaluateRiskUseCase.evaluate(any()))
            .willReturn(RiskDecision.approve());

        given(brokerClient.placeOrder(any()))
            .willReturn(BrokerAck.failure("INSUFFICIENT_BALANCE", "Insufficient balance"));

        // When
        Order result = placeOrderUseCase.execute(order);

        // Then
        assertThat(result.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }
}
```

#### 2.2 ApplyFillUseCase 테스트

**파일**: `src/test/java/maru/trading/application/usecase/execution/ApplyFillUseCaseTest.java`

**테스트 케이스**:
```java
@DisplayName("ApplyFillUseCase 테스트")
class ApplyFillUseCaseTest extends IntegrationTestBase {

    @Autowired
    private ApplyFillUseCase applyFillUseCase;

    @Autowired
    private PositionRepository positionRepository;

    @Test
    @DisplayName("신규 포지션 생성 - 매수")
    void testApplyFill_NewPosition_Buy() {
        // Given
        Fill fill = Fill.builder()
            .fillId("FILL-001")
            .orderId("ORDER-001")
            .accountId("ACC001")
            .symbol("005930")
            .side(Side.BUY)
            .qty(10)
            .price(BigDecimal.valueOf(70000))
            .build();

        // When
        ApplyFillResult result = applyFillUseCase.execute(fill);

        // Then
        Position position = result.getPosition();
        assertThat(position.getQty()).isEqualTo(10);
        assertThat(position.getAvgPrice()).isEqualByComparingTo("70000");
        assertThat(result.getRealizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("포지션 증가 - 추가 매수")
    void testApplyFill_IncreasePosition() {
        // Given - 기존 포지션 10주 @ 70,000
        Position existing = createPosition("005930", 10, BigDecimal.valueOf(70000));
        positionRepository.save(existing);

        // 추가 매수 5주 @ 80,000
        Fill fill = createFill(Side.BUY, 5, BigDecimal.valueOf(80000));

        // When
        ApplyFillResult result = applyFillUseCase.execute(fill);

        // Then
        Position updated = result.getPosition();
        assertThat(updated.getQty()).isEqualTo(15);
        // 평균가: (10*70000 + 5*80000) / 15 = 73,333.33
        assertThat(updated.getAvgPrice()).isEqualByComparingTo("73333.33");
    }

    @Test
    @DisplayName("포지션 청산 - 실현 손익 발생")
    void testApplyFill_ClosePosition_WithProfit() {
        // Given - 기존 포지션 10주 @ 70,000
        Position existing = createPosition("005930", 10, BigDecimal.valueOf(70000));
        positionRepository.save(existing);

        // 매도 10주 @ 80,000
        Fill fill = createFill(Side.SELL, 10, BigDecimal.valueOf(80000));

        // When
        ApplyFillResult result = applyFillUseCase.execute(fill);

        // Then
        assertThat(result.getPosition().getQty()).isEqualTo(0);
        // 실현 손익: (80,000 - 70,000) * 10 = 100,000
        assertThat(result.getRealizedPnl()).isEqualByComparingTo("100000");
        assertThat(result.isPositionClosed()).isTrue();
    }
}
```

---

### Phase 3: Strategy & Signal (전략 및 시그널)

#### 3.1 StrategyEngine 테스트

**파일**: `src/test/java/maru/trading/domain/strategy/impl/MACrossoverStrategyTest.java`

**테스트 케이스**:
```java
@DisplayName("MACrossoverStrategy 테스트")
class MACrossoverStrategyTest {

    private MACrossoverStrategy strategy;

    @BeforeEach
    void setUp() {
        Map<String, Object> params = Map.of(
            "shortPeriod", 5,
            "longPeriod", 20
        );
        strategy = new MACrossoverStrategy(params);
    }

    @Test
    @DisplayName("골든 크로스 - BUY 시그널")
    void testGoldenCross_BuySignal() {
        // Given - 단기 이평이 장기 이평을 상향 돌파
        List<MarketBar> bars = createBarsWithGoldenCross();
        StrategyContext context = StrategyContext.builder()
            .bars(bars)
            .build();

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
        assertThat(decision.getConfidence()).isGreaterThan(0.5);
    }

    @Test
    @DisplayName("데드 크로스 - SELL 시그널")
    void testDeadCross_SellSignal() {
        // Given - 단기 이평이 장기 이평을 하향 돌파
        List<MarketBar> bars = createBarsWithDeadCross();
        StrategyContext context = StrategyContext.builder()
            .bars(bars)
            .build();

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
    }

    @Test
    @DisplayName("이평선 평행 - HOLD 시그널")
    void testParallelMA_HoldSignal() {
        // Given
        List<MarketBar> bars = createBarsWithParallelMA();
        StrategyContext context = StrategyContext.builder()
            .bars(bars)
            .build();

        // When
        SignalDecision decision = strategy.evaluate(context);

        // Then
        assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
    }
}
```

---

## 통합 테스트

### 4.1 E2E 주문 플로우 테스트

**파일**: `src/test/java/maru/trading/integration/OrderFlowIntegrationTest.java`

**테스트 케이스**:
```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("E2E 주문 플로우 통합 테스트")
class OrderFlowIntegrationTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private ApplyFillUseCase applyFillUseCase;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("전체 플로우: 주문 → 체결 → 포지션 생성")
    void testCompleteOrderFlow() {
        // Step 1: 주문 생성
        Order order = placeOrderUseCase.execute(createBuyOrder());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SENT);

        // Step 2: 체결 적용
        Fill fill = createFillForOrder(order);
        ApplyFillResult result = applyFillUseCase.execute(fill);

        // Step 3: 포지션 확인
        assertThat(result.getPosition().getQty()).isEqualTo(10);

        // Step 4: 주문 상태 확인
        Order updated = orderRepository.findById(order.getOrderId()).get();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.FILLED);
    }
}
```

### 4.2 시장 데이터 → 전략 → 주문 통합 테스트

**파일**: `src/test/java/maru/trading/integration/MarketDataToOrderIntegrationTest.java`

**테스트 시나리오**:
```java
@SpringBootTest
@DisplayName("시장 데이터 → 전략 → 주문 통합 테스트")
class MarketDataToOrderIntegrationTest {

    @Test
    @DisplayName("틱 수신 → 바 생성 → 전략 실행 → 주문 생성")
    void testMarketDataToOrder() {
        // Given - 20개 틱 주입
        for (int i = 0; i < 20; i++) {
            MarketTick tick = createTick(i);
            marketDataService.onTickReceived(tick);
        }

        // When - 전략 실행 트리거
        strategyScheduler.executeStrategies();

        // Then - 시그널 생성 확인
        List<Signal> signals = signalRepository.findAll();
        assertThat(signals).isNotEmpty();

        // 주문 생성 확인
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).isNotEmpty();
    }
}
```

---

## E2E 테스트

### 5.1 REST API E2E 테스트

**파일**: `src/test/java/maru/trading/api/OrderApiE2ETest.java`

**테스트 케이스**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DisplayName("Order API E2E 테스트")
class OrderApiE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("GET /api/v1/query/orders - 주문 조회")
    void testGetOrders() {
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/query/orders?accountId=ACC001",
            String.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("POST /api/v1/admin/orders/cancel - 주문 취소")
    void testCancelOrder() {
        // Given
        CancelOrderRequest request = new CancelOrderRequest("ORDER-123");

        // When
        ResponseEntity<AckResponse> response = restTemplate.postForEntity(
            "/api/v1/admin/orders/cancel",
            request,
            AckResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
```

---

## 성능 테스트

### 6.1 주문 처리 성능 테스트

**목표**:
- 초당 100건 주문 처리
- 평균 응답 시간 < 100ms
- 99 percentile < 500ms

**도구**: JMeter, Gatling

---

## 테스트 우선순위

### 우선순위 1 (Critical) - 즉시 구현

1. **RiskEngine 7개 체크**: 100% 커버리지
2. **PlaceOrderUseCase**: 멱등성, 리스크 체크
3. **ApplyFillUseCase**: 포지션 계산
4. **MarketHoursPolicy**: 거래시간 검증
5. **Order 도메인**: 상태 전이, 취소/정정 검증

### 우선순위 2 (High) - 1주 내

1. **StrategyEngine**: MA Crossover, RSI
2. **BarAggregator**: 틱 → 바 집계
3. **SignalPolicy**: TTL, 쿨다운
4. **WebSocket 파서**: KIS 메시지 파싱

### 우선순위 3 (Medium) - 2주 내

1. **통합 테스트**: E2E 플로우
2. **API 테스트**: REST 엔드포인트
3. **Repository 테스트**: JPA 쿼리

### 우선순위 4 (Low) - 추후

1. **성능 테스트**
2. **부하 테스트**
3. **보안 테스트**

---

## 테스트 자동화 및 CI/CD

### GitHub Actions 워크플로우

**`.github/workflows/test.yml`**:
```yaml
name: Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'corretto'

    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

    - name: Run Unit Tests
      run: mvn test

    - name: Run Integration Tests
      run: mvn verify

    - name: Generate Coverage Report
      run: mvn jacoco:report

    - name: Upload Coverage to Codecov
      uses: codecov/codecov-action@v3
```

### 로컬 테스트 실행

```bash
# 전체 테스트
mvn clean test

# 단위 테스트만
mvn test

# 통합 테스트만
mvn verify -DskipUnitTests

# 커버리지 리포트
mvn jacoco:report

# 특정 테스트 클래스
mvn test -Dtest=RiskEngineTest

# 특정 테스트 메서드
mvn test -Dtest=RiskEngineTest#testKillSwitch_Reject
```

---

## 테스트 커버리지 목표

### 패키지별 목표

| 패키지 | 목표 커버리지 | 우선순위 |
|--------|--------------|---------|
| `domain.risk` | 100% | Critical |
| `domain.order` | 95% | Critical |
| `domain.market` | 90% | High |
| `application.usecase` | 85% | High |
| `domain.strategy` | 85% | Medium |
| `api.controller` | 70% | Medium |
| `infra.persistence` | 60% | Low |

---

## 테스트 데이터 관리

### Test Fixtures

**`src/test/java/maru/trading/fixtures/TestFixtures.java`**:
```java
public class TestFixtures {

    public static Order createTestOrder() {
        return Order.builder()
            .orderId(UlidGenerator.generate())
            .accountId("ACC001")
            .symbol("005930")
            .side(Side.BUY)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .build();
    }

    public static MarketTick createTestTick(String symbol, BigDecimal price) {
        return new MarketTick(
            symbol,
            price,
            100L,
            LocalDateTime.now(),
            "NORMAL"
        );
    }

    public static List<MarketBar> createTestBars(int count) {
        // 테스트용 바 생성
    }
}
```

---

## 다음 단계

1. ✅ 테스트 계획 수립 완료
2. ⏳ **우선순위 1 테스트 구현** (RiskEngine, PlaceOrderUseCase 등)
3. ⏳ CI/CD 파이프라인 설정
4. ⏳ 커버리지 리포트 자동화
5. ⏳ 통합 테스트 환경 구축

---

**작성자**: Claude Sonnet 4.5
**프로젝트**: cautostock - KIS Trading System MVP
**작성일**: 2026-01-01
