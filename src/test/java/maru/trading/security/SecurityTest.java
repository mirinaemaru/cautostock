package maru.trading.security;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Security Test.
 *
 * Tests security aspects of the trading system:
 * - Input validation
 * - Error handling
 * - Data integrity
 * - Business rule enforcement
 *
 * Security focus areas:
 * - Prevent invalid order parameters
 * - Protect against malicious input
 * - Ensure proper error handling
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Test")
class SecurityTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        accountId = "ACC_SEC_001";
        symbol = "005930";
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-SEC"));
    }

    // ==================== Input Validation Tests ====================
    // Note: Current implementation does not have strict validation.
    // These tests verify current behavior and can be updated when validation is added.

    @Test
    @DisplayName("Order creation with valid parameters should succeed")
    void testValidParameters_ShouldSucceed() {
        // Given - Valid order parameters
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(10),
                BigDecimal.valueOf(70000),
                UlidGenerator.generate()
        );

        // When
        Order result = placeOrderUseCase.execute(order);

        // Then - Should succeed
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isNotNull();
        assertThat(result.getSymbol()).isNotNull();
    }

    @Test
    @DisplayName("Order with very large quantity should be handled")
    void testLargeQuantity_ShouldBeHandled() {
        // Given - Order with large quantity
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(1000000), // Very large quantity
                BigDecimal.valueOf(70000),
                UlidGenerator.generate()
        );

        // When & Then - Should either succeed or fail gracefully
        // (May fail due to risk limits, which is acceptable)
        try {
            Order result = placeOrderUseCase.execute(order);
            assertThat(result).isNotNull();
        } catch (Exception e) {
            // Risk limit exceeded is acceptable
            assertThat(e.getMessage()).contains("Risk");
        }
    }

    // ==================== Business Rule Enforcement Tests ====================

    @Test
    @DisplayName("Should reject duplicate idempotency key")
    void testDuplicateIdempotencyKey_ShouldBeIdempotent() {
        // Given - Same idempotency key
        String idempotencyKey = "IDEMPOTENCY_TEST_001";

        Order order1 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(5),
                BigDecimal.valueOf(70000),
                idempotencyKey
        );

        // When - Place first order
        Order result1 = placeOrderUseCase.execute(order1);
        assertThat(result1).isNotNull();

        // Create second order with same idempotency key
        Order order2 = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(), // Different orderId
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(10), // Different quantity
                BigDecimal.valueOf(71000), // Different price
                idempotencyKey // SAME idempotency key
        );

        // Then - Second order should return same result (idempotent)
        Order result2 = placeOrderUseCase.execute(order2);
        assertThat(result2.getOrderId()).isEqualTo(result1.getOrderId());
        assertThat(result2.getQty()).isEqualByComparingTo(result1.getQty());
    }

    // ==================== Data Integrity Tests ====================

    @Test
    @DisplayName("Order data should remain immutable after placement")
    void testOrderImmutability() {
        // Given
        Order order = TestFixtures.placeLimitOrder(
                UlidGenerator.generate(),
                accountId,
                symbol,
                Side.BUY,
                BigDecimal.valueOf(7),
                BigDecimal.valueOf(72000),
                UlidGenerator.generate()
        );

        BigDecimal originalQty = order.getQty();
        BigDecimal originalPrice = order.getPrice();

        // When
        Order result = placeOrderUseCase.execute(order);

        // Then - Original order parameters should not change
        assertThat(result.getQty()).isEqualByComparingTo(originalQty);
        assertThat(result.getPrice()).isEqualByComparingTo(originalPrice);
    }
}
