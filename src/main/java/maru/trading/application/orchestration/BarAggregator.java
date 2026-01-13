package maru.trading.application.orchestration;

import maru.trading.application.ports.repo.BarRepository;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.BarCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bar aggregation orchestrator.
 *
 * Responsibilities:
 * 1. Convert incoming ticks to 1-minute bars
 * 2. Maintain in-progress bars
 * 3. Close bars when time boundary is crossed
 * 4. Persist closed bars to database
 * 5. Cache closed bars for strategy access
 *
 * Thread-safe for concurrent tick processing.
 */
@Service
public class BarAggregator {

    private static final Logger log = LoggerFactory.getLogger(BarAggregator.class);
    private static final String DEFAULT_TIMEFRAME = "1m";

    private final BarRepository barRepository;
    private final BarCache barCache;

    // Key: "symbol:timeframe" (e.g., "005930:1m")
    // Value: In-progress bar
    private final Map<String, MarketBar> currentBars = new ConcurrentHashMap<>();

    public BarAggregator(BarRepository barRepository, BarCache barCache) {
        this.barRepository = barRepository;
        this.barCache = barCache;
    }

    /**
     * Process incoming tick and aggregate into bars.
     * Called by MarketDataCache or SubscribeMarketDataUseCase.
     *
     * @param tick Market tick
     */
    public void onTick(MarketTick tick) {
        if (tick == null) {
            log.warn("Received null tick, ignoring");
            return;
        }

        try {
            String key = createKey(tick.getSymbol(), DEFAULT_TIMEFRAME);

            // Get or create current bar for this tick
            MarketBar currentBar = currentBars.computeIfAbsent(key, k -> {
                LocalDateTime barTimestamp = getBarTimestamp(tick.getTimestamp(), DEFAULT_TIMEFRAME);
                MarketBar newBar = new MarketBar(tick.getSymbol(), DEFAULT_TIMEFRAME, barTimestamp);
                log.debug("Created new bar: symbol={}, timestamp={}", tick.getSymbol(), barTimestamp);
                return newBar;
            });

            // Check if tick belongs to current bar or a new bar period
            LocalDateTime tickBarTimestamp = getBarTimestamp(tick.getTimestamp(), DEFAULT_TIMEFRAME);

            if (shouldCloseCurrentBar(currentBar, tickBarTimestamp)) {
                // Close and persist current bar
                closeBar(currentBar);
                currentBars.remove(key);

                // Create new bar for this tick
                currentBar = new MarketBar(tick.getSymbol(), DEFAULT_TIMEFRAME, tickBarTimestamp);
                currentBars.put(key, currentBar);
                log.debug("Started new bar: symbol={}, timestamp={}", tick.getSymbol(), tickBarTimestamp);
            }

            // Add tick to current bar
            currentBar.addTick(tick);

            log.trace("Added tick to bar: symbol={}, price={}, volume={}, barTimestamp={}",
                    tick.getSymbol(), tick.getPrice(), tick.getVolume(), currentBar.getBarTimestamp());

        } catch (Exception e) {
            log.error("Error processing tick: {}", tick, e);
        }
    }

    /**
     * Force close all current bars.
     * Useful for end-of-day processing or shutdown.
     */
    public void closeAllBars() {
        log.info("Closing all current bars: count={}", currentBars.size());

        currentBars.values().forEach(this::closeBar);
        currentBars.clear();
    }

    /**
     * Close a bar and persist it.
     *
     * @param bar Bar to close
     */
    private void closeBar(MarketBar bar) {
        try {
            bar.close();
            bar.validate();

            // Persist to database
            barRepository.save(bar);

            // Cache for fast access
            barCache.put(bar);

            log.info("Closed and persisted bar: symbol={}, timeframe={}, timestamp={}, O={}, H={}, L={}, C={}, V={}",
                    bar.getSymbol(), bar.getTimeframe(), bar.getBarTimestamp(),
                    bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());

        } catch (Exception e) {
            log.error("Error closing bar: {}", bar, e);
        }
    }

    /**
     * Determine if current bar should be closed based on tick timestamp.
     *
     * @param currentBar Current in-progress bar
     * @param tickBarTimestamp Tick's bar timestamp (rounded to bar boundary)
     * @return true if current bar should be closed
     */
    private boolean shouldCloseCurrentBar(MarketBar currentBar, LocalDateTime tickBarTimestamp) {
        return !currentBar.getBarTimestamp().equals(tickBarTimestamp);
    }

    /**
     * Calculate bar timestamp for a tick.
     * Rounds down to the nearest bar boundary.
     *
     * For 1-minute bars:
     * - 09:00:00 -> 09:00:00
     * - 09:00:30 -> 09:00:00
     * - 09:00:59 -> 09:00:00
     * - 09:01:00 -> 09:01:00
     *
     * @param tickTimestamp Tick timestamp
     * @param timeframe Timeframe (e.g., "1m")
     * @return Bar timestamp (rounded down)
     */
    private LocalDateTime getBarTimestamp(LocalDateTime tickTimestamp, String timeframe) {
        if ("1m".equals(timeframe)) {
            return tickTimestamp.truncatedTo(ChronoUnit.MINUTES);
        } else if ("5m".equals(timeframe)) {
            int minute = tickTimestamp.getMinute();
            int roundedMinute = (minute / 5) * 5;
            return tickTimestamp.withMinute(roundedMinute)
                    .withSecond(0)
                    .withNano(0);
        } else if ("1h".equals(timeframe)) {
            return tickTimestamp.truncatedTo(ChronoUnit.HOURS);
        } else {
            // Default: 1-minute
            return tickTimestamp.truncatedTo(ChronoUnit.MINUTES);
        }
    }

    private String createKey(String symbol, String timeframe) {
        return symbol + ":" + timeframe;
    }

    /**
     * Get statistics about current in-progress bars.
     *
     * @return Map of symbol:timeframe -> bar info
     */
    public Map<String, String> getStats() {
        Map<String, String> stats = new ConcurrentHashMap<>();
        currentBars.forEach((key, bar) -> {
            String info = String.format("timestamp=%s, ticks=%s, price=%s",
                    bar.getBarTimestamp(),
                    bar.getVolume() > 0 ? "has_data" : "empty",
                    bar.getClose());
            stats.put(key, info);
        });
        return stats;
    }
}
