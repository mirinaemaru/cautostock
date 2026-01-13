package maru.trading.domain.risk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order frequency tracker.
 *
 * Tracks timestamps of recent orders to enforce frequency limits.
 * Thread-safe through immutable operations.
 */
public class OrderFrequencyTracker {

    private final List<LocalDateTime> orderTimestamps;

    public OrderFrequencyTracker() {
        this.orderTimestamps = new ArrayList<>();
    }

    public OrderFrequencyTracker(List<LocalDateTime> timestamps) {
        this.orderTimestamps = timestamps != null ? new ArrayList<>(timestamps) : new ArrayList<>();
    }

    /**
     * Add a new order timestamp.
     *
     * @param timestamp Order timestamp
     * @return New tracker with added timestamp
     */
    public OrderFrequencyTracker addOrder(LocalDateTime timestamp) {
        List<LocalDateTime> newTimestamps = new ArrayList<>(this.orderTimestamps);
        newTimestamps.add(timestamp);
        return new OrderFrequencyTracker(newTimestamps);
    }

    /**
     * Clean up old timestamps beyond the lookback window.
     *
     * @param cutoffTime Cutoff time (timestamps before this are removed)
     * @return New tracker with cleaned timestamps
     */
    public OrderFrequencyTracker cleanup(LocalDateTime cutoffTime) {
        List<LocalDateTime> recentTimestamps = orderTimestamps.stream()
                .filter(ts -> ts.isAfter(cutoffTime))
                .collect(Collectors.toList());
        return new OrderFrequencyTracker(recentTimestamps);
    }

    /**
     * Count orders within a time window.
     *
     * @param windowStart Start of time window
     * @param windowEnd End of time window
     * @return Number of orders in window
     */
    public long countOrdersInWindow(LocalDateTime windowStart, LocalDateTime windowEnd) {
        return orderTimestamps.stream()
                .filter(ts -> !ts.isBefore(windowStart) && !ts.isAfter(windowEnd))
                .count();
    }

    /**
     * Check if frequency limit would be exceeded by a new order.
     *
     * @param now Current time
     * @param windowSeconds Time window in seconds (e.g., 60 for 1 minute)
     * @param maxOrders Maximum orders allowed in window
     * @return true if limit would be exceeded, false otherwise
     */
    public boolean wouldExceedLimit(LocalDateTime now, int windowSeconds, int maxOrders) {
        LocalDateTime windowStart = now.minusSeconds(windowSeconds);
        long ordersInWindow = countOrdersInWindow(windowStart, now);
        return ordersInWindow >= maxOrders;
    }

    /**
     * Get all timestamps.
     *
     * @return Immutable copy of timestamps
     */
    public List<LocalDateTime> getTimestamps() {
        return new ArrayList<>(orderTimestamps);
    }

    /**
     * Get number of tracked orders.
     *
     * @return Count of timestamps
     */
    public int getCount() {
        return orderTimestamps.size();
    }
}
