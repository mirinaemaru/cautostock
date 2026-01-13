package maru.trading.application.backtest;

import maru.trading.domain.backtest.VirtualBroker;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Virtual Broker implementation.
 *
 * Simulates realistic order execution with:
 * - Market/Limit order handling
 * - Commission application
 * - Slippage simulation
 * - Cash balance tracking
 */
@Component
public class VirtualBrokerImpl implements VirtualBroker {

    private static final Logger log = LoggerFactory.getLogger(VirtualBrokerImpl.class);

    // Pending orders (orderId -> Order)
    private final Map<String, Order> pendingOrders;

    // All fills executed
    private final List<Fill> allFills;

    // Current cash balance
    private BigDecimal cashBalance;

    // Commission rate (e.g., 0.001 = 0.1%)
    private BigDecimal commission;

    // Slippage rate (e.g., 0.0005 = 0.05%)
    private BigDecimal slippage;

    public VirtualBrokerImpl() {
        this.pendingOrders = new HashMap<>();
        this.allFills = new ArrayList<>();
        this.commission = BigDecimal.valueOf(0.001);
        this.slippage = BigDecimal.valueOf(0.0005);
    }

    @Override
    public void submitOrder(Order order) {
        log.debug("Virtual broker: submit order {} {} {} @ {}",
                order.getSide(), order.getQty(), order.getSymbol(), order.getPrice());

        pendingOrders.put(order.getOrderId(), order);
    }

    @Override
    public List<Fill> processBar(HistoricalBarEntity bar) {
        List<Fill> newFills = new ArrayList<>();

        // Process each pending order
        List<String> toRemove = new ArrayList<>();

        for (Order order : pendingOrders.values()) {
            // Only process orders for this symbol
            if (!order.getSymbol().equals(bar.getSymbol())) {
                continue;
            }

            // Check if order can be filled
            Fill fill = tryFillOrder(order, bar);
            if (fill != null) {
                newFills.add(fill);
                allFills.add(fill);
                toRemove.add(order.getOrderId());

                // Update cash balance
                updateCashBalance(fill);
            }
        }

        // Remove filled orders
        toRemove.forEach(pendingOrders::remove);

        if (!newFills.isEmpty()) {
            log.debug("Virtual broker: executed {} fills at {}", newFills.size(), bar.getBarTimestamp());
        }

        return newFills;
    }

    /**
     * Try to fill an order based on bar data.
     *
     * @param order Order to fill
     * @param bar Market bar data
     * @return Fill if order can be filled, null otherwise
     */
    private Fill tryFillOrder(Order order, HistoricalBarEntity bar) {
        BigDecimal fillPrice = null;

        switch (order.getOrderType()) {
            case MARKET:
                // Market orders fill at open price (assuming submitted before bar)
                fillPrice = bar.getOpenPrice();
                break;

            case LIMIT:
                // Limit BUY: fill if low <= limit price
                // Limit SELL: fill if high >= limit price
                if (order.getSide() == Side.BUY) {
                    if (bar.getLowPrice().compareTo(order.getPrice()) <= 0) {
                        fillPrice = order.getPrice(); // Fill at limit price
                    }
                } else { // SELL
                    if (bar.getHighPrice().compareTo(order.getPrice()) >= 0) {
                        fillPrice = order.getPrice(); // Fill at limit price
                    }
                }
                break;

            default:
                log.warn("Unsupported order type for backtest: {}", order.getOrderType());
                return null;
        }

        if (fillPrice == null) {
            return null; // Order not filled this bar
        }

        // Apply slippage
        BigDecimal slippageAmount = fillPrice.multiply(slippage);
        if (order.getSide() == Side.BUY) {
            fillPrice = fillPrice.add(slippageAmount); // Buy at higher price
        } else {
            fillPrice = fillPrice.subtract(slippageAmount); // Sell at lower price
        }

        // Calculate commission cost (fee)
        BigDecimal fillValue = fillPrice.multiply(order.getQty());
        BigDecimal commissionCost = fillValue.multiply(commission);

        // Create fill
        Fill fill = new Fill(
                UlidGenerator.generate(),  // fillId
                order.getOrderId(),         // orderId
                order.getAccountId(),       // accountId
                order.getSymbol(),          // symbol
                order.getSide(),            // side
                fillPrice,                  // fillPrice
                order.getQty().intValue(),  // fillQty (int)
                commissionCost,             // fee
                BigDecimal.ZERO,            // tax (no tax in backtest)
                bar.getBarTimestamp(),      // fillTimestamp
                "BACKTEST"                  // brokerOrderNo
        );

        log.debug("Virtual fill: {} {} @ {} (slippage: {}, commission: {})",
                order.getSide(), order.getQty(), fillPrice, slippageAmount, commissionCost);

        return fill;
    }

    /**
     * Update cash balance after fill.
     *
     * @param fill Fill to process
     */
    private void updateCashBalance(Fill fill) {
        BigDecimal fillValue = fill.getFillPrice().multiply(BigDecimal.valueOf(fill.getFillQty()));
        BigDecimal commissionCost = fill.getFee();

        if (fill.getSide() == Side.BUY) {
            // BUY: decrease cash (pay for stock + commission)
            cashBalance = cashBalance.subtract(fillValue).subtract(commissionCost);
        } else {
            // SELL: increase cash (receive money - commission)
            cashBalance = cashBalance.add(fillValue).subtract(commissionCost);
        }

        log.debug("Cash balance: {} (after {} fill)", cashBalance, fill.getSide());
    }

    @Override
    public List<Order> getPendingOrders() {
        return new ArrayList<>(pendingOrders.values());
    }

    @Override
    public List<Fill> getAllFills() {
        return new ArrayList<>(allFills);
    }

    @Override
    public boolean cancelOrder(String orderId) {
        return pendingOrders.remove(orderId) != null;
    }

    @Override
    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    @Override
    public void reset(BigDecimal initialCash) {
        pendingOrders.clear();
        allFills.clear();
        cashBalance = initialCash;
        log.info("Virtual broker reset with initial cash: {}", initialCash);
    }

    @Override
    public void setCommission(BigDecimal commission) {
        this.commission = commission;
        log.info("Virtual broker commission set to: {}", commission);
    }

    @Override
    public void setSlippage(BigDecimal slippage) {
        this.slippage = slippage;
        log.info("Virtual broker slippage set to: {}", slippage);
    }
}
