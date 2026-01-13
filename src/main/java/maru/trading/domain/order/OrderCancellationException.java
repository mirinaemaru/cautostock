package maru.trading.domain.order;

/**
 * Exception thrown when attempting to cancel an order that cannot be cancelled.
 *
 * Reasons:
 * - Order already filled (FILLED)
 * - Order already cancelled (CANCELLED)
 * - Order rejected or in error state (REJECTED, ERROR)
 * - Order not yet sent to broker (NEW)
 */
public class OrderCancellationException extends RuntimeException {

    private final String orderId;
    private final OrderStatus currentStatus;

    public OrderCancellationException(String message, String orderId, OrderStatus currentStatus) {
        super(message);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public OrderCancellationException(String message, String orderId, OrderStatus currentStatus, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public String getOrderId() {
        return orderId;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
