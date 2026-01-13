package maru.trading.broker.kis.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages WebSocket reconnection logic with exponential backoff.
 */
public class WebSocketReconnectionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketReconnectionManager.class);

    private final ReconnectionPolicy policy;
    private final Runnable reconnectAction;
    private final ScheduledExecutorService scheduler;

    private final AtomicInteger attemptCount = new AtomicInteger(0);
    private volatile boolean reconnecting = false;

    public WebSocketReconnectionManager(ReconnectionPolicy policy, Runnable reconnectAction) {
        this.policy = policy;
        this.reconnectAction = reconnectAction;
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "WebSocket-Reconnection");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Start reconnection process.
     *
     * Uses exponential backoff strategy.
     */
    public void startReconnection() {
        if (reconnecting) {
            log.warn("Reconnection already in progress");
            return;
        }

        reconnecting = true;
        attemptCount.set(0);
        log.info("Starting WebSocket reconnection process");

        scheduleNextAttempt();
    }

    /**
     * Stop reconnection process.
     */
    public void stopReconnection() {
        reconnecting = false;
        attemptCount.set(0);
        log.info("Stopped WebSocket reconnection process");
    }

    /**
     * Reset reconnection state after successful connection.
     */
    public void reset() {
        reconnecting = false;
        attemptCount.set(0);
        log.info("Reset reconnection state");
    }

    /**
     * Check if currently attempting to reconnect.
     */
    public boolean isReconnecting() {
        return reconnecting;
    }

    /**
     * Get current attempt count.
     */
    public int getAttemptCount() {
        return attemptCount.get();
    }

    /**
     * Schedule next reconnection attempt.
     */
    private void scheduleNextAttempt() {
        if (!reconnecting) {
            return;
        }

        int currentAttempt = attemptCount.get();

        if (!policy.shouldRetry(currentAttempt)) {
            log.error("Max reconnection attempts ({}) reached. Giving up.", policy.getMaxRetries());
            reconnecting = false;
            return;
        }

        long delay = policy.calculateDelay(currentAttempt);
        log.info("Scheduling reconnection attempt {} after {}ms", currentAttempt + 1, delay);

        scheduler.schedule(() -> {
            try {
                log.info("Reconnection attempt {}/{}", currentAttempt + 1, policy.getMaxRetries());
                reconnectAction.run();
                attemptCount.incrementAndGet();

                // Schedule next attempt (connection success will call reset())
                if (reconnecting) {
                    scheduleNextAttempt();
                }
            } catch (Exception e) {
                log.error("Reconnection attempt {} failed: {}", currentAttempt + 1, e.getMessage());
                attemptCount.incrementAndGet();

                if (reconnecting) {
                    scheduleNextAttempt();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Shutdown the reconnection manager.
     */
    public void shutdown() {
        log.info("Shutting down WebSocket reconnection manager");
        reconnecting = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
