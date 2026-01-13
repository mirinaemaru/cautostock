package maru.trading.broker.kis.marketdata;

import maru.trading.application.orchestration.BarAggregator;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.MarketDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Market Data Collector.
 *
 * Collects real-time tick data from WebSocket and processes it.
 */
@Component
public class MarketDataCollector {

    private static final Logger log = LoggerFactory.getLogger(MarketDataCollector.class);

    private final MarketDataCache marketDataCache;
    private final BarAggregator barAggregator;
    private final TickDataValidator validator;
    private final DataQualityMonitor qualityMonitor;

    private final AtomicLong ticksReceived = new AtomicLong(0);
    private final AtomicLong ticksValid = new AtomicLong(0);
    private final AtomicLong ticksInvalid = new AtomicLong(0);

    public MarketDataCollector(
            MarketDataCache marketDataCache,
            BarAggregator barAggregator,
            TickDataValidator validator,
            DataQualityMonitor qualityMonitor) {
        this.marketDataCache = marketDataCache;
        this.barAggregator = barAggregator;
        this.validator = validator;
        this.qualityMonitor = qualityMonitor;
    }

    /**
     * Process incoming tick data from WebSocket.
     *
     * @param tick Market tick data
     */
    public void onTick(MarketTick tick) {
        ticksReceived.incrementAndGet();

        try {
            // 1. Validate tick data
            TickDataValidator.ValidationResult validation = validator.validate(tick);

            if (!validation.isValid()) {
                ticksInvalid.incrementAndGet();
                log.warn("Invalid tick data for {}: {}", tick.getSymbol(), validation.getErrorMessage());
                qualityMonitor.recordInvalidTick(tick.getSymbol(), validation.getErrorMessage());
                return;
            }

            ticksValid.incrementAndGet();

            // 2. Update cache
            marketDataCache.put(tick);

            // 3. Trigger bar aggregator
            barAggregator.onTick(tick);

            // 4. Record quality metrics
            qualityMonitor.recordValidTick(tick.getSymbol());

            if (ticksReceived.get() % 1000 == 0) {
                log.info("Market data stats - Received: {}, Valid: {}, Invalid: {}",
                        ticksReceived.get(), ticksValid.get(), ticksInvalid.get());
            }

        } catch (Exception e) {
            log.error("Error processing tick for {}: {}", tick.getSymbol(), e.getMessage(), e);
            qualityMonitor.recordError(tick.getSymbol(), e.getMessage());
        }
    }

    /**
     * Get total ticks received.
     */
    public long getTicksReceived() {
        return ticksReceived.get();
    }

    /**
     * Get valid ticks count.
     */
    public long getTicksValid() {
        return ticksValid.get();
    }

    /**
     * Get invalid ticks count.
     */
    public long getTicksInvalid() {
        return ticksInvalid.get();
    }

    /**
     * Reset statistics.
     */
    public void resetStats() {
        ticksReceived.set(0);
        ticksValid.set(0);
        ticksInvalid.set(0);
        log.info("Market data statistics reset");
    }
}
