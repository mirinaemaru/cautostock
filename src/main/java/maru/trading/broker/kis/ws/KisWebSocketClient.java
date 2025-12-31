package maru.trading.broker.kis.ws;

import maru.trading.application.ports.broker.BrokerStream;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.market.MarketTick;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * STUB implementation of KIS WebSocket client.
 *
 * Implements BrokerStream port for real-time data streaming.
 *
 * In MVP (stub mode), simulates:
 * - Tick events every 5 seconds for subscribed symbols
 * - Fill events (on demand via triggerFillForOrder)
 *
 * In production, this would:
 * - Establish WebSocket connection to KIS
 * - Subscribe to tick/fill channels
 * - Parse binary/JSON messages
 * - Handle reconnection
 */
@Component
public class KisWebSocketClient implements BrokerStream {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketClient.class);

    private final WebSocketConnectionManager connectionManager;
    private final KisWebSocketMessageHandler messageHandler;
    private final UlidGenerator ulidGenerator;

    // Subscription registry
    private final Map<String, TickSubscription> tickSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, FillSubscription> fillSubscriptions = new ConcurrentHashMap<>();

    // Track last simulated prices for tick generation
    private final Map<String, BigDecimal> lastPrices = new ConcurrentHashMap<>();

    public KisWebSocketClient(
            WebSocketConnectionManager connectionManager,
            KisWebSocketMessageHandler messageHandler,
            UlidGenerator ulidGenerator) {
        this.connectionManager = connectionManager;
        this.messageHandler = messageHandler;
        this.ulidGenerator = ulidGenerator;
    }

    @Override
    public String subscribeTicks(List<String> symbols, Consumer<MarketTick> handler) {
        String subscriptionId = ulidGenerator.generateInstance();

        log.info("[STUB] Subscribing to ticks for {} symbols: {}", symbols.size(), symbols);

        // Initialize last prices for simulation
        for (String symbol : symbols) {
            lastPrices.putIfAbsent(symbol, getInitialPrice(symbol));
        }

        TickSubscription subscription = new TickSubscription(subscriptionId, symbols, handler);
        tickSubscriptions.put(subscriptionId, subscription);

        log.info("[STUB] Tick subscription created: subscriptionId={}", subscriptionId);

        return subscriptionId;
    }

    @Override
    public String subscribeFills(String accountId, Consumer<Fill> handler) {
        String subscriptionId = ulidGenerator.generateInstance();

        log.info("[STUB] Subscribing to fills for account: {}", accountId);

        FillSubscription subscription = new FillSubscription(subscriptionId, accountId, handler);
        fillSubscriptions.put(subscriptionId, subscription);

        log.info("[STUB] Fill subscription created: subscriptionId={}", subscriptionId);

        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        log.info("[STUB] Unsubscribing: {}", subscriptionId);
        tickSubscriptions.remove(subscriptionId);
        fillSubscriptions.remove(subscriptionId);
    }

    @Override
    public boolean isConnected() {
        return connectionManager.isConnected();
    }

    /**
     * Simulate tick events every 5 seconds.
     * Generates random price movements for all subscribed symbols.
     */
    @Scheduled(fixedDelay = 5000)
    public void simulateTickEvents() {
        if (tickSubscriptions.isEmpty()) {
            return; // No subscriptions, skip simulation
        }

        log.debug("[STUB] Simulating tick events for {} subscriptions", tickSubscriptions.size());

        for (TickSubscription subscription : tickSubscriptions.values()) {
            for (String symbol : subscription.symbols) {
                // Generate simulated tick
                MarketTick tick = generateSimulatedTick(symbol);

                // Deliver to handler
                try {
                    subscription.handler.accept(tick);
                } catch (Exception e) {
                    log.error("Error delivering tick to handler: symbol={}", symbol, e);
                }
            }
        }
    }

    /**
     * Trigger a simulated fill event for an order.
     * Called manually for demo/testing purposes.
     */
    public void triggerFillForOrder(String orderId, String accountId, String symbol, Side side, int qty, BigDecimal price) {
        log.info("[STUB] Triggering fill: orderId={}, symbol={}, side={}, qty={}, price={}",
                orderId, symbol, side, qty, price);

        // Calculate fees and taxes (simple stub calculation)
        BigDecimal transactionValue = price.multiply(BigDecimal.valueOf(qty));
        BigDecimal fee = transactionValue.multiply(new BigDecimal("0.00015")); // 0.015%
        BigDecimal tax = side == Side.SELL ? transactionValue.multiply(new BigDecimal("0.0023")) : BigDecimal.ZERO; // 0.23% for sells

        Fill fill = new Fill(
                ulidGenerator.generateInstance(),
                orderId,
                accountId,
                symbol,
                side,
                price,
                qty,
                fee,
                tax,
                LocalDateTime.now(),
                "STUB_BROKER_ORDER_" + ulidGenerator.generateInstance().substring(0, 10)
        );

        // Deliver to all fill subscriptions for this account
        for (FillSubscription subscription : fillSubscriptions.values()) {
            if (subscription.accountId.equals(accountId)) {
                try {
                    subscription.handler.accept(fill);
                } catch (Exception e) {
                    log.error("Error delivering fill to handler: fillId={}", fill.getFillId(), e);
                }
            }
        }
    }

    /**
     * Generate simulated tick with random price movement.
     */
    private MarketTick generateSimulatedTick(String symbol) {
        BigDecimal lastPrice = lastPrices.get(symbol);

        // Random price change: -1% to +1%
        double changePercent = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.02;
        BigDecimal priceChange = lastPrice.multiply(BigDecimal.valueOf(changePercent));
        BigDecimal newPrice = lastPrice.add(priceChange).setScale(0, BigDecimal.ROUND_HALF_UP);

        // Update last price
        lastPrices.put(symbol, newPrice);

        // Random volume: 1 to 100
        long volume = ThreadLocalRandom.current().nextLong(1, 101);

        return new MarketTick(
                symbol,
                newPrice,
                volume,
                LocalDateTime.now(),
                "NORMAL"
        );
    }

    /**
     * Get initial price for a symbol (for simulation).
     */
    private BigDecimal getInitialPrice(String symbol) {
        // Use symbol hash to generate consistent initial prices
        // Common Korean stocks range from 1,000 to 100,000 KRW
        int symbolHash = Math.abs(symbol.hashCode());
        int basePrice = 10000 + (symbolHash % 90000);
        return BigDecimal.valueOf(basePrice);
    }

    /**
     * Tick subscription record.
     */
    private static class TickSubscription {
        final String subscriptionId;
        final List<String> symbols;
        final Consumer<MarketTick> handler;

        TickSubscription(String subscriptionId, List<String> symbols, Consumer<MarketTick> handler) {
            this.subscriptionId = subscriptionId;
            this.symbols = symbols;
            this.handler = handler;
        }
    }

    /**
     * Fill subscription record.
     */
    private static class FillSubscription {
        final String subscriptionId;
        final String accountId;
        final Consumer<Fill> handler;

        FillSubscription(String subscriptionId, String accountId, Consumer<Fill> handler) {
            this.subscriptionId = subscriptionId;
            this.accountId = accountId;
            this.handler = handler;
        }
    }
}
