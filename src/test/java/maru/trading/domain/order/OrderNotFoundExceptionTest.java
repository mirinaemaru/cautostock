package maru.trading.domain.order;

import maru.trading.domain.shared.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderNotFoundException Test")
class OrderNotFoundExceptionTest {

    @Test
    @DisplayName("Should create exception with order ID")
    void shouldCreateExceptionWithOrderId() {
        String orderId = "TEST-ORDER-123";

        OrderNotFoundException exception = new OrderNotFoundException(orderId);

        assertThat(exception.getMessage()).contains(orderId);
        assertThat(exception.getMessage()).contains("Order not found");
    }

    @Test
    @DisplayName("Should have correct error code")
    void shouldHaveCorrectErrorCode() {
        OrderNotFoundException exception = new OrderNotFoundException("ORDER-456");

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_001);
    }
}
