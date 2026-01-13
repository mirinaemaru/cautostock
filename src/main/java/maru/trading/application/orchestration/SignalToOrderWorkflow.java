package maru.trading.application.orchestration;

import lombok.extern.slf4j.Slf4j;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.infra.config.UlidGenerator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Signal to Order Workflow (Phase 3.4).
 *
 * Responsible for converting trading signals into executable orders.
 * Extracted from TradingWorkflow for better separation of concerns.
 *
 * Conversion rules:
 * - SignalType.BUY → Side.BUY
 * - SignalType.SELL → Side.SELL
 * - SignalType.HOLD → null (no order created)
 * - OrderType defaults to MARKET (MVP)
 * - Idempotency key: "sig_" + signalId (prevents duplicate orders from same signal)
 */
@Slf4j
@Component
public class SignalToOrderWorkflow {

    private final UlidGenerator ulidGenerator;

    public SignalToOrderWorkflow(UlidGenerator ulidGenerator) {
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Convert signal to order.
     *
     * @param signal Trading signal
     * @return Order ready for execution, or null if signal is HOLD
     */
    public Order convertToOrder(Signal signal) {
        log.debug("Converting signal to order: signalId={}, type={}, symbol={}",
                signal.getSignalId(), signal.getSignalType(), signal.getSymbol());

        // HOLD signals don't generate orders
        if (signal.getSignalType() == SignalType.HOLD) {
            log.debug("Signal type is HOLD, no order created: reason={}", signal.getReason());
            return null;
        }

        // Generate order ID and idempotency key
        String orderId = ulidGenerator.generateInstance();
        String idempotencyKey = "sig_" + signal.getSignalId();

        // Determine order side from signal type
        Side side = mapSignalTypeToSide(signal.getSignalType());

        // Convert target value to quantity
        BigDecimal qty = signal.getTargetValue();

        // Validate quantity
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid signal target value: signalId={}, targetValue={}",
                    signal.getSignalId(), qty);
            throw new IllegalArgumentException(
                    "Signal target value must be positive: " + signal.getSignalId());
        }

        // MVP: Use market orders by default
        OrderType orderType = OrderType.MARKET;
        String ordDvsn = "01"; // KIS: 01=market order

        // Build order
        Order order = Order.builder()
                .orderId(orderId)
                .accountId(signal.getAccountId())
                .strategyId(signal.getStrategyId())
                .strategyVersionId(signal.getStrategyVersionId())
                .signalId(signal.getSignalId())
                .symbol(signal.getSymbol())
                .side(side)
                .orderType(orderType)
                .ordDvsn(ordDvsn)
                .qty(qty)
                .price(null) // Market orders have no limit price
                .idempotencyKey(idempotencyKey)
                .build();

        log.info("Order created from signal: orderId={}, signalId={}, side={}, qty={}",
                orderId, signal.getSignalId(), side, qty);

        return order;
    }

    /**
     * Map signal type to order side.
     *
     * @param signalType Signal type (BUY/SELL/HOLD)
     * @return Order side (BUY/SELL)
     * @throws IllegalArgumentException if signal type is not BUY or SELL
     */
    private Side mapSignalTypeToSide(SignalType signalType) {
        switch (signalType) {
            case BUY:
                return Side.BUY;
            case SELL:
                return Side.SELL;
            case HOLD:
                throw new IllegalArgumentException("HOLD signals should not be converted to orders");
            default:
                throw new IllegalArgumentException("Unknown signal type: " + signalType);
        }
    }
}
