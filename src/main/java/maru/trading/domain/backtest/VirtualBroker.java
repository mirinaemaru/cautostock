package maru.trading.domain.backtest;

import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Order;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Virtual Broker interface.
 *
 * Simulates order execution during backtest without real market interaction.
 * Applies commission and slippage to simulate realistic trading costs.
 */
public interface VirtualBroker {

    /**
     * Submit order for execution.
     *
     * Orders are held until market data triggers execution.
     *
     * @param order Order to submit
     */
    void submitOrder(Order order);

    /**
     * Process market data and execute pending orders.
     *
     * Checks if pending orders can be filled based on bar data.
     * Applies slippage and commission to fills.
     *
     * @param bar Current market bar
     * @return List of fills generated
     */
    List<Fill> processBar(HistoricalBarEntity bar);

    /**
     * Get pending orders (not yet filled).
     *
     * @return List of pending orders
     */
    List<Order> getPendingOrders();

    /**
     * Get all fills executed so far.
     *
     * @return List of all fills
     */
    List<Fill> getAllFills();

    /**
     * Cancel pending order.
     *
     * @param orderId Order ID to cancel
     * @return true if order was canceled, false if not found
     */
    boolean cancelOrder(String orderId);

    /**
     * Get current cash balance.
     *
     * @return Cash balance
     */
    BigDecimal getCashBalance();

    /**
     * Reset broker state (for new backtest).
     *
     * @param initialCash Initial cash balance
     */
    void reset(BigDecimal initialCash);

    /**
     * Set commission rate.
     *
     * @param commission Commission rate (e.g., 0.001 = 0.1%)
     */
    void setCommission(BigDecimal commission);

    /**
     * Set slippage rate.
     *
     * @param slippage Slippage rate (e.g., 0.0005 = 0.05%)
     */
    void setSlippage(BigDecimal slippage);
}
