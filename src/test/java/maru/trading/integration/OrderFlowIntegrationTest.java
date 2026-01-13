package maru.trading.integration;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.repo.FillRepository;
import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.application.usecase.execution.ApplyFillUseCase;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.execution.Position;
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
 * Order Flow Integration Test.
 *
 * Tests the complete E2E order workflow:
 * 1. Place order (PlaceOrderUseCase)
 * 2. Receive fill from broker (ApplyFillUseCase)
 * 3. Update position and PnL
 * 4. Verify database state
 *
 * This is a true integration test using:
 * - Real Spring context
 * - Real database (H2 in-memory)
 * - Real service beans
 * - Mocked BrokerClient (to avoid actual broker calls)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("E2E Order Flow Integration Test")
class OrderFlowIntegrationTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private ApplyFillUseCase applyFillUseCase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FillRepository fillRepository;

    @Autowired
    private PositionRepository positionRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        accountId = "ACC_TEST_001";
        symbol = "005930";
    }

    // ==================== Complete Flow Tests ====================

    @Test
    @DisplayName("완전한 플로우: 주문 → 체결 → 포지션 생성")
    void testCompleteOrderFlow_NewPosition() {
        // Given - Mock broker to accept order
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-ORDER-123"));

        // Create buy order
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(70000),
                UlidGenerator.generate()
        );

        // Step 1: Place order
        Order placedOrder = placeOrderUseCase.execute(order);

        // Then - Order should be SENT
        assertThat(placedOrder).isNotNull();
        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(placedOrder.getBrokerOrderNo()).isEqualTo("BROKER-ORDER-123");

        // Verify order saved in DB
        Order savedOrder = orderRepository.findById(placedOrder.getOrderId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.SENT);

        // Step 2: Receive fill from broker
        Fill fill = TestFixtures.createFill(
                UlidGenerator.generate(), // fillId
                placedOrder.getOrderId(), // orderId
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(70000), // fillPrice
                10, // fillQty
                BigDecimal.valueOf(500), // fee
                BigDecimal.valueOf(100) // tax
        );

        // Apply fill
        ApplyFillUseCase.ApplyFillResult fillResult = applyFillUseCase.execute(fill);

        // Then - Position should be created
        Position position = fillResult.getPosition();
        assertThat(position).isNotNull();
        assertThat(position.getSymbol()).isEqualTo(symbol);
        assertThat(position.getQty()).isEqualTo(10);
        assertThat(position.getAvgPrice()).isEqualByComparingTo("70000");

        // Step 3: Verify position in DB
        Position dbPosition = positionRepository.findByAccountAndSymbol(accountId, symbol).orElseThrow();
        assertThat(dbPosition.getQty()).isEqualTo(10);
        assertThat(dbPosition.getAvgPrice()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("완전한 플로우: 주문 → 부분 체결 → 포지션 생성")
    void testCompleteOrderFlow_PartialFill() {
        // Given
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-ORDER-456"));

        // Create buy order for 12 shares (smaller to avoid position limit)
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(70000),
                UlidGenerator.generate()
        );

        // Step 1: Place order
        Order placedOrder = placeOrderUseCase.execute(order);
        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.SENT);

        // Step 2: Receive partial fill (6 shares out of 12)
        Fill partialFill = TestFixtures.createFill(
                UlidGenerator.generate(), // fillId
                placedOrder.getOrderId(), // orderId
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(70000), // fillPrice
                6, // fillQty (partial - half of the order)
                BigDecimal.valueOf(150), // fee
                BigDecimal.valueOf(30) // tax
        );

        ApplyFillUseCase.ApplyFillResult fillResult = applyFillUseCase.execute(partialFill);

        // Then - Position created with 6 shares (partial fill amount)
        assertThat(fillResult.getPosition().getQty()).isEqualTo(6);
        assertThat(fillResult.getPosition().getAvgPrice()).isEqualByComparingTo("70000");

        // Verify position in DB
        Position dbPosition = positionRepository.findByAccountAndSymbol(accountId, symbol).orElseThrow();
        assertThat(dbPosition.getQty()).isEqualTo(6);
    }

    @Test
    @DisplayName("완전한 플로우: 매수 → 매도 → 포지션 청산 + 실현 손익")
    void testCompleteOrderFlow_BuyThenSellWithProfit() {
        // Given
        given(brokerClient.placeOrder(any()))
                .willReturn(
                        BrokerAck.success("BROKER-BUY-123"),
                        BrokerAck.success("BROKER-SELL-456")
                );

        // Step 1: Buy 10 shares @ 70,000
        Order buyOrder = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(70000),
                UlidGenerator.generate()
        );

        Order placedBuyOrder = placeOrderUseCase.execute(buyOrder);

        Fill buyFill = TestFixtures.createFill(
                UlidGenerator.generate(),
                placedBuyOrder.getOrderId(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(70000),
                10,
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(100)
        );

        ApplyFillUseCase.ApplyFillResult buyResult = applyFillUseCase.execute(buyFill);

        // Verify position created
        assertThat(buyResult.getPosition().getQty()).isEqualTo(10);
        assertThat(buyResult.getPosition().getAvgPrice()).isEqualByComparingTo("70000");

        // Step 2: Sell 10 shares @ 80,000 (profit scenario)
        Order sellOrder = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.SELL,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(80000),
                UlidGenerator.generate()
        );

        Order placedSellOrder = placeOrderUseCase.execute(sellOrder);

        Fill sellFill = TestFixtures.createFill(
                UlidGenerator.generate(),
                placedSellOrder.getOrderId(),
                accountId,
                symbol,
                Side.SELL,
                BigDecimal.valueOf(80000),
                10,
                BigDecimal.valueOf(600),
                BigDecimal.valueOf(120)
        );

        ApplyFillUseCase.ApplyFillResult sellResult = applyFillUseCase.execute(sellFill);

        // Then - Position should be closed (qty = 0)
        assertThat(sellResult.getPosition().getQty()).isEqualTo(0);

        // Realized PnL should be positive
        // Profit = (80,000 - 70,000) * 10 = 100,000
        // Note: Fees and taxes are recorded separately in PnL ledgers
        BigDecimal expectedPnlDelta = BigDecimal.valueOf(100000);

        assertThat(sellResult.getRealizedPnlDelta()).isEqualByComparingTo(expectedPnlDelta);

        // Verify position in DB shows qty = 0
        Position dbPosition = positionRepository.findByAccountAndSymbol(accountId, symbol).orElseThrow();
        assertThat(dbPosition.getQty()).isEqualTo(0);
    }

    @Test
    @DisplayName("완전한 플로우: 다중 주문 처리 (순차 실행)")
    void testCompleteOrderFlow_MultipleOrders() {
        // Given
        given(brokerClient.placeOrder(any()))
                .willReturn(
                        BrokerAck.success("BROKER-1"),
                        BrokerAck.success("BROKER-2"),
                        BrokerAck.success("BROKER-3")
                );

        // Step 1: First buy - 5 shares @ 70,000
        Order order1 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(5), BigDecimal.valueOf(70000), UlidGenerator.generate()
        );
        Order placed1 = placeOrderUseCase.execute(order1);
        Fill fill1 = TestFixtures.createFill(
                UlidGenerator.generate(), placed1.getOrderId(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(70000), 5, BigDecimal.valueOf(100), BigDecimal.valueOf(20)
        );
        ApplyFillUseCase.ApplyFillResult result1 = applyFillUseCase.execute(fill1);

        assertThat(result1.getPosition().getQty()).isEqualTo(5);
        assertThat(result1.getPosition().getAvgPrice()).isEqualByComparingTo("70000");

        // Step 2: Second buy - 3 shares @ 75,000
        Order order2 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(3), BigDecimal.valueOf(75000), UlidGenerator.generate()
        );
        Order placed2 = placeOrderUseCase.execute(order2);
        Fill fill2 = TestFixtures.createFill(
                UlidGenerator.generate(), placed2.getOrderId(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(75000), 3, BigDecimal.valueOf(60), BigDecimal.valueOf(12)
        );
        ApplyFillUseCase.ApplyFillResult result2 = applyFillUseCase.execute(fill2);

        // Position: 8 shares, avg price = (5*70000 + 3*75000) / 8 = 71,875
        assertThat(result2.getPosition().getQty()).isEqualTo(8);
        assertThat(result2.getPosition().getAvgPrice()).isEqualByComparingTo("71875");

        // Step 3: Sell - 4 shares @ 80,000
        Order order3 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.SELL,
                BigDecimal.valueOf(4), BigDecimal.valueOf(80000), UlidGenerator.generate()
        );
        Order placed3 = placeOrderUseCase.execute(order3);
        Fill fill3 = TestFixtures.createFill(
                UlidGenerator.generate(), placed3.getOrderId(), accountId, symbol, Side.SELL,
                BigDecimal.valueOf(80000), 4, BigDecimal.valueOf(80), BigDecimal.valueOf(16)
        );
        ApplyFillUseCase.ApplyFillResult result3 = applyFillUseCase.execute(fill3);

        // Position: 4 shares remaining, avg price unchanged
        assertThat(result3.getPosition().getQty()).isEqualTo(4);
        assertThat(result3.getPosition().getAvgPrice()).isEqualByComparingTo("71875");

        // Realized PnL delta = (80,000 - 71,875) * 4 = 32,500
        assertThat(result3.getRealizedPnlDelta()).isGreaterThan(BigDecimal.ZERO);

        // Verify final DB state
        Position finalPosition = positionRepository.findByAccountAndSymbol(accountId, symbol).orElseThrow();
        assertThat(finalPosition.getQty()).isEqualTo(4);
    }
}
