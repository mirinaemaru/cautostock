package maru.trading.broker.kis.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors WebSocket connection health using heartbeat (ping/pong).
 *
 * Sends periodic ping messages and monitors pong responses.
 * Triggers reconnection if pong timeout occurs.
 */
public class HeartbeatMonitor {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatMonitor.class);

    private static final long PING_INTERVAL_MS = 30_000; // 30 seconds
    private static final long PONG_TIMEOUT_MS = 10_000; // 10 seconds
    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final Runnable sendPingAction;
    private final Runnable onTimeoutAction;
    private final ScheduledExecutorService scheduler;

    private final AtomicLong lastPongTimestamp = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    private volatile boolean monitoring = false;

    public HeartbeatMonitor(Runnable sendPingAction, Runnable onTimeoutAction) {
        this.sendPingAction = sendPingAction;
        this.onTimeoutAction = onTimeoutAction;
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "WebSocket-Heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Start heartbeat monitoring.
     */
    public void start() {
        if (monitoring) {
            log.warn("Heartbeat monitor already running");
            return;
        }

        monitoring = true;
        lastPongTimestamp.set(System.currentTimeMillis());
        consecutiveFailures.set(0);

        log.info("Starting heartbeat monitor (ping interval: {}ms, pong timeout: {}ms)",
                PING_INTERVAL_MS, PONG_TIMEOUT_MS);

        // Schedule periodic ping
        scheduler.scheduleAtFixedRate(
                this::sendPingAndCheckPong,
                PING_INTERVAL_MS,
                PING_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Stop heartbeat monitoring.
     */
    public void stop() {
        monitoring = false;
        log.info("Stopped heartbeat monitor");
    }

    /**
     * Record pong received.
     *
     * Resets failure counter.
     */
    public void onPongReceived() {
        lastPongTimestamp.set(System.currentTimeMillis());
        int failures = consecutiveFailures.getAndSet(0);

        if (failures > 0) {
            log.info("Pong received, reset consecutive failures from {} to 0", failures);
        } else {
            log.debug("Pong received");
        }
    }

    /**
     * Send ping and check if pong timeout occurred.
     */
    private void sendPingAndCheckPong() {
        if (!monitoring) {
            return;
        }

        try {
            // Check pong timeout
            long timeSinceLastPong = System.currentTimeMillis() - lastPongTimestamp.get();

            if (timeSinceLastPong > PONG_TIMEOUT_MS + PING_INTERVAL_MS) {
                // Pong timeout occurred
                int failures = consecutiveFailures.incrementAndGet();
                log.warn("Pong timeout detected ({}ms since last pong). Consecutive failures: {}/{}",
                        timeSinceLastPong, failures, MAX_CONSECUTIVE_FAILURES);

                if (failures >= MAX_CONSECUTIVE_FAILURES) {
                    log.error("Max consecutive pong timeouts ({}) reached. Triggering reconnection.",
                            MAX_CONSECUTIVE_FAILURES);
                    onTimeoutAction.run();
                    consecutiveFailures.set(0); // Reset after triggering
                }
            }

            // Send ping
            log.debug("Sending ping at {}", Instant.now());
            sendPingAction.run();

        } catch (Exception e) {
            log.error("Error in heartbeat monitor: {}", e.getMessage(), e);
        }
    }

    /**
     * Get time since last pong (milliseconds).
     */
    public long getTimeSinceLastPong() {
        return System.currentTimeMillis() - lastPongTimestamp.get();
    }

    /**
     * Get consecutive failure count.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * Check if monitoring is active.
     */
    public boolean isMonitoring() {
        return monitoring;
    }

    /**
     * Shutdown the heartbeat monitor.
     */
    public void shutdown() {
        log.info("Shutting down heartbeat monitor");
        monitoring = false;
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
