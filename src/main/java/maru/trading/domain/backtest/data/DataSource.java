package maru.trading.domain.backtest.data;

import java.time.LocalDate;
import java.util.List;

/**
 * Data source interface for backtest data loading.
 *
 * Abstracts data loading from various sources (DB, CSV, API).
 * Implements Iterator pattern for sequential data access.
 *
 * Port interface following Hexagonal Architecture.
 */
public interface DataSource {

    /**
     * Initialize the data source with configuration.
     *
     * @param symbols Symbols to load
     * @param startDate Start date
     * @param endDate End date
     * @param timeframe Bar timeframe (e.g., "1m", "5m", "1d")
     */
    void initialize(List<String> symbols, LocalDate startDate, LocalDate endDate, String timeframe);

    /**
     * Check if more data is available.
     *
     * @return true if next() can be called
     */
    boolean hasNext();

    /**
     * Get next bar in chronological order.
     *
     * @return Next bar data
     * @throws IllegalStateException if no more data available
     */
    BarData next();

    /**
     * Reset to the beginning of data.
     */
    void reset();

    /**
     * Get total number of bars.
     *
     * @return Total bar count
     */
    int getTotalBars();

    /**
     * Get current position (0-based).
     *
     * @return Current index
     */
    int getCurrentIndex();

    /**
     * Get data source type.
     *
     * @return Data source type
     */
    DataSourceType getType();

    /**
     * Close and release resources.
     */
    void close();

    /**
     * Get progress percentage (0-100).
     *
     * @return Progress percentage
     */
    default double getProgress() {
        int total = getTotalBars();
        if (total == 0) {
            return 0.0;
        }
        return (getCurrentIndex() + 1) * 100.0 / total;
    }

    /**
     * Get all loaded bars (for analysis purposes).
     * Use with caution on large datasets.
     *
     * @return List of all bars
     */
    List<BarData> getAllBars();
}
