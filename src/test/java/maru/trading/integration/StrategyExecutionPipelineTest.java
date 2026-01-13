package maru.trading.integration;

import maru.trading.TestFixtures;
import maru.trading.application.orchestration.BarAggregator;
import maru.trading.application.orchestration.TradingWorkflow;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.service.MarketDataService;
import maru.trading.application.usecase.strategy.GenerateSignalUseCase;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.market.MarketTick;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.infra.cache.MarketDataCache;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.entity.RiskRuleEntity;
import maru.trading.infra.persistence.jpa.entity.SignalEntity;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import maru.trading.infra.persistence.jpa.repository.RiskRuleJpaRepository;
import maru.trading.infra.persistence.jpa.repository.SignalJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Phase 3 Strategy Execution Pipeline Integration Test.
 *
 * Tests the complete automated trading workflow:
 * 1. Market Data Ingestion → MarketDataCache
 * 2. Bar Aggregation → 1-minute bars
 * 3. Strategy Execution → Signal generation (future)
 * 4. Signal Processing → Order placement
 * 5. Risk Checks → Order validation
 * 6. Order Execution → Broker submission
 *
 * This test validates the end-to-end pipeline from market data
 * to order placement, demonstrating full Phase 3 functionality.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 3 Strategy Execution Pipeline Integration Test")
class StrategyExecutionPipelineTest {

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private MarketDataCache marketDataCache;

    @Autowired
    private BarAggregator barAggregator;

    @Autowired
    private GenerateSignalUseCase generateSignalUseCase;

    @Autowired
    private TradingWorkflow tradingWorkflow;

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private SignalJpaRepository signalRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private RiskRuleJpaRepository riskRuleRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;
    private String strategyId;

    @BeforeEach
    void setUp() {
        accountId = "ACC_PIPELINE_001";
        symbol = "005930";
        strategyId = "STRATEGY_MA_CROSS";

        // Setup relaxed risk rules for integration testing
        RiskRule relaxedRule = TestFixtures.createRelaxedRiskRule(UlidGenerator.generate());
        RiskRuleEntity ruleEntity = RiskRuleEntity.builder()
                .riskRuleId(relaxedRule.getRiskRuleId())
                .scope(RiskRuleScope.GLOBAL)
                .dailyLossLimit(relaxedRule.getDailyLossLimit())
                .maxOpenOrders(relaxedRule.getMaxOpenOrders())
                .maxOrdersPerMinute(relaxedRule.getMaxOrdersPerMinute())
                .maxPositionValuePerSymbol(relaxedRule.getMaxPositionValuePerSymbol())
                .consecutiveOrderFailuresLimit(relaxedRule.getConsecutiveOrderFailuresLimit())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        riskRuleRepository.save(ruleEntity);

        // Mock broker client
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-PIPELINE-TEST"));
    }

    // ==================== Full Pipeline Tests ====================

    @Test
    @DisplayName("Complete Pipeline: Market Data → Signal → Order")
    void testCompletePipeline_MarketDataToOrder() {
        // Phase 1: Market Data Ingestion
        MarketTick tick = createMarketTick(symbol, BigDecimal.valueOf(70000), 1000);
        marketDataCache.put(tick);

        // Verify market data is cached
        MarketTick cachedTick = marketDataCache.get(symbol);
        assertThat(cachedTick).isNotNull();
        assertThat(cachedTick.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));

        // Phase 2: Bar Aggregation (simulated via tick injection)
        barAggregator.onTick(tick);

        // Phase 3: Signal Generation (manual injection for now)
        Signal signal = Signal.builder()
                .signalId(UlidGenerator.generate())
                .strategyId(strategyId)
                .accountId(accountId)
                .symbol(symbol)
                .signalType(SignalType.BUY)
                .targetType("QTY")
                .targetValue(BigDecimal.valueOf(10))
                .ttlSeconds(120)
                .build();

        SignalEntity savedSignal = signalRepository.save(toSignalEntity(signal));
        assertThat(savedSignal).isNotNull();

        // Phase 4: Signal Processing → Order
        tradingWorkflow.processSignal(signal);

        // Phase 5: Verify Order was created
        List<OrderEntity> allOrders = orderRepository.findAll();
        List<OrderEntity> ordersForSymbol = allOrders.stream()
                .filter(o -> symbol.equals(o.getSymbol()))
                .toList();
        assertThat(ordersForSymbol).isNotEmpty();

        OrderEntity order = ordersForSymbol.get(0);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getSide()).isEqualTo(Side.BUY);
        assertThat(order.getQty()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SENT);
    }

    @Test
    @DisplayName("Pipeline: Multiple ticks should update market cache")
    void testPipeline_MultipleTicks() {
        // Given - Inject 5 ticks with varying prices
        injectTick(symbol, BigDecimal.valueOf(69500), 500);
        injectTick(symbol, BigDecimal.valueOf(70000), 800);
        injectTick(symbol, BigDecimal.valueOf(70500), 1200);
        injectTick(symbol, BigDecimal.valueOf(70200), 900);
        injectTick(symbol, BigDecimal.valueOf(70300), 1000);

        // When - Get latest tick from cache
        MarketTick latest = marketDataCache.get(symbol);

        // Then - Should reflect the last tick
        assertThat(latest).isNotNull();
        assertThat(latest.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(70300));
        assertThat(latest.getVolume()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Pipeline: Signal → Order with Risk Check")
    void testPipeline_SignalToOrderWithRiskCheck() {
        // Given - Create BUY signal
        Signal buySignal = Signal.builder()
                .signalId(UlidGenerator.generate())
                .strategyId(strategyId)
                .accountId(accountId)
                .symbol(symbol)
                .signalType(SignalType.BUY)
                .targetType("QTY")
                .targetValue(BigDecimal.valueOf(15))
                .ttlSeconds(60)
                .build();

        // When - Process signal through trading workflow
        tradingWorkflow.processSignal(buySignal);

        // Then - Order should be created and pass risk checks
        List<OrderEntity> allOrders = orderRepository.findAll();
        List<OrderEntity> ordersForSymbol = allOrders.stream()
                .filter(o -> symbol.equals(o.getSymbol()))
                .toList();
        assertThat(ordersForSymbol).hasSize(1);

        OrderEntity order = ordersForSymbol.get(0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(order.getQty()).isEqualByComparingTo(BigDecimal.valueOf(15));
    }

    @Test
    @DisplayName("Pipeline: SELL signal should create SELL order")
    void testPipeline_SellSignalToSellOrder() {
        // Given - Create SELL signal
        Signal sellSignal = Signal.builder()
                .signalId(UlidGenerator.generate())
                .strategyId(strategyId)
                .accountId(accountId)
                .symbol("035420") // Different symbol
                .signalType(SignalType.SELL)
                .targetType("QTY")
                .targetValue(BigDecimal.valueOf(8))
                .ttlSeconds(90)
                .build();

        // When - Process signal
        tradingWorkflow.processSignal(sellSignal);

        // Then - SELL order should be created
        List<OrderEntity> allOrders = orderRepository.findAll();
        List<OrderEntity> ordersForSymbol = allOrders.stream()
                .filter(o -> "035420".equals(o.getSymbol()))
                .toList();
        assertThat(ordersForSymbol).hasSize(1);

        OrderEntity order = ordersForSymbol.get(0);
        assertThat(order.getSide()).isEqualTo(Side.SELL);
        assertThat(order.getSymbol()).isEqualTo("035420");
        assertThat(order.getQty()).isEqualByComparingTo(BigDecimal.valueOf(8));
    }

    @Test
    @DisplayName("Pipeline: Direct order placement should work")
    void testPipeline_DirectOrderPlacement() {
        // Given - Create order directly
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(71000),
                UlidGenerator.generate()
        );

        // When - Place order
        Order placedOrder = placeOrderUseCase.execute(order);

        // Then - Order should be sent successfully
        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placedOrder.getOrderId()).isEqualTo(order.getOrderId());

        // Verify persistence
        OrderEntity savedOrder = orderRepository.findById(order.getOrderId())
                .orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(savedOrder.getBrokerOrderNo()).isEqualTo("BROKER-PIPELINE-TEST");
    }

    @Test
    @DisplayName("Pipeline: Market data cache should handle multiple symbols")
    void testPipeline_MultipleSymbols() {
        // Given - Inject ticks for multiple symbols
        String symbol1 = "005930";
        String symbol2 = "035420";
        String symbol3 = "000660";

        injectTick(symbol1, BigDecimal.valueOf(70000), 1000);
        injectTick(symbol2, BigDecimal.valueOf(40000), 500);
        injectTick(symbol3, BigDecimal.valueOf(150000), 200);

        // When - Retrieve each symbol
        MarketTick tick1 = marketDataCache.get(symbol1);
        MarketTick tick2 = marketDataCache.get(symbol2);
        MarketTick tick3 = marketDataCache.get(symbol3);

        // Then - Each should be cached independently
        assertThat(tick1.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(tick2.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(40000));
        assertThat(tick3.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(150000));
    }

    // ==================== Helper Methods ====================

    private void injectTick(String tickSymbol, BigDecimal price, long volume) {
        MarketTick tick = createMarketTick(tickSymbol, price, volume);
        marketDataCache.put(tick);
        barAggregator.onTick(tick);
    }

    private MarketTick createMarketTick(String tickSymbol, BigDecimal price, long volume) {
        return new MarketTick(
                tickSymbol,
                price,
                volume,
                LocalDateTime.now(),
                "NORMAL"
        );
    }

    private SignalEntity toSignalEntity(Signal signal) {
        return SignalEntity.builder()
                .signalId(signal.getSignalId())
                .strategyId(signal.getStrategyId())
                .accountId(signal.getAccountId())
                .symbol(signal.getSymbol())
                .signalType(signal.getSignalType().name()) // Convert enum to String
                .targetType(signal.getTargetType())
                .targetValue(signal.getTargetValue())
                .ttlSeconds(signal.getTtlSeconds())
                .expired(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
