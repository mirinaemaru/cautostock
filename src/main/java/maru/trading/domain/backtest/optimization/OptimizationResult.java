package maru.trading.domain.backtest.optimization;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.BacktestResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result of parameter optimization.
 */
@Getter
@Builder
public class OptimizationResult {

    /**
     * Optimization ID.
     */
    private final String optimizationId;

    /**
     * Optimization configuration.
     */
    private final OptimizationConfig config;

    /**
     * Best parameters found.
     */
    private final Map<String, Object> bestParameters;

    /**
     * Best objective value achieved.
     */
    private final BigDecimal bestObjectiveValue;

    /**
     * Best backtest result.
     */
    private final BacktestResult bestBacktestResult;

    /**
     * All backtest runs (parameter combinations and results).
     */
    private final List<OptimizationRun> allRuns;

    /**
     * Total runs executed.
     */
    private final int totalRuns;

    /**
     * Optimization start time.
     */
    private final LocalDateTime startTime;

    /**
     * Optimization end time.
     */
    private final LocalDateTime endTime;

    /**
     * Duration in milliseconds.
     */
    private final long durationMs;

    /**
     * Single optimization run (one parameter combination).
     */
    @Getter
    @Builder
    public static class OptimizationRun {

        /**
         * Parameter values for this run.
         */
        private final Map<String, Object> parameters;

        /**
         * Backtest result for this run.
         */
        private final BacktestResult backtestResult;

        /**
         * Objective value for this run.
         */
        private final BigDecimal objectiveValue;

        /**
         * Run number (1-based).
         */
        private final int runNumber;
    }
}
