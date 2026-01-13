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
 * - POST /api/v1/admin/orders/cancel - Cancel an order
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
