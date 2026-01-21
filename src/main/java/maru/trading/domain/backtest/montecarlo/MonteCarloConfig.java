package maru.trading.domain.backtest.montecarlo;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.BacktestResult;

import java.math.BigDecimal;

/**
 * Configuration for Monte Carlo simulation.
 *
 * Monte Carlo simulation generates many possible equity curve paths
 * by resampling the original trade returns to estimate the distribution
 * of possible outcomes.
 */
@Getter
@Builder
public class MonteCarloConfig {

    /**
     * Unique simulation ID.
     */
    private final String simulationId;

    /**
     * Base backtest result to simulate from.
     */
    private final BacktestResult baseBacktestResult;

    /**
     * Number of simulations to run.
     */
    @Builder.Default
    private final int numSimulations = 1000;

    /**
     * Simulation method.
     */
    @Builder.Default
    private final SimulationMethod method = SimulationMethod.BOOTSTRAP;

    /**
     * Confidence level for VaR/CVaR calculation (0.0-1.0).
     * Default is 0.95 (95% confidence).
     */
    @Builder.Default
    private final BigDecimal confidenceLevel = new BigDecimal("0.95");

    /**
     * Whether to preserve trade sequence correlation.
     * If true, uses block bootstrap to maintain some autocorrelation.
     */
    @Builder.Default
    private final boolean preserveCorrelation = false;

    /**
     * Block size for block bootstrap (only used when preserveCorrelation is true).
     */
    @Builder.Default
    private final int blockSize = 5;

    /**
     * Seed for random number generator (for reproducibility).
     * If null, uses random seed.
     */
    private final Long randomSeed;

    /**
     * Number of histogram bins for return distribution.
     */
    @Builder.Default
    private final int distributionBins = 50;

    /**
     * Monte Carlo simulation methods.
     */
    public enum SimulationMethod {
        /**
         * Bootstrap - random sampling with replacement from historical returns.
         * Creates new equity curves by randomly selecting trades from the original set.
         */
        BOOTSTRAP,

        /**
         * Permutation - random shuffle of trade order.
         * Preserves all original trades but randomizes the sequence.
         */
        PERMUTATION,

        /**
         * Parametric - assumes normal distribution of returns.
         * Generates random returns based on mean and standard deviation.
         */
        PARAMETRIC
    }
}
