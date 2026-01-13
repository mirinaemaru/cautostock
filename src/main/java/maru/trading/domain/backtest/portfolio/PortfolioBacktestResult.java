package maru.trading.domain.backtest.portfolio;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.BacktestResult;
import maru.trading.domain.backtest.PerformanceMetrics;
import maru.trading.domain.backtest.RiskMetrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Portfolio backtest result.
 */
@Getter
@Builder
public class PortfolioBacktestResult {

    /**
     * Portfolio backtest ID.
     */
    private final String portfolioBacktestId;

    /**
     * Configuration used.
     */
    private final PortfolioBacktestConfig config;

    /**
     * Individual symbol backtest results.
     */
    private final Map<String, BacktestResult> symbolResults;

    /**
     * Portfolio-level performance metrics.
     */
    private final PerformanceMetrics portfolioMetrics;

    /**
     * Portfolio-level risk metrics.
     */
    private final RiskMetrics portfolioRiskMetrics;

    /**
     * Final capital for entire portfolio.
     */
    private final BigDecimal finalCapital;

    /**
     * Total return for portfolio.
     */
    private final BigDecimal totalReturn;

    /**
     * Portfolio equity curve (combined).
     */
    private final List<PortfolioEquityPoint> equityCurve;

    /**
     * Correlation matrix between symbols.
     */
    private final Map<String, Map<String, BigDecimal>> correlationMatrix;

    /**
     * Start time.
     */
    private final LocalDateTime startTime;

    /**
     * End time.
     */
    private final LocalDateTime endTime;

    /**
     * Duration in milliseconds.
     */
    private final long durationMs;

    /**
     * Portfolio equity point (combined value).
     */
    @Getter
    @Builder
    public static class PortfolioEquityPoint {

        /**
         * Timestamp.
         */
        private final LocalDateTime timestamp;

        /**
         * Total portfolio equity.
         */
        private final BigDecimal totalEquity;

        /**
         * Individual symbol equities.
         */
        private final Map<String, BigDecimal> symbolEquities;
    }
}
