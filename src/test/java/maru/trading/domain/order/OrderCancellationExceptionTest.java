package maru.trading.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderCancellationException Test")
class OrderCancellationExceptionTest {

    @Test
    @DisplayName("Should create exception with message, orderId and status")
    void shouldCreateExceptionWithMessageOrderIdAndStatus() {
        String message = "Cannot cancel filled order";
        String orderId = "ORDER-123";
        OrderStatus status = OrderStatus.FILLED;

        OrderCancellationException exception = new OrderCancellationException(message, orderId, status);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getOrderId()).isEqualTo(orderId);
        assertThat(exception.getCurrentStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Should create exception with cause")
    void shouldCreateExceptionWithCause() {
        String message = "Cannot cancel order";
        String orderId = "ORDER-456";
        OrderStatus status = OrderStatus.CANCELLED;
        Throwable cause = new RuntimeException("Original error");

        OrderCancellationException exception = new OrderCancellationException(message, orderId, status, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getOrderId()).isEqualTo(orderId);
        assertThat(exception.getCurrentStatus()).isEqualTo(status);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should be RuntimeException")
    void shouldBeRuntimeException() {
        OrderCancellationException exception = new OrderCancellationException(
                "test", "ORDER-789", OrderStatus.ERROR);

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}
