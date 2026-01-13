package maru.trading.broker.kis.ws;

import lombok.extern.slf4j.Slf4j;
import maru.trading.application.service.MarketDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * WebSocket reconnection service (Stage 4).
 *
 * Monitors WebSocket connection health and handles automatic reconnection.
 *
 * Responsibilities:
 * - Periodic connection health check (every 10 seconds)
 * - Automatic reconnection on disconnect
 * - Resubscription to market data after reconnection
 *
 * Only active in LIVE mode.
 */
@Slf4j
@Service
public class WebSocketReconnectionService {

    private final WebSocketConnectionManager connectionManager;
    private final MarketDataService marketDataService;

    @Value("${trading.market-data.mode:STUB}")
    private String marketDataMode;

    private boolean lastKnownConnectionState = true;

    public WebSocketReconnectionService(
            WebSocketConnectionManager connectionManager,
            MarketDataService marketDataService) {
        this.connectionManager = connectionManager;
        this.marketDataService = marketDataService;
    }

    /**
     * Check connection health every 10 seconds.
     *
     * Stage 4: Scheduled health check and automatic reconnection.
     */
    @Scheduled(fixedDelay = 10000) // 10 seconds
    public void checkConnectionHealth() {
        // Skip in STUB mode
        if (!"LIVE".equalsIgnoreCase(marketDataMode)) {
            return;
        }

        boolean isConnected = connectionManager.isConnected();

        // Log state changes
        if (isConnected != lastKnownConnectionState) {
            if (isConnected) {
                log.info("WebSocket connection state changed: DISCONNECTED -> CONNECTED");
                onConnectionRestored();
            } else {
                log.warn("WebSocket connection state changed: CONNECTED -> DISCONNECTED");
                onConnectionLost();
            }
            lastKnownConnectionState = isConnected;
        }

        // If disconnected, attempt reconnection
        if (!isConnected) {
            log.warn("WebSocket disconnected, attempting reconnection...");

            try {
                connectionManager.connect();

                if (connectionManager.isConnected()) {
                    log.info("WebSocket reconnection successful");
                    onConnectionRestored();
                } else {
                    log.warn("WebSocket reconnection failed, will retry in 10 seconds");
                }

            } catch (Exception e) {
                log.error("WebSocket reconnection attempt failed", e);
            }
        }
    }

    /**
     * Called when connection is restored.
     * Resubscribes to market data.
     */
    private void onConnectionRestored() {
        log.info("Connection restored, resubscribing to market data...");

        try {
            // Resubscribe to all symbols
            marketDataService.resubscribe();

            log.info("Market data resubscription completed successfully");

        } catch (Exception e) {
            log.error("Failed to resubscribe to market data after reconnection", e);
        }
    }

    /**
     * Called when connection is lost.
     * Could trigger alerts or cleanup actions.
     */
    private void onConnectionLost() {
        log.warn("Connection lost - market data streaming interrupted");

        // In production, could:
        // - Publish outbox event for alerting
        // - Trigger SNS/email notification
        // - Update system health metrics
    }

    /**
     * Force reconnection (for admin/testing purposes).
     */
    public void forceReconnect() {
        if (!"LIVE".equalsIgnoreCase(marketDataMode)) {
            log.info("Force reconnect skipped in STUB mode");
            return;
        }

        log.info("Force reconnection triggered");

        try {
            // Disconnect first
            connectionManager.disconnect();

            // Wait briefly
            Thread.sleep(1000);

            // Reconnect
            connectionManager.connect();

            // Resubscribe
            if (connectionManager.isConnected()) {
                marketDataService.resubscribe();
                log.info("Force reconnection completed successfully");
            } else {
                log.warn("Force reconnection failed");
            }

        } catch (Exception e) {
            log.error("Force reconnection failed", e);
        }
    }

    /**
     * Get current connection state.
     */
    public boolean isConnected() {
        return connectionManager.isConnected();
    }
}
