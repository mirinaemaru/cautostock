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
}
