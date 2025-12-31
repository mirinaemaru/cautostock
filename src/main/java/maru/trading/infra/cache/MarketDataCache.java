package maru.trading.infra.cache;

import maru.trading.domain.market.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for latest market tick data.
 *
 * Thread-safe cache using ConcurrentHashMap.
 * Stores the most recent tick for each symbol.
 *
 * In MVP, no persistence or expiry.
 * In production, could add:
 * - TTL-based expiry (evict ticks older than 1 hour)
 * - Redis/Memcached for distributed cache
 * - Historical tick storage
 */
@Component
public class MarketDataCache {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCache.class);
    private static final Duration STALENESS_THRESHOLD = Duration.ofHours(1);

    private final Map<String, MarketTick> tickCache = new ConcurrentHashMap<>();

    /**
     * Store a market tick in cache.
     * Overwrites existing tick for the symbol.
     */
    public void put(MarketTick tick) {
        if (tick == null) {
            log.warn("Attempted to cache null tick");
            return;
        }

        tickCache.put(tick.getSymbol(), tick);
        log.debug("Cached tick: symbol={}, price={}", tick.getSymbol(), tick.getPrice());
    }

    /**
     * Get the latest tick for a symbol.
     * Returns null if not found.
     */
    public MarketTick get(String symbol) {
        return tickCache.get(symbol);
    }

    /**
     * Get the latest price for a symbol.
     * Returns null if no tick found or tick is stale.
     */
    public BigDecimal getPrice(String symbol) {
        MarketTick tick = tickCache.get(symbol);

        if (tick == null) {
            log.debug("No tick found for symbol: {}", symbol);
            return null;
        }

        // Check if tick is stale
        if (tick.isDelayed(STALENESS_THRESHOLD)) {
            log.warn("Stale tick for symbol {}: age={}", symbol,
                    Duration.between(tick.getTimestamp(), java.time.LocalDateTime.now()));
            return null;
        }

        return tick.getPrice();
    }

    /**
     * Check if cache contains a tick for the symbol.
     */
    public boolean contains(String symbol) {
        return tickCache.containsKey(symbol);
    }

    /**
     * Remove tick from cache.
     */
    public void remove(String symbol) {
        tickCache.remove(symbol);
        log.debug("Removed tick from cache: symbol={}", symbol);
    }

    /**
     * Clear all ticks from cache.
     */
    public void clear() {
        tickCache.clear();
        log.info("Cleared market data cache");
    }

    /**
     * Get cache size (number of symbols).
     */
    public int size() {
        return tickCache.size();
    }
}
