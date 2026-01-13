package maru.trading.domain.backtest;

import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;

import java.time.LocalDateTime;

/**
 * Data Replay Engine interface.
 *
 * Replays historical market data chronologically to simulate live trading.
 * Prevents lookahead bias by only providing data available up to current timestamp.
 *
 * Usage (Iterator pattern):
 * <pre>
 * DataReplayEngine engine = ...;
 * engine.loadData(config);
 *
 * while (engine.hasNext()) {
 *     HistoricalBarEntity bar = engine.next();
 *     // Execute strategy with bar data
 * }
 * </pre>
 */
public interface DataReplayEngine {

    /**
     * Load historical data for replay.
     *
     * Loads data from database for specified symbols and date range,
     * sorted by timestamp in ascending order.
     *
     * @param config Backtest configuration
     */
    void loadData(BacktestConfig config);

    /**
     * Check if more data is available.
     *
     * @return true if next() can be called
     */
    boolean hasNext();

    /**
     * Get next bar in chronological order.
     *
     * @return Next historical bar
     * @throws IllegalStateException if no more data available
     */
    HistoricalBarEntity next();

    /**
     * Reset replay to beginning.
     */
    void reset();

    /**
     * Get current replay timestamp.
     *
     * @return Current timestamp in replay
     */
    LocalDateTime getCurrentTime();

    /**
     * Get total number of bars loaded.
     *
     * @return Total bar count
     */
    int getTotalBars();

    /**
     * Get current bar index (0-based).
     *
     * @return Current position in replay
     */
    int getCurrentIndex();
}
