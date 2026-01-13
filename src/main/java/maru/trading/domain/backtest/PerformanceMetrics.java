package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Performance metrics for backtest results.
 *
 * Contains key statistics for evaluating strategy performance.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceMetrics {

    // ========== Returns ==========

    /**
     * Total return percentage.
     */
    private BigDecimal totalReturn;

    /**
     * Annualized return percentage.
     */
    private BigDecimal annualReturn;

    /**
     * Sharpe ratio (risk-adjusted return).
     * Formula: (Return - RiskFreeRate) / Volatility
     */
    private BigDecimal sharpeRatio;

    /**
     * Sortino ratio (downside risk-adjusted return).
     * Similar to Sharpe but only considers downside volatility.
     */
    private BigDecimal sortinoRatio;

    // ========== Drawdown ==========

    /**
     * Maximum drawdown percentage (peak-to-trough decline).
     */
    private BigDecimal maxDrawdown;

    /**
     * Maximum drawdown duration in days.
     */
    private Integer maxDrawdownDuration;

    // ========== Trade Statistics ==========

    /**
     * Total number of trades executed.
     */
    private Integer totalTrades;

    /**
     * Number of winning trades (profit > 0).
     */
    private Integer winningTrades;

    /**
     * Number of losing trades (profit < 0).
     */
    private Integer losingTrades;

    /**
     * Win rate percentage.
     * Formula: (WinningTrades / TotalTrades) * 100
     */
    private BigDecimal winRate;

    /**
     * Profit factor (gross profit / gross loss).
     * Values > 1 indicate profitable strategy.
     */
    private BigDecimal profitFactor;

    /**
     * Average profit per winning trade.
     */
    private BigDecimal avgWin;

    /**
     * Average loss per losing trade (absolute value).
     */
    private BigDecimal avgLoss;

    /**
     * Average profit/loss per trade.
     */
    private BigDecimal avgTrade;

    /**
     * Largest winning trade.
     */
    private BigDecimal largestWin;

    /**
     * Largest losing trade (absolute value).
     */
    private BigDecimal largestLoss;

    // ========== Additional Metrics ==========

    /**
     * Total profit from all winning trades.
     */
    private BigDecimal totalProfit;

    /**
     * Total loss from all losing trades (absolute value).
     */
    private BigDecimal totalLoss;

    /**
     * Number of consecutive winning trades (max streak).
     */
    private Integer maxConsecutiveWins;

    /**
     * Number of consecutive losing trades (max streak).
     */
    private Integer maxConsecutiveLosses;
}
