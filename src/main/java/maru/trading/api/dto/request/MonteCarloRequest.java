package maru.trading.api.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for Monte Carlo simulation.
 */
@Getter
@Setter
public class MonteCarloRequest {

    /**
     * Backtest ID to use as base for simulation.
     */
    private String backtestId;

    /**
     * Number of simulations to run (default: 1000).
     */
    private Integer numSimulations;

    /**
     * Simulation method: BOOTSTRAP, PERMUTATION, PARAMETRIC (default: BOOTSTRAP).
     */
    private String method;

    /**
     * Confidence level for VaR/CVaR (default: 0.95).
     */
    private BigDecimal confidenceLevel;

    /**
     * Whether to preserve trade sequence correlation (default: false).
     */
    private Boolean preserveCorrelation;

    /**
     * Block size for block bootstrap (default: 5).
     */
    private Integer blockSize;

    /**
     * Random seed for reproducibility (optional).
     */
    private Long randomSeed;

    /**
     * Number of histogram bins for return distribution (default: 50).
     */
    private Integer distributionBins;
}
