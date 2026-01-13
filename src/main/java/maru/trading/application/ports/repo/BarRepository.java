package maru.trading.application.ports.repo;

import maru.trading.domain.market.MarketBar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for market bars.
 *
 * Abstracts persistence layer from domain/application layers.
 */
public interface BarRepository {

    /**
     * Save a market bar.
     *
     * @param bar Market bar to save
     * @return Saved bar
     */
    MarketBar save(MarketBar bar);

    /**
     * Find bar by symbol, timeframe, and timestamp.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe (e.g., "1m")
     * @param barTimestamp Bar timestamp
     * @return Optional containing bar if found
     */
    Optional<MarketBar> findBySymbolAndTimeframeAndTimestamp(
            String symbol, String timeframe, LocalDateTime barTimestamp);

    /**
     * Find N most recent closed bars for a symbol.
     * Returns bars ordered from oldest to newest (ascending by timestamp).
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @param count Number of bars to retrieve
     * @return List of bars (oldest first)
     */
    List<MarketBar> findRecentClosedBars(String symbol, String timeframe, int count);

    /**
     * Find bars within a time range.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @param startTime Start time (inclusive)
     * @param endTime End time (inclusive)
     * @return List of bars ordered by timestamp ascending
     */
    List<MarketBar> findBarsInRange(
            String symbol, String timeframe, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Check if a bar exists.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @param barTimestamp Bar timestamp
     * @return true if bar exists, false otherwise
     */
    boolean existsBySymbolAndTimeframeAndTimestamp(
            String symbol, String timeframe, LocalDateTime barTimestamp);
}
