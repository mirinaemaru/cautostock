package maru.trading.broker.kis.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages WebSocket connection lifecycle.
 *
 * Responsibilities:
 * - Connection state tracking
 * - Reconnection with exponential backoff
 * - Health checks (ping/pong)
 *
 * In MVP (stub mode), always reports connected.
 * In production, manages real WebSocket connection.
 */
@Component
public class WebSocketConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionManager.class);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final int INITIAL_RECONNECT_DELAY_MS = 1000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;

    private final AtomicBoolean connected = new AtomicBoolean(true); // Stub: always connected
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    /**
     * Ensure WebSocket connection is established.
     * In stub mode, always returns true.
     */
    public boolean ensureConnected() {
        if (!connected.get()) {
            log.info("WebSocket not connected, attempting reconnection");
            return reconnect();
        }
        return true;
    }

    /**
     * Handle disconnect event.
     * Schedules reconnection with exponential backoff.
     */
    public void handleDisconnect() {
        log.warn("WebSocket disconnected");
        connected.set(false);
        scheduleReconnect();
    }

    /**
     * Schedule reconnection with exponential backoff.
     */
    public void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();

        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            log.error("Max reconnection attempts ({}) exceeded, giving up", MAX_RECONNECT_ATTEMPTS);
            return;
        }

        // Exponential backoff: 1s, 2s, 4s, 8s, ..., max 30s
        int delay = Math.min(
                INITIAL_RECONNECT_DELAY_MS * (1 << (attempts - 1)),
                MAX_RECONNECT_DELAY_MS
        );

        log.info("Scheduling reconnect attempt {} in {}ms", attempts, delay);

        // In production, use ScheduledExecutorService
        // For stub, just log
        reconnect();
    }

    /**
     * Attempt to reconnect.
     * In stub mode, always succeeds.
     */
    private boolean reconnect() {
        log.info("Reconnecting WebSocket (stub mode - always succeeds)");
        connected.set(true);
        reconnectAttempts.set(0);
        return true;
    }

    /**
     * Check if WebSocket is connected.
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Reset connection state (for testing).
     */
    public void reset() {
        connected.set(true);
        reconnectAttempts.set(0);
    }
}
