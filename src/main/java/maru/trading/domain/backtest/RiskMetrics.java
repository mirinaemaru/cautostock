package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Risk metrics for backtest results.
 *
 * Contains risk analysis statistics for evaluating strategy risk profile.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskMetrics {

    // ========== Volatility ==========

    /**
     * Annualized volatility (standard deviation of returns).
     */
    private BigDecimal volatility;

    /**
     * Downside deviation (volatility of negative returns only).
     */
    private BigDecimal downsideDeviation;

    // ========== Market Risk ==========

    /**
     * Beta coefficient (sensitivity to market movements).
     * Beta > 1: more volatile than market
     * Beta < 1: less volatile than market
     */
    private BigDecimal beta;

    /**
     * Alpha (excess return above market).
     * Formula: StrategyReturn - (RiskFreeRate + Beta * (MarketReturn - RiskFreeRate))
     */
    private BigDecimal alpha;

    // ========== Value at Risk ==========

    /**
     * Value at Risk (VaR) at 95% confidence level.
     * Maximum expected loss over a given time period with 95% confidence.
     */
    private BigDecimal var95;

    /**
     * Conditional Value at Risk (CVaR) at 95% confidence level.
     * Expected loss given that loss exceeds VaR threshold.
     * Also known as Expected Shortfall (ES).
     */
    private BigDecimal cvar95;

    // ========== Risk-Adjusted Returns ==========

    /**
     * Calmar ratio (annualized return / max drawdown).
     * Measures return relative to maximum drawdown risk.
     */
    private BigDecimal calmarRatio;

    /**
     * Information ratio (excess return / tracking error).
     * Measures consistency of excess returns.
     */
    private BigDecimal informationRatio;

    // ========== Additional Risk Metrics ==========

    /**
     * Ulcer index (measure of downside risk).
     * Quantifies depth and duration of drawdowns.
     */
    private BigDecimal ulcerIndex;

    /**
     * Recovery factor (net profit / max drawdown).
     * Higher is better - indicates ability to recover from losses.
     */
    private BigDecimal recoveryFactor;

    /**
     * Risk of ruin (probability of losing all capital).
     * Estimated probability based on win rate and risk/reward.
     */
    private BigDecimal riskOfRuin;

    // ========== Advanced Risk Metrics ==========

    /**
     * Omega ratio - ratio of gains to losses relative to a threshold return.
     * Formula: Sum of returns above threshold / Sum of returns below threshold
     * Higher is better. Value > 1 indicates more gains than losses.
     * Threshold is typically 0 (risk-free rate).
     */
    private BigDecimal omegaRatio;

    /**
     * Return skewness - asymmetry of the return distribution.
     * Positive skew: more large gains than large losses (favorable)
     * Negative skew: more large losses than large gains (unfavorable)
     * Normal distribution has skewness of 0.
     */
    private BigDecimal skewness;

    /**
     * Return kurtosis - "tailedness" of the return distribution.
     * High kurtosis (>3): heavy tails, more extreme returns
     * Low kurtosis (<3): light tails, fewer extreme returns
     * Normal distribution has kurtosis of 3 (excess kurtosis = 0).
     */
    private BigDecimal kurtosis;

    /**
     * Excess kurtosis (kurtosis - 3).
     * Measures deviation from normal distribution.
     * Positive: heavier tails (more extreme events)
     * Negative: lighter tails (fewer extreme events)
     */
    private BigDecimal excessKurtosis;

    /**
     * Kelly criterion - optimal fraction of capital to risk.
     * Formula: (Win Probability * Average Win - Loss Probability * Average Loss) / Average Win
     * Indicates the theoretically optimal position size for geometric growth.
     */
    private BigDecimal kellyFraction;

    /**
     * Half-Kelly - conservative version of Kelly (Kelly / 2).
     * Commonly used in practice to reduce volatility.
     */
    private BigDecimal halfKelly;

    /**
     * Tail ratio - ratio of right tail to left tail.
     * Formula: 95th percentile return / 5th percentile return (absolute)
     * Higher is better - indicates larger gains than losses in extreme cases.
     */
    private BigDecimal tailRatio;

    /**
     * Gain-to-pain ratio - sum of returns / sum of absolute negative returns.
     * Similar to omega ratio but simpler calculation.
     * Higher is better.
     */
    private BigDecimal gainToPainRatio;
}
