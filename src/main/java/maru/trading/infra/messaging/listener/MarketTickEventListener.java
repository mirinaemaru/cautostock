package maru.trading.infra.messaging.listener;

import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.MarketDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event listener for market tick events from WebSocket.
 *
 * Listens for MarketTick events and:
 * 1. Updates market data cache
 * 2. Notifies strategy engine (Phase 3 - not implemented in MVP)
 *
 * In MVP, simply updates cache.
 * In production, would also:
 * - Forward to strategy engine for signal generation
 * - Publish to event bus for other consumers
 * - Store in time-series database for historical analysis
 */
@Component
public class MarketTickEventListener {

    private static final Logger log = LoggerFactory.getLogger(MarketTickEventListener.class);

    private final MarketDataCache marketDataCache;

    public MarketTickEventListener(MarketDataCache marketDataCache) {
        this.marketDataCache = marketDataCache;
    }

    /**
     * Handle tick received from WebSocket.
     * Updates market data cache.
     *
     * @param tick Market tick event
     */
    public void onTickReceived(MarketTick tick) {
        log.debug("Tick event received: symbol={}, price={}, volume={}",
                tick.getSymbol(), tick.getPrice(), tick.getVolume());

        try {
            // Validate tick
            tick.validate();

            // Update cache
            marketDataCache.put(tick);

            // TODO (Phase 3): Notify strategy engine
            // strategyEngine.onMarketTick(tick);

        } catch (Exception e) {
            log.error("Error processing market tick: symbol={}", tick.getSymbol(), e);
        }
    }
}
