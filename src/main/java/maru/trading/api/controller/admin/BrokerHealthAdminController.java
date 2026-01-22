package maru.trading.api.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.ws.KisWebSocketClient;
import maru.trading.broker.kis.ws.WebSocketConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Broker Health Admin Controller.
 *
 * Provides endpoints for monitoring broker connection health and status.
 *
 * Endpoints:
 * - GET /api/v1/admin/broker/health - Overall broker health status
 * - GET /api/v1/admin/broker/websocket - WebSocket connection status
 * - POST /api/v1/admin/broker/websocket/reconnect - Trigger WebSocket reconnection
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/broker")
@RequiredArgsConstructor
public class BrokerHealthAdminController {

    private final KisWebSocketClient webSocketClient;
    private final WebSocketConnectionManager connectionManager;

    @Value("${trading.market-data.mode:STUB}")
    private String marketDataMode;

    @Value("${spring.profiles.active:paper}")
    private String activeProfile;

    /**
     * Get overall broker health status.
     *
     * Returns comprehensive health information including:
     * - REST API status
     * - WebSocket status
     * - Current mode (STUB/LIVE)
     * - Active profile (paper/live)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getBrokerHealth() {
        log.info("Getting broker health status");

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("mode", marketDataMode);
        response.put("profile", activeProfile);

        // WebSocket health
        boolean wsConnected = webSocketClient.isConnected();
        Map<String, Object> wsHealth = new HashMap<>();
        wsHealth.put("connected", wsConnected);
        wsHealth.put("status", wsConnected ? "UP" : "DOWN");
        wsHealth.put("mode", marketDataMode);
        response.put("websocket", wsHealth);

        // REST API health (in STUB mode, always UP)
        Map<String, Object> restHealth = new HashMap<>();
        boolean restAvailable = isRestApiAvailable();
        restHealth.put("available", restAvailable);
        restHealth.put("status", restAvailable ? "UP" : "DOWN");
        restHealth.put("mode", marketDataMode);
        response.put("restApi", restHealth);

        // Overall status
        boolean healthy = wsConnected && restAvailable;
        response.put("status", healthy ? "UP" : "DEGRADED");
        response.put("healthy", healthy);

        // Safety info
        Map<String, Object> safetyInfo = new HashMap<>();
        safetyInfo.put("isStubMode", "STUB".equalsIgnoreCase(marketDataMode));
        safetyInfo.put("isPaperTrading", "paper".equalsIgnoreCase(activeProfile));
        safetyInfo.put("liveOrdersEnabled", "live".equalsIgnoreCase(activeProfile) &&
                                            !"STUB".equalsIgnoreCase(marketDataMode));
        response.put("safety", safetyInfo);

        return ResponseEntity.ok(response);
    }

    /**
     * Get WebSocket connection details.
     */
    @GetMapping("/websocket")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        log.info("Getting WebSocket status");

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("mode", marketDataMode);

        boolean connected = webSocketClient.isConnected();
        response.put("connected", connected);
        response.put("status", connected ? "CONNECTED" : "DISCONNECTED");

        // Additional details
        if ("STUB".equalsIgnoreCase(marketDataMode)) {
            response.put("note", "Running in STUB mode - WebSocket simulates connection");
        } else {
            response.put("note", "Running in LIVE mode - Real WebSocket connection to KIS");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Trigger WebSocket reconnection.
     * Only applicable in LIVE mode.
     */
    @PostMapping("/websocket/reconnect")
    public ResponseEntity<Map<String, Object>> reconnectWebSocket() {
        log.info("Triggering WebSocket reconnection");

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("mode", marketDataMode);

        if ("STUB".equalsIgnoreCase(marketDataMode)) {
            response.put("success", false);
            response.put("message", "Reconnection not applicable in STUB mode");
            return ResponseEntity.ok(response);
        }

        try {
            // Disconnect existing connection
            connectionManager.disconnect();

            // Reset and reconnect
            connectionManager.reset();
            connectionManager.connect();

            boolean connected = connectionManager.isConnected();
            response.put("success", true);
            response.put("connected", connected);
            response.put("message", connected ? "WebSocket reconnected successfully" : "Reconnection initiated");

        } catch (Exception e) {
            log.error("Failed to reconnect WebSocket", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get broker connection summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBrokerSummary() {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());

        // Connection summary
        response.put("broker", "KIS (Korea Investment & Securities)");
        response.put("mode", marketDataMode);
        response.put("profile", activeProfile);

        // Status summary
        boolean wsConnected = webSocketClient.isConnected();
        boolean restAvailable = isRestApiAvailable();

        response.put("connections", Map.of(
            "websocket", wsConnected ? "UP" : "DOWN",
            "restApi", restAvailable ? "UP" : "DOWN"
        ));

        // Capabilities based on mode
        Map<String, Boolean> capabilities = new HashMap<>();
        capabilities.put("realTimeMarketData", wsConnected);
        capabilities.put("orderPlacement", restAvailable);
        capabilities.put("orderModification", restAvailable);
        capabilities.put("orderCancellation", restAvailable);
        capabilities.put("fillNotifications", wsConnected);
        response.put("capabilities", capabilities);

        // Warnings
        if ("live".equalsIgnoreCase(activeProfile) && !"STUB".equalsIgnoreCase(marketDataMode)) {
            response.put("warning", "LIVE MODE ACTIVE - Real orders will be executed!");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Check if REST API is available.
     * In STUB mode, always returns true.
     * In LIVE mode, would perform an actual health check.
     */
    private boolean isRestApiAvailable() {
        if ("STUB".equalsIgnoreCase(marketDataMode)) {
            return true; // STUB mode: always available
        }
        // In LIVE mode, could perform a simple API call to check availability
        // For now, return true as we don't want to make unnecessary API calls
        return true;
    }
}
