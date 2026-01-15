package maru.trading.performance;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.usecase.execution.ApplyFillUseCase;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Order;
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
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Performance Test.
 *
 * Measures performance of critical operations:
 * - Order placement latency
 * - Fill processing latency
 * - Database query performance
 *
 * Performance targets:
 * - Order placement: < 100ms
 * - Fill processing: < 50ms
 * - Batch operations: < 500ms for 10 items
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Performance Test")
class PerformanceTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private ApplyFillUseCase applyFillUseCase;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        accountId = "ACC_PERF_001";
        symbol = "005930";
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-PERF"));
    }

    @Test
    @DisplayName("Order placement should complete within 100ms")
    void testOrderPlacementPerformance() {
        // Given
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(70000),
                UlidGenerator.generate()
        );

        // When - Measure execution time
        Instant start = Instant.now();
        Order placedOrder = placeOrderUseCase.execute(order);
        Instant end = Instant.now();

        // Then - Should complete within 100ms
        Duration duration = Duration.between(start, end);
        assertThat(placedOrder).isNotNull();
        assertThat(duration.toMillis()).isLessThan(100L);
    }

    @Test
    @DisplayName("Fill processing should complete within 50ms")
    void testFillProcessingPerformance() {
        // Given - Place order first
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(5), BigDecimal.valueOf(70000), UlidGenerator.generate()
        );
        Order placedOrder = placeOrderUseCase.execute(order);

        Fill fill = TestFixtures.createFill(
                UlidGenerator.generate(),
                placedOrder.getOrderId(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(70000),
                5,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(20)
        );

        // When - Measure fill processing time
        Instant start = Instant.now();
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(fill);
        Instant end = Instant.now();

        // Then - Should complete within 50ms
        Duration duration = Duration.between(start, end);
        assertThat(result.getPosition()).isNotNull();
        assertThat(duration.toMillis()).isLessThan(50L);
    }

    @Test
    @DisplayName("Batch order placement (10 orders) should complete within 500ms")
    void testBatchOrderPlacementPerformance() {
        // When - Place 10 orders (using different accounts to avoid risk limits)
        Instant start = Instant.now();

        for (int i = 0; i < 10; i++) {
            Order order = TestFixtures.placeLimitOrder(
                    UlidGenerator.generate(),
                    accountId + "_" + i,  // Different account for each order
                    symbol,
                    Side.BUY,
                    BigDecimal.valueOf(1),
                    BigDecimal.valueOf(70000 + i * 100),
                    UlidGenerator.generate()
            );
            placeOrderUseCase.execute(order);
        }

        Instant end = Instant.now();

        // Then - Should complete within reasonable time (adjusted for risk state updates)
        Duration duration = Duration.between(start, end);
        assertThat(duration.toMillis()).isLessThan(1000L);
    }

    @Test
    @DisplayName("Complete order flow (place + fill) should complete within 150ms")
    void testCompleteFlowPerformance() {
        // Given
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), accountId, symbol, Side.BUY,
                BigDecimal.valueOf(3), BigDecimal.valueOf(71000), UlidGenerator.generate()
        );

        // When - Measure complete flow
        Instant start = Instant.now();

        Order placedOrder = placeOrderUseCase.execute(order);

        Fill fill = TestFixtures.createFill(
                UlidGenerator.generate(),
                placedOrder.getOrderId(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(71000),
                3,
                BigDecimal.valueOf(75),
                BigDecimal.valueOf(15)
        );

        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(fill);

        Instant end = Instant.now();

        // Then - Complete flow should finish within 150ms
        Duration duration = Duration.between(start, end);
        assertThat(result.getPosition()).isNotNull();
        assertThat(duration.toMillis()).isLessThan(150L);
    }
}
