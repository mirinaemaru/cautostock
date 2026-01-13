package maru.trading.application.usecase.trading;

import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.broker.BrokerResult;
import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderCancellationException;
import maru.trading.domain.order.OrderStatus;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cancel Order Use Case (Phase 3.3).
 *
 * Workflow:
 * 1. Load order from database
 * 2. Validate order is cancellable (status check)
 * 3. Send cancel request to broker
 * 4. Update order status to CANCELLED
 * 5. Publish ORDER_CANCELLED event
 *
 * Throws OrderCancellationException if order cannot be cancelled.
 */
@Slf4j
@Service
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final BrokerClient brokerClient;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;

    public CancelOrderUseCase(
            OrderRepository orderRepository,
            BrokerClient brokerClient,
            OutboxService outboxService,
            UlidGenerator ulidGenerator) {
        this.orderRepository = orderRepository;
        this.brokerClient = brokerClient;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Execute order cancellation.
     *
     * @param orderId Order ID to cancel
     * @return Updated order with CANCELLED status
     * @throws OrderCancellationException if order cannot be cancelled
     */
    @Transactional
    public Order execute(String orderId) {
        log.info("Cancelling order: orderId={}", orderId);

        // Step 1: Load order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        log.debug("Order loaded: orderId={}, status={}, symbol={}",
                order.getOrderId(), order.getStatus(), order.getSymbol());

        // Step 2: Validate cancellable
        order.validateCancellable();

        // Step 3: Send cancel request to broker
        try {
            BrokerResult result = brokerClient.cancelOrder(orderId);

            if (result.isSuccess()) {
                log.info("Order cancelled successfully at broker: orderId={}", orderId);
            } else {
                log.warn("Broker cancel request failed: orderId={}, error={}",
                        orderId, result.getMessage());
                throw new OrderCancellationException(
                        "Broker rejected cancel request: " + result.getMessage(),
                        orderId,
                        order.getStatus()
                );
            }

        } catch (OrderCancellationException e) {
            throw e; // Re-throw domain exception
        } catch (Exception e) {
            log.error("Failed to send cancel request to broker: orderId={}", orderId, e);
            throw new OrderCancellationException(
                    "Failed to communicate with broker: " + e.getMessage(),
                    orderId,
                    order.getStatus(),
                    e
            );
        }

        // Step 4: Update order status to CANCELLED
        Order updatedOrder = Order.builder()
                .orderId(order.getOrderId())
                .accountId(order.getAccountId())
                .strategyId(order.getStrategyId())
                .strategyVersionId(order.getStrategyVersionId())
                .signalId(order.getSignalId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .orderType(order.getOrderType())
                .ordDvsn(order.getOrdDvsn())
                .qty(order.getQty())
                .price(order.getPrice())
                .status(OrderStatus.CANCELLED)
                .idempotencyKey(order.getIdempotencyKey())
                .brokerOrderNo(order.getBrokerOrderNo())
                .build();

        Order savedOrder = orderRepository.save(updatedOrder);

        log.info("Order status updated to CANCELLED: orderId={}", orderId);

        // Step 5: Publish ORDER_CANCELLED event
        publishOrderCancelledEvent(savedOrder);

        return savedOrder;
    }

    /**
     * Publish ORDER_CANCELLED event to outbox.
     */
    private void publishOrderCancelledEvent(Order order) {
        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("ORDER_CANCELLED")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "orderId", order.getOrderId(),
                        "accountId", order.getAccountId(),
                        "symbol", order.getSymbol(),
                        "side", order.getSide().name(),
                        "qty", order.getQty(),
                        "status", order.getStatus().name()
                ))
                .build();

        outboxService.save(event);
        log.debug("Published ORDER_CANCELLED event: eventId={}", eventId);
    }
}
