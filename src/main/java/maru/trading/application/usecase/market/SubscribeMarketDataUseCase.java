package maru.trading.application.usecase.market;

import maru.trading.application.ports.broker.BrokerStream;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.MarketDataCache;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Use case for subscribing to real-time market data.
 *
 * This use case:
 * 1. Validates symbols
 * 2. Subscribes to tick data via BrokerStream
 * 3. Registers handler to process incoming ticks
 * 4. Returns subscription ID
 */
@Service
public class SubscribeMarketDataUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubscribeMarketDataUseCase.class);

    private final BrokerStream brokerStream;
    private final MarketDataCache marketDataCache;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;

    public SubscribeMarketDataUseCase(
            BrokerStream brokerStream,
            MarketDataCache marketDataCache,
            OutboxService outboxService,
            UlidGenerator ulidGenerator) {
        this.brokerStream = brokerStream;
        this.marketDataCache = marketDataCache;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Execute the use case to subscribe to market data.
     *
     * @param symbols List of symbols to subscribe (e.g., ["005930", "000660"])
     * @return Subscription ID for managing the subscription
     */
    public String execute(List<String> symbols) {
        // Step 1: Validate symbols (basic validation)
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("Symbols list cannot be null or empty");
        }

        for (String symbol : symbols) {
            if (symbol == null || symbol.isBlank()) {
                throw new IllegalArgumentException("Symbol cannot be null or blank");
            }
        }

        log.info("Subscribing to market data for {} symbols: {}", symbols.size(), symbols);

        // Step 2: Subscribe to tick data with handler
        String subscriptionId = brokerStream.subscribeTicks(symbols, this::handleTick);

        log.info("Market data subscription created: subscriptionId={}, symbols={}",
                subscriptionId, symbols);

        return subscriptionId;
    }

    /**
     * Handler for incoming market ticks.
     * Called by BrokerStream when a tick is received.
     */
    private void handleTick(MarketTick tick) {
        try {
            log.debug("Received tick: symbol={}, price={}, volume={}, timestamp={}",
                    tick.getSymbol(), tick.getPrice(), tick.getVolume(), tick.getTimestamp());

            // Validate tick
            tick.validate();

            // Store in market data cache (in-memory)
            marketDataCache.put(tick);

            // Publish MarketTickReceived event (optional - for strategy engine)
            publishMarketTickEvent(tick);

        } catch (Exception e) {
            log.error("Error processing market tick: {}", tick, e);
        }
    }

    private void publishMarketTickEvent(MarketTick tick) {
        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("MarketTickReceived")
                .occurredAt(java.time.LocalDateTime.now())
                .payload(Map.of(
                        "symbol", tick.getSymbol(),
                        "price", tick.getPrice(),
                        "volume", tick.getVolume(),
                        "timestamp", tick.getTimestamp(),
                        "tradingStatus", tick.getTradingStatus()
                ))
                .build();
        outboxService.save(event);
    }
}
