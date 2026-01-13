package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Equity curve (capital over time).
 *
 * Tracks portfolio value throughout the backtest period.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquityCurve {

    /**
     * List of equity data points.
     */
    @Builder.Default
    private List<EquityPoint> points = new ArrayList<>();

    /**
     * Add an equity point.
     */
    public void addPoint(LocalDateTime timestamp, BigDecimal equity) {
        points.add(new EquityPoint(timestamp, equity));
    }

    /**
     * Get current equity (last point).
     */
    public BigDecimal getCurrentEquity() {
        if (points.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return points.get(points.size() - 1).getEquity();
    }

    /**
     * Get initial equity (first point).
     */
    public BigDecimal getInitialEquity() {
        if (points.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return points.get(0).getEquity();
    }

    /**
     * Single equity data point.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EquityPoint {
        private LocalDateTime timestamp;
        private BigDecimal equity;
    }
}
