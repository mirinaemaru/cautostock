package maru.trading.domain.backtest;

import java.util.concurrent.CompletableFuture;

/**
 * Backtest Engine interface.
 *
 * Main orchestrator for running backtest simulations.
 * Supports both synchronous and asynchronous execution.
 *
 * Usage (Synchronous):
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
 *
 * Usage (Asynchronous):
 * <pre>
 * String jobId = backtestEngine.runAsync(config);
 * // Later...
 * BacktestProgress progress = backtestEngine.getProgress(jobId);
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

    /**
     * Run backtest asynchronously.
     *
     * Submits backtest to background executor and returns immediately.
     * Use getProgress() to track execution status.
     *
     * @param config Backtest configuration
     * @return Job ID for tracking progress
     */
    String runAsync(BacktestConfig config);

    /**
     * Run backtest asynchronously with callback.
     *
     * @param config Backtest configuration
     * @return CompletableFuture that completes with result
     */
    CompletableFuture<BacktestResult> runAsyncWithFuture(BacktestConfig config);

    /**
     * Get progress of running backtest.
     *
     * @param jobId Job ID from runAsync()
     * @return Current progress information
     */
    BacktestProgress getProgress(String jobId);

    /**
     * Get result of completed backtest.
     *
     * @param jobId Job ID
     * @return Backtest result if completed, null otherwise
     */
    BacktestResult getResult(String jobId);
}
