package maru.trading.application.usecase.trading;

import lombok.extern.slf4j.Slf4j;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.ports.broker.BrokerResult;
import maru.trading.application.ports.repo.OrderRepository;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderModificationException;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Modify Order Use Case (Phase 3.3).
 *
 * Workflow:
 * 1. Load order from database
 * 2. Validate order is modifiable (status check)
 * 3. Send modify request to broker (qty and/or price change)
 * 4. Update order with new values
 * 5. Publish ORDER_MODIFIED event
 *
 * Throws OrderModificationException if order cannot be modified.
 */
@Slf4j
@Service
public class ModifyOrderUseCase {

    private final OrderRepository orderRepository;
    private final BrokerClient brokerClient;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;

    public ModifyOrderUseCase(
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
     * Execute order modification.
     *
     * @param orderId Order ID to modify
     * @param newQty New quantity (null to keep current)
     * @param newPrice New price (null to keep current)
     * @return Updated order with new values
     * @throws OrderModificationException if order cannot be modified
     */
    @Transactional
    public Order execute(String orderId, BigDecimal newQty, BigDecimal newPrice) {
        log.info("Modifying order: orderId={}, newQty={}, newPrice={}", orderId, newQty, newPrice);

        // Validate at least one parameter is provided
        if (newQty == null && newPrice == null) {
            throw new IllegalArgumentException("At least one of newQty or newPrice must be provided");
        }

        // Step 1: Load order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        log.debug("Order loaded: orderId={}, status={}, currentQty={}, currentPrice={}",
                order.getOrderId(), order.getStatus(), order.getQty(), order.getPrice());

        // Step 2: Validate modifiable
        order.validateModifiable();

        // Store original values for audit trail
        BigDecimal originalQty = order.getQty();
        BigDecimal originalPrice = order.getPrice();

        // Determine final values
        BigDecimal finalQty = newQty != null ? newQty : order.getQty();
        BigDecimal finalPrice = newPrice != null ? newPrice : order.getPrice();

        // Step 3: Send modify request to broker
        try {
            BrokerResult result = brokerClient.modifyOrder(orderId, newQty, newPrice);

            if (result.isSuccess()) {
                log.info("Order modified successfully at broker: orderId={}", orderId);
            } else {
                log.warn("Broker modify request failed: orderId={}, error={}",
                        orderId, result.getMessage());
                throw new OrderModificationException(
                        "Broker rejected modify request: " + result.getMessage(),
                        orderId,
                        order.getStatus()
                );
            }

        } catch (OrderModificationException e) {
            throw e; // Re-throw domain exception
        } catch (Exception e) {
            log.error("Failed to send modify request to broker: orderId={}", orderId, e);
            throw new OrderModificationException(
                    "Failed to communicate with broker: " + e.getMessage(),
                    orderId,
                    order.getStatus(),
                    e
            );
        }

        // Step 4: Update order with new values
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
                .qty(finalQty)
                .price(finalPrice)
                .status(order.getStatus()) // Status remains same (SENT/ACCEPTED/PART_FILLED)
                .idempotencyKey(order.getIdempotencyKey())
                .brokerOrderNo(order.getBrokerOrderNo())
                .build();

        Order savedOrder = orderRepository.save(updatedOrder);

        log.info("Order modified: orderId={}, qty: {} -> {}, price: {} -> {}",
                orderId, originalQty, finalQty, originalPrice, finalPrice);

        // Step 5: Publish ORDER_MODIFIED event
        publishOrderModifiedEvent(savedOrder, originalQty, originalPrice);

        return savedOrder;
    }

    /**
     * Publish ORDER_MODIFIED event to outbox.
     */
    private void publishOrderModifiedEvent(Order order, BigDecimal originalQty, BigDecimal originalPrice) {
        String eventId = ulidGenerator.generateInstance();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getOrderId());
        payload.put("accountId", order.getAccountId());
        payload.put("symbol", order.getSymbol());
        payload.put("side", order.getSide().name());
        payload.put("originalQty", originalQty);
        payload.put("newQty", order.getQty());
        payload.put("originalPrice", originalPrice);
        payload.put("newPrice", order.getPrice());
        payload.put("status", order.getStatus().name());

        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("ORDER_MODIFIED")
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build();

        outboxService.save(event);
        log.debug("Published ORDER_MODIFIED event: eventId={}", eventId);
    }
}
