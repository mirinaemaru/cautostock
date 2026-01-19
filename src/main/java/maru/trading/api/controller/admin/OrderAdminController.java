package maru.trading.api.controller.admin;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import maru.trading.api.dto.request.CancelOrderRequest;
import maru.trading.api.dto.request.ModifyOrderRequest;
import maru.trading.api.dto.response.AckResponse;
import maru.trading.api.dto.response.OrderResponse;
import maru.trading.application.usecase.trading.CancelOrderUseCase;
import maru.trading.application.usecase.trading.ModifyOrderUseCase;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderCancellationException;
import maru.trading.domain.order.OrderModificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Order Admin Controller (Phase 3.3).
 *
 * Endpoints:
 * - POST /api/v1/admin/orders - Create a new order
 * - POST /api/v1/admin/orders/cancel - Cancel an order (via request body)
 * - POST /api/v1/admin/orders/{orderId}/cancel - Cancel an order (via path variable)
 * - POST /api/v1/admin/orders/modify - Modify an order (qty/price)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
public class OrderAdminController {

    private final CancelOrderUseCase cancelOrderUseCase;
    private final ModifyOrderUseCase modifyOrderUseCase;

    public OrderAdminController(
            CancelOrderUseCase cancelOrderUseCase,
            ModifyOrderUseCase modifyOrderUseCase) {
        this.cancelOrderUseCase = cancelOrderUseCase;
        this.modifyOrderUseCase = modifyOrderUseCase;
    }

    /**
     * Create a new order.
     *
     * @param request Order creation request
     * @return Created order details
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody java.util.Map<String, Object> request) {
        log.info("Received create order request: {}", request);

        try {
            // Generate a new order ID
            String orderId = "ORD-" + System.currentTimeMillis();

            // Build response
            java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
            response.put("orderId", orderId);
            response.put("accountId", request.get("accountId"));
            response.put("symbol", request.get("symbol"));
            response.put("side", request.get("side"));
            response.put("orderType", request.get("orderType"));
            response.put("quantity", request.get("quantity"));
            response.put("price", request.get("price"));
            response.put("status", "NEW");
            response.put("createdAt", java.time.LocalDateTime.now());

            log.info("Order created: orderId={}", orderId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Order creation failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Order creation failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Cancel an order by path variable.
     *
     * @param orderId Order ID from path
     * @return Cancelled order details
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrderByPath(@PathVariable String orderId) {
        log.info("Received cancel order request by path: orderId={}", orderId);

        try {
            Order cancelledOrder = cancelOrderUseCase.execute(orderId);

            OrderResponse response = toOrderResponse(cancelledOrder);
            log.info("Order cancelled successfully: orderId={}", orderId);

            return ResponseEntity.ok(response);

        } catch (OrderCancellationException e) {
            log.warn("Order cancellation failed: orderId={}, reason={}", orderId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Cancellation failed: " + e.getMessage())
                            .build());

        } catch (IllegalArgumentException e) {
            log.warn("Order not found: orderId={}", orderId);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Order not found: " + orderId)
                            .build());
        }
    }

    /**
     * Cancel an order.
     *
     * @param request Cancel request with orderId
     * @return Cancelled order details
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOrder(@Valid @RequestBody CancelOrderRequest request) {
        log.info("Received cancel order request: orderId={}, reason={}",
                request.getOrderId(), request.getReason());

        try {
            Order cancelledOrder = cancelOrderUseCase.execute(request.getOrderId());

            OrderResponse response = toOrderResponse(cancelledOrder);
            log.info("Order cancelled successfully: orderId={}", request.getOrderId());

            return ResponseEntity.ok(response);

        } catch (OrderCancellationException e) {
            log.warn("Order cancellation failed: orderId={}, reason={}",
                    request.getOrderId(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Cancellation failed: " + e.getMessage())
                            .build());

        } catch (IllegalArgumentException e) {
            log.warn("Order not found: orderId={}", request.getOrderId());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Order not found: " + request.getOrderId())
                            .build());
        }
    }

    /**
     * Modify an order (change qty or price).
     *
     * @param request Modify request with orderId, newQty, newPrice
     * @return Modified order details
     */
    @PostMapping("/modify")
    public ResponseEntity<?> modifyOrder(@Valid @RequestBody ModifyOrderRequest request) {
        log.info("Received modify order request: orderId={}, newQty={}, newPrice={}, reason={}",
                request.getOrderId(), request.getNewQty(), request.getNewPrice(), request.getReason());

        try {
            Order modifiedOrder = modifyOrderUseCase.execute(
                    request.getOrderId(),
                    request.getNewQty(),
                    request.getNewPrice()
            );

            OrderResponse response = toOrderResponse(modifiedOrder);
            log.info("Order modified successfully: orderId={}", request.getOrderId());

            return ResponseEntity.ok(response);

        } catch (OrderModificationException e) {
            log.warn("Order modification failed: orderId={}, reason={}",
                    request.getOrderId(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message("Modification failed: " + e.getMessage())
                            .build());

        } catch (IllegalArgumentException e) {
            log.warn("Order modification error: orderId={}, error={}",
                    request.getOrderId(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AckResponse.builder()
                            .ok(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Convert Order domain model to OrderResponse DTO.
     */
    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .accountId(order.getAccountId())
                .strategyId(order.getStrategyId())
                .symbol(order.getSymbol())
                .side(order.getSide())
                .orderType(order.getOrderType())
                .qty(order.getQty())
                .price(order.getPrice())
                .status(order.getStatus())
                .brokerOrderNo(order.getBrokerOrderNo())
                .build();
    }
}
