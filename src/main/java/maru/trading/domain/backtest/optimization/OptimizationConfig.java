package maru.trading.domain.backtest.optimization;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.BacktestConfig;

import java.util.List;
import java.util.Map;

/**
 * Configuration for parameter optimization.
 */
@Getter
@Builder
public class OptimizationConfig {

    /**
     * Unique optimization ID.
     */
    private final String optimizationId;

    /**
     * Base backtest configuration.
     */
    private final BacktestConfig baseConfig;

    /**
     * Parameters to optimize with their value ranges.
     *
     * Example:
     * {
     *   "shortPeriod": [5, 10, 15, 20],
     *   "longPeriod": [20, 30, 50, 100]
     * }
     */
    private final Map<String, List<Object>> parameterRanges;

    /**
     * Optimization method.
     */
    @Builder.Default
    private final OptimizationMethod method = OptimizationMethod.GRID_SEARCH;

    /**
     * Optimization objective (metric to maximize).
     */
    @Builder.Default
    private final OptimizationObjective objective = OptimizationObjective.SHARPE_RATIO;

    /**
     * Maximum number of backtest runs.
     *
     * For grid search: all combinations
     * For random search: random sample size
     */
    @Builder.Default
    private final int maxRuns = 1000;

    /**
     * Optimization methods.
     */
    public enum OptimizationMethod {
        /**
         * Grid search - test all parameter combinations.
         */
        GRID_SEARCH,

        /**
         * Random search - test random parameter combinations.
         */
        RANDOM_SEARCH
    }

    /**
     * Optimization objectives (metrics to maximize).
     */
    public enum OptimizationObjective {
        /**
         * Maximize total return.
         */
        TOTAL_RETURN,

        /**
         * Maximize Sharpe ratio.
         */
        SHARPE_RATIO,

        /**
         * Maximize Sortino ratio.
         */
        SORTINO_RATIO,

        /**
         * Maximize profit factor.
         */
        PROFIT_FACTOR,

        /**
         * Maximize Calmar ratio (return / max drawdown).
         */
        CALMAR_RATIO
    }
}
