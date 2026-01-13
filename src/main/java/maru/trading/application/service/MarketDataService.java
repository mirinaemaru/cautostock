package maru.trading.application.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import maru.trading.application.orchestration.BarAggregator;
import maru.trading.application.ports.broker.BrokerStream;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.MarketDataCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Market Data Service (Stage 1).
 *
 * Responsibilities:
 * 1. Subscribe to market data for configured symbols
 * 2. Receive real-time ticks from BrokerStream
 * 3. Cache ticks in MarketDataCache
 * 4. Forward ticks to BarAggregator for bar generation
 *
 * This service acts as the central hub for all market data flow.
 */
@Slf4j
@Service
public class MarketDataService {

    private final BrokerStream brokerStream;
    private final MarketDataCache marketDataCache;
    private final BarAggregator barAggregator;

    @Value("${trading.market-data.symbols:005930,035420,000660}")
    private String symbolsConfig;

    private String activeSubscriptionId;
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();

    public MarketDataService(
            BrokerStream brokerStream,
            MarketDataCache marketDataCache,
            BarAggregator barAggregator) {
        this.brokerStream = brokerStream;
        this.marketDataCache = marketDataCache;
        this.barAggregator = barAggregator;
    }

    /**
     * Initialize and start market data subscription.
     * Called automatically by Spring after bean construction.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing MarketDataService");

        // Parse symbols from configuration
        List<String> symbols = parseSymbols(symbolsConfig);

        if (symbols.isEmpty()) {
            log.warn("No symbols configured for market data subscription");
            return;
        }

        // Subscribe to market data
        subscribeToMarketData(symbols);

        log.info("MarketDataService initialized: {} symbols subscribed", symbols.size());
    }

    /**
     * Subscribe to real-time market data for given symbols.
     *
     * @param symbols List of symbol codes (e.g., "005930", "035420")
     */
    public void subscribeToMarketData(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            log.warn("Cannot subscribe: symbol list is empty");
            return;
        }

        log.info("Subscribing to market data for {} symbols: {}", symbols.size(), symbols);

        try {
            // Subscribe through BrokerStream (KisWebSocketClient)
            activeSubscriptionId = brokerStream.subscribeTicks(symbols, this::onTickReceived);

            // Track subscribed symbols
            subscribedSymbols.addAll(symbols);

            log.info("Market data subscription created: subscriptionId={}, symbols={}",
                    activeSubscriptionId, symbols);

        } catch (Exception e) {
            log.error("Failed to subscribe to market data", e);
        }
    }

    /**
     * Add new symbols to subscription.
     *
     * @param newSymbols Symbols to add
     */
    public void addSymbols(List<String> newSymbols) {
        List<String> symbolsToAdd = newSymbols.stream()
                .filter(s -> !subscribedSymbols.contains(s))
                .toList();

        if (symbolsToAdd.isEmpty()) {
            log.debug("All symbols already subscribed");
            return;
        }

        log.info("Adding {} new symbols to subscription", symbolsToAdd.size());

        // Unsubscribe existing and resubscribe with all symbols
        if (activeSubscriptionId != null) {
            brokerStream.unsubscribe(activeSubscriptionId);
        }

        List<String> allSymbols = new ArrayList<>(subscribedSymbols);
        allSymbols.addAll(symbolsToAdd);

        subscribeToMarketData(allSymbols);
    }

    /**
     * Remove symbols from subscription.
     *
     * @param symbolsToRemove Symbols to remove
     */
    public void removeSymbols(List<String> symbolsToRemove) {
        subscribedSymbols.removeAll(symbolsToRemove);

        log.info("Removed {} symbols from subscription", symbolsToRemove.size());

        // Resubscribe with remaining symbols
        if (activeSubscriptionId != null) {
            brokerStream.unsubscribe(activeSubscriptionId);
        }

        if (!subscribedSymbols.isEmpty()) {
            subscribeToMarketData(new ArrayList<>(subscribedSymbols));
        } else {
            log.warn("No symbols remaining after removal");
        }
    }

    /**
     * Callback handler for received ticks.
     * Called by BrokerStream when a new tick arrives.
     *
     * @param tick Market tick data
     */
    private void onTickReceived(MarketTick tick) {
        if (tick == null) {
            log.warn("Received null tick");
            return;
        }

        try {
            // Validate tick data
            tick.validate();

            // 1. Cache tick for latest price queries
            marketDataCache.put(tick);

            // 2. Forward to bar aggregator for bar generation
            barAggregator.onTick(tick);

            log.trace("Processed tick: symbol={}, price={}, volume={}, timestamp={}",
                    tick.getSymbol(), tick.getPrice(), tick.getVolume(), tick.getTimestamp());

        } catch (Exception e) {
            log.error("Error processing tick: {}", tick, e);
        }
    }

    /**
     * Get list of currently subscribed symbols.
     *
     * @return Set of symbol codes
     */
    public Set<String> getSubscribedSymbols() {
        return Set.copyOf(subscribedSymbols);
    }

    /**
     * Get active subscription ID.
     *
     * @return Subscription ID or null if not subscribed
     */
    public String getActiveSubscriptionId() {
        return activeSubscriptionId;
    }

    /**
     * Check if service is actively subscribed to market data.
     *
     * @return true if subscribed, false otherwise
     */
    public boolean isSubscribed() {
        return activeSubscriptionId != null && brokerStream.isConnected();
    }

    /**
     * Resubscribe to market data.
     * Useful after WebSocket reconnection.
     */
    public void resubscribe() {
        if (subscribedSymbols.isEmpty()) {
            log.warn("No symbols to resubscribe");
            return;
        }

        log.info("Resubscribing to market data for {} symbols", subscribedSymbols.size());

        subscribeToMarketData(new ArrayList<>(subscribedSymbols));
    }

    /**
     * Parse symbol configuration string.
     *
     * @param config Comma-separated symbol list (e.g., "005930,035420,000660")
     * @return List of symbol codes
     */
    private List<String> parseSymbols(String config) {
        if (config == null || config.isBlank()) {
            return List.of();
        }

        return List.of(config.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Cleanup on service shutdown.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MarketDataService");

        if (activeSubscriptionId != null) {
            try {
                brokerStream.unsubscribe(activeSubscriptionId);
                log.info("Unsubscribed from market data: subscriptionId={}", activeSubscriptionId);
            } catch (Exception e) {
                log.error("Error during unsubscription", e);
            }
        }

        subscribedSymbols.clear();
        log.info("MarketDataService shutdown complete");
    }
}
