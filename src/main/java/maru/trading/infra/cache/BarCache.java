package maru.trading.infra.cache;

import maru.trading.domain.market.MarketBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory cache for market bars.
 *
 * Stores recent bars for fast access by strategy engine.
 * Thread-safe for concurrent access.
 */
@Component
public class BarCache {

    private static final Logger log = LoggerFactory.getLogger(BarCache.class);

    // Key: "symbol:timeframe" (e.g., "005930:1m")
    // Value: List of bars (ordered oldest to newest)
    private final Map<String, List<MarketBar>> cache = new ConcurrentHashMap<>();

    // Default maximum bars to keep per symbol-timeframe
    private static final int DEFAULT_MAX_BARS = 200;

    /**
     * Put a bar into the cache.
     *
     * @param bar Market bar to cache
     */
    public void put(MarketBar bar) {
        if (bar == null) {
            throw new IllegalArgumentException("Bar cannot be null");
        }

        String key = createKey(bar.getSymbol(), bar.getTimeframe());

        cache.compute(key, (k, bars) -> {
            if (bars == null) {
                bars = new ArrayList<>();
            }

            // Add bar to the end (newest)
            bars.add(bar);

            // Trim if exceeds max size
            if (bars.size() > DEFAULT_MAX_BARS) {
                bars = bars.subList(bars.size() - DEFAULT_MAX_BARS, bars.size());
            }

            log.debug("Cached bar: symbol={}, timeframe={}, total_bars={}",
                    bar.getSymbol(), bar.getTimeframe(), bars.size());

            return bars;
        });
    }

    /**
     * Get recent bars for a symbol and timeframe.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe (e.g., "1m")
     * @param count Number of bars to retrieve (most recent)
     * @return List of bars (oldest first), or empty list if not found
     */
    public List<MarketBar> getRecentBars(String symbol, String timeframe, int count) {
        String key = createKey(symbol, timeframe);
        List<MarketBar> bars = cache.get(key);

        if (bars == null || bars.isEmpty()) {
            return new ArrayList<>();
        }

        if (count <= 0 || count >= bars.size()) {
            return new ArrayList<>(bars);
        }

        // Return last N bars (most recent)
        return new ArrayList<>(bars.subList(bars.size() - count, bars.size()));
    }

    /**
     * Get all bars for a symbol and timeframe.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @return List of all cached bars (oldest first)
     */
    public List<MarketBar> getAllBars(String symbol, String timeframe) {
        String key = createKey(symbol, timeframe);
        List<MarketBar> bars = cache.get(key);
        return bars != null ? new ArrayList<>(bars) : new ArrayList<>();
    }

    /**
     * Get the most recent bar for a symbol and timeframe.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @return Most recent bar, or null if not found
     */
    public MarketBar getLatestBar(String symbol, String timeframe) {
        String key = createKey(symbol, timeframe);
        List<MarketBar> bars = cache.get(key);

        if (bars == null || bars.isEmpty()) {
            return null;
        }

        return bars.get(bars.size() - 1);
    }

    /**
     * Clear all bars for a symbol and timeframe.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     */
    public void clear(String symbol, String timeframe) {
        String key = createKey(symbol, timeframe);
        cache.remove(key);
        log.info("Cleared bar cache for: symbol={}, timeframe={}", symbol, timeframe);
    }

    /**
     * Clear all cached bars.
     */
    public void clearAll() {
        cache.clear();
        log.info("Cleared all bar cache");
    }

    /**
     * Get cache statistics.
     *
     * @return Map of symbol:timeframe -> bar count
     */
    public Map<String, Integer> getStats() {
        return cache.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size()
                ));
    }

    private String createKey(String symbol, String timeframe) {
        return symbol + ":" + timeframe;
    }
}
