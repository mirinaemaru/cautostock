package maru.trading.domain.backtest.walkforward;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.optimization.OptimizationConfig;

import java.time.LocalDate;

/**
 * Walk-Forward Analysis configuration.
 *
 * Walk-Forward analysis divides data into multiple in-sample (training)
 * and out-of-sample (testing) periods, rolling forward through time.
 */
@Getter
@Builder
public class WalkForwardConfig {

    /**
     * Walk-forward analysis ID.
     */
    private final String walkForwardId;

    /**
     * Base backtest configuration.
     */
    private final BacktestConfig baseConfig;

    /**
     * Optimization configuration for in-sample periods.
     */
    private final OptimizationConfig optimizationConfig;

    /**
     * Start date of entire analysis period.
     */
    private final LocalDate analysisStartDate;

    /**
     * End date of entire analysis period.
     */
    private final LocalDate analysisEndDate;

    /**
     * In-sample period length (days).
     *
     * Example: 180 days (6 months)
     */
    @Builder.Default
    private final int inSampleDays = 180;

    /**
     * Out-of-sample period length (days).
     *
     * Example: 90 days (3 months)
     */
    @Builder.Default
    private final int outOfSampleDays = 90;

    /**
     * Step size for rolling window (days).
     *
     * Example: 30 days (1 month)
     * If stepDays < outOfSampleDays, windows will overlap.
     */
    @Builder.Default
    private final int stepDays = 30;

    /**
     * Minimum number of windows required.
     */
    @Builder.Default
    private final int minWindows = 3;

    /**
     * Walk-forward mode.
     *
     * ROLLING: In-sample window moves forward with each step
     * ANCHORED: In-sample always starts from the beginning, growing over time
     */
    @Builder.Default
    private final WalkForwardMode mode = WalkForwardMode.ROLLING;

    /**
     * Walk-forward analysis mode.
     */
    public enum WalkForwardMode {
        /**
         * Rolling mode: Both in-sample start and end move forward.
         * In-sample window size stays constant.
         */
        ROLLING,

        /**
         * Anchored mode: In-sample always starts from analysisStartDate.
         * In-sample window grows over time.
         */
        ANCHORED
    }
}
