package maru.trading.domain.backtest.walkforward;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.BacktestResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Walk-Forward Analysis result.
 */
@Getter
@Builder
public class WalkForwardResult {

    /**
     * Walk-forward analysis ID.
     */
    private final String walkForwardId;

    /**
     * Configuration used.
     */
    private final WalkForwardConfig config;

    /**
     * All windows (in-sample + out-of-sample pairs).
     */
    private final List<WalkForwardWindow> windows;

    /**
     * Combined out-of-sample performance.
     */
    private final BigDecimal combinedOutOfSampleReturn;

    /**
     * Average out-of-sample Sharpe ratio.
     */
    private final BigDecimal avgOutOfSampleSharpeRatio;

    /**
     * Stability score (0-1).
     *
     * Measures consistency across windows.
     * 1.0 = perfectly consistent
     * 0.0 = highly inconsistent
     */
    private final BigDecimal stabilityScore;

    /**
     * Total windows analyzed.
     */
    private final int totalWindows;

    /**
     * Analysis start time.
     */
    private final LocalDateTime startTime;

    /**
     * Analysis end time.
     */
    private final LocalDateTime endTime;

    /**
     * Duration in milliseconds.
     */
    private final long durationMs;

    /**
     * Single walk-forward window.
     */
    @Getter
    @Builder
    public static class WalkForwardWindow {

        /**
         * Window number (1-based).
         */
        private final int windowNumber;

        /**
         * In-sample period start.
         */
        private final LocalDate inSampleStart;

        /**
         * In-sample period end.
         */
        private final LocalDate inSampleEnd;

        /**
         * Out-of-sample period start.
         */
        private final LocalDate outOfSampleStart;

        /**
         * Out-of-sample period end.
         */
        private final LocalDate outOfSampleEnd;

        /**
         * Best parameters found in in-sample optimization.
         */
        private final Map<String, Object> optimizedParameters;

        /**
         * In-sample backtest result (with optimized parameters).
         */
        private final BacktestResult inSampleResult;

        /**
         * Out-of-sample backtest result (with optimized parameters).
         */
        private final BacktestResult outOfSampleResult;

        /**
         * In-sample performance metric.
         */
        private final BigDecimal inSampleMetric;

        /**
         * Out-of-sample performance metric.
         */
        private final BigDecimal outOfSampleMetric;

        /**
         * Performance degradation (in-sample - out-of-sample).
         *
         * Positive value indicates overfitting.
         */
        private final BigDecimal performanceDegradation;
    }
}
