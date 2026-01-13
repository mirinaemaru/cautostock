package maru.trading.domain.backtest;

/**
 * Backtest Engine interface.
 *
 * Main orchestrator for running backtest simulations.
 *
 * Usage:
 * <pre>
 * BacktestConfig config = BacktestConfig.builder()
 *     .strategyId("STR_001")
 *     .startDate(LocalDate.of(2024, 1, 1))
 *     .endDate(LocalDate.of(2024, 12, 31))
 *     .symbols(List.of("005930", "000660"))
 *     .build();
 *
 * BacktestResult result = backtestEngine.run(config);
 * </pre>
 */
public interface BacktestEngine {

    /**
     * Run a backtest simulation.
     *
     * Process:
     * 1. Load historical data
     * 2. Initialize virtual broker
     * 3. Replay data chronologically
     * 4. Execute strategy for each bar
     * 5. Simulate order execution
     * 6. Calculate performance metrics
     *
     * @param config Backtest configuration
     * @return Backtest result with performance metrics
     * @throws BacktestException if backtest execution fails
     */
    BacktestResult run(BacktestConfig config) throws BacktestException;

    /**
     * Validate backtest configuration.
     *
     * Checks:
     * - Date range is valid
     * - Symbols exist in historical data
     * - Strategy parameters are valid
     * - Sufficient data available
     *
     * @param config Backtest configuration
     * @throws IllegalArgumentException if configuration is invalid
     */
    void validateConfig(BacktestConfig config);

    /**
     * Get backtest status (for async execution).
     *
     * @param backtestId Backtest ID
     * @return Current status (RUNNING, COMPLETED, FAILED)
     */
    String getStatus(String backtestId);

    /**
     * Cancel running backtest (for async execution).
     *
     * @param backtestId Backtest ID
     */
    void cancel(String backtestId);
}
