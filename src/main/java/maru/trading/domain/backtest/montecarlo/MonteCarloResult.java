package maru.trading.domain.backtest.montecarlo;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result of Monte Carlo simulation.
 *
 * Contains statistical analysis of simulated outcomes including
 * probability distributions, risk metrics, and confidence intervals.
 */
@Getter
@Builder
public class MonteCarloResult {

    /**
     * Unique simulation ID.
     */
    private final String simulationId;

    /**
     * Configuration used for simulation.
     */
    private final MonteCarloConfig config;

    /**
     * Number of simulations actually run.
     */
    private final int numSimulations;

    // ========== Return Statistics ==========

    /**
     * Mean (average) return across all simulations.
     */
    private final BigDecimal meanReturn;

    /**
     * Median return across all simulations.
     */
    private final BigDecimal medianReturn;

    /**
     * Standard deviation of returns.
     */
    private final BigDecimal stdDevReturn;

    /**
     * Minimum return observed.
     */
    private final BigDecimal minReturn;

    /**
     * Maximum return observed.
     */
    private final BigDecimal maxReturn;

    // ========== Risk Metrics ==========

    /**
     * Value at Risk at configured confidence level.
     * Maximum expected loss at confidence level (e.g., 95%).
     */
    private final BigDecimal valueAtRisk;

    /**
     * Conditional Value at Risk (Expected Shortfall).
     * Expected loss given that loss exceeds VaR.
     */
    private final BigDecimal conditionalVaR;

    /**
     * Maximum drawdown statistics.
     */
    private final DrawdownStatistics maxDrawdownStats;

    // ========== Probability Metrics ==========

    /**
     * Probability of achieving profit (return > 0).
     */
    private final BigDecimal probabilityOfProfit;

    /**
     * Probability of achieving target return.
     */
    private final BigDecimal probabilityOfTargetReturn;

    /**
     * Target return used for probability calculation.
     */
    private final BigDecimal targetReturn;

    /**
     * Probability of ruin (losing more than specified % of capital).
     */
    private final BigDecimal probabilityOfRuin;

    /**
     * Ruin threshold percentage used.
     */
    private final BigDecimal ruinThreshold;

    // ========== Percentiles ==========

    /**
     * Return percentiles (5th, 10th, 25th, 50th, 75th, 90th, 95th).
     */
    private final Map<Integer, BigDecimal> returnPercentiles;

    /**
     * Drawdown percentiles.
     */
    private final Map<Integer, BigDecimal> drawdownPercentiles;

    // ========== Distribution ==========

    /**
     * Return distribution histogram.
     * Key: bin center value, Value: frequency count.
     */
    private final List<DistributionBin> returnDistribution;

    /**
     * Final equity distribution histogram.
     */
    private final List<DistributionBin> equityDistribution;

    // ========== Confidence Intervals ==========

    /**
     * 95% confidence interval for expected return [lower, upper].
     */
    private final BigDecimal[] returnConfidenceInterval95;

    /**
     * 99% confidence interval for expected return [lower, upper].
     */
    private final BigDecimal[] returnConfidenceInterval99;

    // ========== Best/Worst Cases ==========

    /**
     * Best case scenario (highest return simulation).
     */
    private final SimulationPath bestCase;

    /**
     * Worst case scenario (lowest return simulation).
     */
    private final SimulationPath worstCase;

    /**
     * Median case scenario.
     */
    private final SimulationPath medianCase;

    // ========== Execution Info ==========

    /**
     * Simulation start time.
     */
    private final LocalDateTime startTime;

    /**
     * Simulation end time.
     */
    private final LocalDateTime endTime;

    /**
     * Duration in milliseconds.
     */
    private final long durationMs;

    /**
     * Drawdown statistics from simulations.
     */
    @Getter
    @Builder
    public static class DrawdownStatistics {
        private final BigDecimal meanMaxDrawdown;
        private final BigDecimal medianMaxDrawdown;
        private final BigDecimal stdDevMaxDrawdown;
        private final BigDecimal worstMaxDrawdown;
        private final BigDecimal bestMaxDrawdown;
    }

    /**
     * Distribution histogram bin.
     */
    @Getter
    @Builder
    public static class DistributionBin {
        private final BigDecimal binStart;
        private final BigDecimal binEnd;
        private final BigDecimal binCenter;
        private final int count;
        private final BigDecimal frequency;  // count / total
    }

    /**
     * Individual simulation path summary.
     */
    @Getter
    @Builder
    public static class SimulationPath {
        private final int simulationNumber;
        private final BigDecimal totalReturn;
        private final BigDecimal maxDrawdown;
        private final BigDecimal finalEquity;
        private final List<BigDecimal> equityCurve;  // Optional: sampled points
    }
}
