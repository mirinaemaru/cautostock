package maru.trading.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderModificationException Test")
class OrderModificationExceptionTest {

    @Test
    @DisplayName("Should create exception with message, orderId and status")
    void shouldCreateExceptionWithMessageOrderIdAndStatus() {
        String message = "Cannot modify filled order";
        String orderId = "ORDER-123";
        OrderStatus status = OrderStatus.FILLED;

        OrderModificationException exception = new OrderModificationException(message, orderId, status);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getOrderId()).isEqualTo(orderId);
        assertThat(exception.getCurrentStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Should create exception with cause")
    void shouldCreateExceptionWithCause() {
        String message = "Cannot modify order";
        String orderId = "ORDER-456";
        OrderStatus status = OrderStatus.REJECTED;
        Throwable cause = new RuntimeException("Original error");

        OrderModificationException exception = new OrderModificationException(message, orderId, status, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getOrderId()).isEqualTo(orderId);
        assertThat(exception.getCurrentStatus()).isEqualTo(status);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be RuntimeException")
    void shouldBeRuntimeException() {
        OrderModificationException exception = new OrderModificationException(
                "test", "ORDER-789", OrderStatus.NEW);

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
