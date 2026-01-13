package maru.trading.integration;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Market Data to Order Integration Test (Simplified Version).
 *
 * This test validates the order placement workflow that would be triggered
 * by market data analysis. Currently focuses on the order placement mechanics
 * without full strategy execution (which requires Phase 3 completion).
 *
 * Test scenarios:
 * 1. Manual signal → Order placement
 * 2. Multiple signals → Multiple orders
 * 3. Order validation and risk checks
 *
 * Note: Full market data → strategy → signal → order pipeline will be
 * tested once Phase 3 strategy components are fully implemented.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Market Data to Order Integration Test (Simplified)")
class MarketDataToOrderIntegrationTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        accountId = "ACC_TEST_002";
        symbol = "005930";
    }

    @Test
    @DisplayName("시그널 기반 매수 주문 생성")
    void testSignalTriggeredBuyOrder() {
        // Given - Mock broker to accept order
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-BUY-001"));

        // Simulate a BUY signal from strategy analysis
        // In real scenario, this would come from MACrossoverStrategy or RSIStrategy
        Order buyOrder = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(71000),
                UlidGenerator.generate()
        );

        // When - Place order triggered by signal
        Order placedOrder = placeOrderUseCase.execute(buyOrder);

        // Then - Order should be sent to broker
        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placedOrder.getBrokerOrderNo()).isEqualTo("BROKER-BUY-001");
        assertThat(placedOrder.getSide()).isEqualTo(Side.BUY);

        // Verify order persisted in database
        Order savedOrder = orderRepository.findById(placedOrder.getOrderId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(savedOrder.getSymbol()).isEqualTo(symbol);
        assertThat(savedOrder.getAccountId()).isEqualTo(accountId);
    }

    @Test
    @DisplayName("시그널 기반 매도 주문 생성")
    void testSignalTriggeredSellOrder() {
        // Given
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-SELL-001"));

        // Simulate a SELL signal from strategy analysis
        Order sellOrder = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.SELL,
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(72000),
                UlidGenerator.generate()
        );

        // When
        Order placedOrder = placeOrderUseCase.execute(sellOrder);

        // Then
        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placedOrder.getSide()).isEqualTo(Side.SELL);
        assertThat(placedOrder.getBrokerOrderNo()).isEqualTo("BROKER-SELL-001");
    }

    @Test
    @DisplayName("연속된 시그널로 여러 주문 생성")
    void testMultipleSignalsGenerateMultipleOrders() {
        // Given
        given(brokerClient.placeOrder(any()))
                .willReturn(
                        BrokerAck.success("BROKER-001"),
                        BrokerAck.success("BROKER-002"),
                        BrokerAck.success("BROKER-003")
                );

        // Simulate multiple signals over time
        // Signal 1: BUY
        Order order1 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(2), BigDecimal.valueOf(70500), UlidGenerator.generate()
        );
        Order placed1 = placeOrderUseCase.execute(order1);

        // Signal 2: BUY (accumulation)
        Order order2 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(3), BigDecimal.valueOf(71000), UlidGenerator.generate()
        );
        Order placed2 = placeOrderUseCase.execute(order2);

        // Signal 3: SELL (take profit)
        Order order3 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.SELL,
                BigDecimal.valueOf(2), BigDecimal.valueOf(73000), UlidGenerator.generate()
        );
        Order placed3 = placeOrderUseCase.execute(order3);

        // Then - All orders should be placed
        assertThat(placed1.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placed2.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placed3.getStatus()).isEqualTo(OrderStatus.SENT);

        // Verify all orders in database
        assertThat(orderRepository.findById(placed1.getOrderId())).isPresent();
        assertThat(orderRepository.findById(placed2.getOrderId())).isPresent();
        assertThat(orderRepository.findById(placed3.getOrderId())).isPresent();
    }

    @Test
    @DisplayName("Market order 생성 테스트")
    void testMarketOrderFromSignal() {
        // Given
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-MARKET-001"));

        // Simulate urgent signal requiring market order
        Order marketOrder = TestFixtures.placeMarketOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(10),
                UlidGenerator.generate()
        );

        // When
        Order placedOrder = placeOrderUseCase.execute(marketOrder);

        // Then
        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placedOrder.getPrice()).isNull(); // Market order has no limit price
        assertThat(placedOrder.getBrokerOrderNo()).isEqualTo("BROKER-MARKET-001");
    }
}
