package maru.trading.domain.backtest.data;

/**
 * Data source types for backtest data loading.
 */
public enum DataSourceType {

    /**
     * Load from database (default).
     */
    DATABASE,

    /**
     * Load from CSV file.
     */
    CSV,

    /**
     * Load from real-time feed (for paper trading).
     */
    REALTIME
}
