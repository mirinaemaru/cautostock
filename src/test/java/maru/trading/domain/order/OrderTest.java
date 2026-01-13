package maru.trading.domain.order;

import maru.trading.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Order 도메인 모델 테스트
 *
 * 테스트 범위:
 * 1. 주문 취소 가능 여부 (isCancellable)
 * 2. 주문 정정 가능 여부 (isModifiable)
 * 3. 취소 검증 (validateCancellable)
 * 4. 정정 검증 (validateModifiable)
 * 5. 상태별 취소/정정 가능 여부 매트릭스
 */
@DisplayName("Order 도메인 테스트")
class OrderTest {

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .accountId("ACC_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.LIMIT)
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.NEW)
            .idempotencyKey("KEY_001")
            .build();
    }

    // ==================== 1. isCancellable Tests ====================

    @Test
    @DisplayName("isCancellable - NEW 상태는 취소 불가")
    void testIsCancellable_New_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.NEW)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isFalse();
    }

    @Test
    @DisplayName("isCancellable - SENT 상태는 취소 가능")
    void testIsCancellable_Sent_True() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.SENT)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isTrue();
    }

    @Test
    @DisplayName("isCancellable - ACCEPTED 상태는 취소 가능")
    void testIsCancellable_Accepted_True() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.ACCEPTED)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isTrue();
    }

    @Test
    @DisplayName("isCancellable - PART_FILLED 상태는 취소 가능")
    void testIsCancellable_PartFilled_True() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.PART_FILLED)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isTrue();
    }

    @Test
    @DisplayName("isCancellable - FILLED 상태는 취소 불가")
    void testIsCancellable_Filled_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.FILLED)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isFalse();
    }

    @Test
    @DisplayName("isCancellable - CANCELLED 상태는 취소 불가")
    void testIsCancellable_Cancelled_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.CANCELLED)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isFalse();
    }

    @Test
    @DisplayName("isCancellable - REJECTED 상태는 취소 불가")
    void testIsCancellable_Rejected_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.REJECTED)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isFalse();
    }

    @Test
    @DisplayName("isCancellable - ERROR 상태는 취소 불가")
    void testIsCancellable_Error_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.ERROR)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isFalse();
    }

    @Test
    @DisplayName("isCancellable - null 상태는 취소 불가")
    void testIsCancellable_Null_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(null)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();

        // Then
        assertThat(cancellable).isFalse();
    }

    // ==================== 2. isModifiable Tests ====================

    @Test
    @DisplayName("isModifiable - NEW 상태는 정정 불가")
    void testIsModifiable_New_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.NEW)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isFalse();
    }

    @Test
    @DisplayName("isModifiable - SENT 상태는 정정 가능")
    void testIsModifiable_Sent_True() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.SENT)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isTrue();
    }

    @Test
    @DisplayName("isModifiable - ACCEPTED 상태는 정정 가능")
    void testIsModifiable_Accepted_True() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.ACCEPTED)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isTrue();
    }

    @Test
    @DisplayName("isModifiable - PART_FILLED 상태는 정정 가능")
    void testIsModifiable_PartFilled_True() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.PART_FILLED)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isTrue();
    }

    @Test
    @DisplayName("isModifiable - FILLED 상태는 정정 불가")
    void testIsModifiable_Filled_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.FILLED)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isFalse();
    }

    @Test
    @DisplayName("isModifiable - CANCELLED 상태는 정정 불가")
    void testIsModifiable_Cancelled_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.CANCELLED)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isFalse();
    }

    @Test
    @DisplayName("isModifiable - REJECTED 상태는 정정 불가")
    void testIsModifiable_Rejected_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.REJECTED)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isFalse();
    }

    @Test
    @DisplayName("isModifiable - ERROR 상태는 정정 불가")
    void testIsModifiable_Error_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.ERROR)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isFalse();
    }

    @Test
    @DisplayName("isModifiable - null 상태는 정정 불가")
    void testIsModifiable_Null_False() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(null)
            .build();

        // When
        boolean modifiable = testOrder.isModifiable();

        // Then
        assertThat(modifiable).isFalse();
    }

    // ==================== 3. validateCancellable Tests ====================

    @Test
    @DisplayName("validateCancellable - 취소 가능 상태는 예외 없음 (SENT)")
    void testValidateCancellable_Sent_NoException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.SENT)
            .build();

        // When & Then - No exception
        testOrder.validateCancellable();
    }

    @Test
    @DisplayName("validateCancellable - 취소 불가 상태는 OrderCancellationException (NEW)")
    void testValidateCancellable_New_ThrowException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.NEW)
            .build();

        // When & Then
        assertThatThrownBy(() -> testOrder.validateCancellable())
            .isInstanceOf(OrderCancellationException.class)
            .hasMessageContaining("Order cannot be cancelled in current state: NEW");
    }

    @Test
    @DisplayName("validateCancellable - 취소 불가 상태는 OrderCancellationException (FILLED)")
    void testValidateCancellable_Filled_ThrowException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.FILLED)
            .build();

        // When & Then
        assertThatThrownBy(() -> testOrder.validateCancellable())
            .isInstanceOf(OrderCancellationException.class)
            .hasMessageContaining("Order cannot be cancelled in current state: FILLED");
    }

    @Test
    @DisplayName("validateCancellable - 취소 불가 상태는 OrderCancellationException (CANCELLED)")
    void testValidateCancellable_Cancelled_ThrowException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.CANCELLED)
            .build();

        // When & Then
        assertThatThrownBy(() -> testOrder.validateCancellable())
            .isInstanceOf(OrderCancellationException.class)
            .hasMessageContaining("Order cannot be cancelled in current state: CANCELLED");
    }

    // ==================== 4. validateModifiable Tests ====================

    @Test
    @DisplayName("validateModifiable - 정정 가능 상태는 예외 없음 (ACCEPTED)")
    void testValidateModifiable_Accepted_NoException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.ACCEPTED)
            .build();

        // When & Then - No exception
        testOrder.validateModifiable();
    }

    @Test
    @DisplayName("validateModifiable - 정정 불가 상태는 OrderModificationException (NEW)")
    void testValidateModifiable_New_ThrowException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.NEW)
            .build();

        // When & Then
        assertThatThrownBy(() -> testOrder.validateModifiable())
            .isInstanceOf(OrderModificationException.class)
            .hasMessageContaining("Order cannot be modified in current state: NEW");
    }

    @Test
    @DisplayName("validateModifiable - 정정 불가 상태는 OrderModificationException (FILLED)")
    void testValidateModifiable_Filled_ThrowException() {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(OrderStatus.FILLED)
            .build();

        // When & Then
        assertThatThrownBy(() -> testOrder.validateModifiable())
            .isInstanceOf(OrderModificationException.class)
            .hasMessageContaining("Order cannot be modified in current state: FILLED");
    }

    // ==================== 5. State Transition Matrix Tests ====================

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("모든 상태에 대한 취소/정정 가능 여부 검증")
    void testCancellableAndModifiable_AllStates(OrderStatus status) {
        // Given
        testOrder = Order.builder()
            .orderId("ORDER_001")
            .status(status)
            .build();

        // When
        boolean cancellable = testOrder.isCancellable();
        boolean modifiable = testOrder.isModifiable();

        // Then - Verify expected behavior per state
        switch (status) {
            case NEW:
                assertThat(cancellable).isFalse();
                assertThat(modifiable).isFalse();
                break;
            case SENT:
            case ACCEPTED:
            case PART_FILLED:
                assertThat(cancellable).isTrue();
                assertThat(modifiable).isTrue();
                break;
            case FILLED:
            case CANCELLED:
            case REJECTED:
            case ERROR:
                assertThat(cancellable).isFalse();
                assertThat(modifiable).isFalse();
                break;
        }
    }

    // ==================== 6. Builder Tests ====================

    @Test
    @DisplayName("Builder - 모든 필드 설정")
    void testBuilder_AllFields() {
        // When
        Order order = Order.builder()
            .orderId("ORDER_001")
            .accountId("ACC_001")
            .strategyId("STRATEGY_001")
            .strategyVersionId("VERSION_001")
            .signalId("SIGNAL_001")
            .symbol("005930")
            .side(Side.BUY)
            .orderType(OrderType.LIMIT)
            .ordDvsn("00")
            .qty(BigDecimal.valueOf(10))
            .price(BigDecimal.valueOf(70000))
            .status(OrderStatus.NEW)
            .idempotencyKey("KEY_001")
            .brokerOrderNo("BROKER_001")
            .build();

        // Then
        assertThat(order.getOrderId()).isEqualTo("ORDER_001");
        assertThat(order.getAccountId()).isEqualTo("ACC_001");
        assertThat(order.getStrategyId()).isEqualTo("STRATEGY_001");
        assertThat(order.getStrategyVersionId()).isEqualTo("VERSION_001");
        assertThat(order.getSignalId()).isEqualTo("SIGNAL_001");
        assertThat(order.getSymbol()).isEqualTo("005930");
        assertThat(order.getSide()).isEqualTo(Side.BUY);
        assertThat(order.getOrderType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getOrdDvsn()).isEqualTo("00");
        assertThat(order.getQty()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(order.getPrice()).isEqualTo(BigDecimal.valueOf(70000));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getIdempotencyKey()).isEqualTo("KEY_001");
        assertThat(order.getBrokerOrderNo()).isEqualTo("BROKER_001");
    }

    // ==================== 7. Test Fixtures Tests ====================

    @Test
    @DisplayName("Test Fixtures - placeMarketOrder 생성")
    void testTestFixtures_PlaceMarketOrder() {
        // When
        Order order = TestFixtures.placeMarketOrder(
            "ORDER_001",
            "ACC_001",
            "005930",
            Side.BUY,
            BigDecimal.valueOf(10),
            "KEY_001"
        );

        // Then
        assertThat(order.getOrderId()).isEqualTo("ORDER_001");
        assertThat(order.getAccountId()).isEqualTo("ACC_001");
        assertThat(order.getSymbol()).isEqualTo("005930");
        assertThat(order.getSide()).isEqualTo(Side.BUY);
        assertThat(order.getOrderType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getQty()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(order.getPrice()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getIdempotencyKey()).isEqualTo("KEY_001");
    }
}
