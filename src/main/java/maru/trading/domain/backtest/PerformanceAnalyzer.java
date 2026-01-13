package maru.trading.domain.backtest;

/**
 * Performance Analyzer interface.
 *
 * Analyzes backtest results and calculates performance metrics.
 */
public interface PerformanceAnalyzer {

    /**
     * Calculate performance metrics from backtest result.
     *
     * Calculates:
     * - Total/Annual returns
     * - Sharpe/Sortino ratios
     * - Max drawdown
     * - Win rate, profit factor
     * - Average win/loss
     *
     * @param result Backtest result
     * @return Performance metrics
     */
    PerformanceMetrics analyze(BacktestResult result);

    /**
     * Calculate risk metrics from backtest result.
     *
     * Calculates:
     * - Volatility
     * - Beta/Alpha
     * - VaR/CVaR
     * - Calmar ratio
     *
     * @param result Backtest result
     * @return Risk metrics
     */
    RiskMetrics analyzeRisk(BacktestResult result);

    /**
     * Generate equity curve from backtest result.
     *
     * Creates time-series of portfolio value over the backtest period.
     *
     * @param result Backtest result
     * @return Equity curve
     */
    EquityCurve generateEquityCurve(BacktestResult result);
}
