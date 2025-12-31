package maru.trading.application.ports.broker;

import maru.trading.domain.execution.Fill;
import maru.trading.domain.market.MarketTick;

import java.util.List;
import java.util.function.Consumer;

/**
 * Port interface for WebSocket streaming from broker.
 * Provides real-time market data and fill event subscriptions.
 *
 * Implementation: KisWebSocketClient (broker adapter layer)
 */
public interface BrokerStream {

    /**
     * Subscribe to real-time tick data for given symbols.
     *
     * @param symbols List of symbols to subscribe (e.g., "005930", "000660")
     * @param handler Callback to receive MarketTick events
     * @return Subscription ID for managing the subscription
     */
    String subscribeTicks(List<String> symbols, Consumer<MarketTick> handler);

    /**
     * Subscribe to fill events for an account.
     *
     * @param accountId Account ID to subscribe
     * @param handler Callback to receive Fill events
     * @return Subscription ID for managing the subscription
     */
    String subscribeFills(String accountId, Consumer<Fill> handler);

    /**
     * Unsubscribe from a subscription.
     *
     * @param subscriptionId Subscription ID returned from subscribeTicks or subscribeFills
     */
    void unsubscribe(String subscriptionId);

    /**
     * Check if WebSocket connection is active.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();
}
