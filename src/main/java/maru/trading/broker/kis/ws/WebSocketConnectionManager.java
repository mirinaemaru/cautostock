package maru.trading.broker.kis.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.auth.KisAuthenticationClient;
import maru.trading.broker.kis.config.KisProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Manages WebSocket connection lifecycle for KIS API.
 *
 * Stage 3: LIVE mode implementation with actual WebSocket connection.
 *
 * Responsibilities:
 * - WebSocket connection establishment
 * - Authentication message sending
 * - Subscription message sending
 * - Connection state tracking
 * - Reconnection with exponential backoff
 * - Message routing to handler
 */
@Slf4j
@Component
public class WebSocketConnectionManager implements WebSocket.Listener {

    private final KisProperties kisProperties;
    private final KisWebSocketMessageHandler messageHandler;
    private final KisAuthenticationClient authenticationClient;
    private final ObjectMapper objectMapper;

    @Value("${trading.market-data.mode:STUB}")
    private String marketDataMode;

    @Value("${spring.profiles.active:paper}")
    private String activeProfile;

    private WebSocket webSocket;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private Consumer<String> onMessageCallback;
    private String approvalKey;

    public WebSocketConnectionManager(
            KisProperties kisProperties,
            KisWebSocketMessageHandler messageHandler,
            KisAuthenticationClient authenticationClient,
            ObjectMapper objectMapper) {
        this.kisProperties = kisProperties;
        this.messageHandler = messageHandler;
        this.authenticationClient = authenticationClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize WebSocket connection on startup (LIVE mode only).
     */
    @PostConstruct
    public void init() {
        if ("LIVE".equalsIgnoreCase(marketDataMode)) {
            log.info("Initializing WebSocket connection manager in LIVE mode");
            connect();
        } else {
            log.info("WebSocket connection manager in STUB mode (no actual connection)");
            connected.set(true); // STUB mode: always connected
        }
    }

    /**
     * Establish WebSocket connection to KIS API.
     *
     * Stage 3: Actual WebSocket connection implementation.
     */
    public void connect() {
        if (!"LIVE".equalsIgnoreCase(marketDataMode)) {
            log.debug("Skipping WebSocket connection in STUB mode");
            return;
        }

        try {
            // Step 1: Issue approval key via REST API
            if (approvalKey == null) {
                issueApprovalKey();
            }

            if (approvalKey == null) {
                log.error("Cannot connect WebSocket: approval key not available");
                scheduleReconnect();
                return;
            }

            // Step 2: Connect WebSocket
            String wsUrl = getWebSocketUrl();
            log.info("Connecting to KIS WebSocket: {}", wsUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), this);

            this.webSocket = wsFuture.join();

            log.info("WebSocket connection established: {}", wsUrl);

            // Step 3: Send authentication message with approval key
            sendAuthMessage();

            connected.set(true);
            reconnectAttempts.set(0);

        } catch (Exception e) {
            log.error("Failed to connect to KIS WebSocket", e);
            connected.set(false);
            scheduleReconnect();
        }
    }

    /**
     * Issue WebSocket approval key via REST API.
     */
    private void issueApprovalKey() {
        try {
            boolean isLive = "live".equalsIgnoreCase(activeProfile);
            KisProperties.EnvironmentConfig env = getEnvironmentConfig();

            log.info("Issuing WebSocket approval key (isLive={})", isLive);

            this.approvalKey = authenticationClient.issueApprovalKey(
                    env.getAppKey(),
                    env.getAppSecret(),
                    isLive
            );

            log.info("Approval key issued successfully: {}...",
                    approvalKey.substring(0, Math.min(20, approvalKey.length())));

        } catch (Exception e) {
            log.error("Failed to issue approval key", e);
            this.approvalKey = null;
        }
    }

    /**
     * Send KIS authentication message.
     *
     * Note: KIS WebSocket doesn't use a separate auth message.
     * The approval_key is included in each subscription request header.
     */
    private void sendAuthMessage() {
        log.info("Authentication message sent to KIS WebSocket with approval key");
        // KIS WebSocket doesn't require a separate auth message
        // The approval_key is included in each subscription request header
    }

    /**
     * Subscribe to real-time tick data for given symbols.
     *
     * Stage 3: Subscription message implementation.
     * KIS WebSocket subscription format requires approval_key in header and tr_id/tr_key in body.input
     *
     * @param symbols List of symbol codes (e.g., "005930")
     */
    public void subscribe(List<String> symbols) {
        if (!"LIVE".equalsIgnoreCase(marketDataMode)) {
            log.debug("Skipping subscription in STUB mode");
            return;
        }

        if (approvalKey == null) {
            log.error("Cannot subscribe: approval key is null");
            return;
        }

        for (String symbol : symbols) {
            try {
                // KIS real-time tick subscription message (correct format)
                // See: https://apiportal.koreainvestment.com
                Map<String, Object> subMsg = Map.of(
                        "header", Map.of(
                                "approval_key", approvalKey,
                                "custtype", "P",         // P: Personal, B: Business
                                "tr_type", "1",          // 1: Register, 2: Unregister
                                "content-type", "utf-8"
                        ),
                        "body", Map.of(
                                "input", Map.of(
                                        "tr_id", "H0STCNT0",  // Real-time tick TR_ID
                                        "tr_key", symbol
                                )
                        )
                );

                String subJson = objectMapper.writeValueAsString(subMsg);
                send(subJson);

                log.info("Subscribed to symbol: {}", symbol);

            } catch (Exception e) {
                log.error("Failed to subscribe to symbol: {}", symbol, e);
            }
        }
    }

    /**
     * Unsubscribe from given symbols.
     */
    public void unsubscribe(List<String> symbols) {
        if (!"LIVE".equalsIgnoreCase(marketDataMode)) {
            return;
        }

        if (approvalKey == null) {
            log.error("Cannot unsubscribe: approval key is null");
            return;
        }

        for (String symbol : symbols) {
            try {
                // KIS WebSocket unsubscribe message (tr_type: 2)
                Map<String, Object> unsubMsg = Map.of(
                        "header", Map.of(
                                "approval_key", approvalKey,
                                "custtype", "P",
                                "tr_type", "2",  // 2: Unregister
                                "content-type", "utf-8"
                        ),
                        "body", Map.of(
                                "input", Map.of(
                                        "tr_id", "H0STCNT0",
                                        "tr_key", symbol
                                )
                        )
                );

                String unsubJson = objectMapper.writeValueAsString(unsubMsg);
                send(unsubJson);

                log.info("Unsubscribed from symbol: {}", symbol);

            } catch (Exception e) {
                log.error("Failed to unsubscribe from symbol: {}", symbol, e);
            }
        }
    }

    /**
     * Send message through WebSocket.
     */
    public void send(String message) {
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendText(message, true);
            log.trace("Sent WebSocket message: {}", message);
        } else {
            log.warn("Cannot send message: WebSocket is closed");
        }
    }

    /**
     * WebSocket.Listener callback: onText
     */
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String message = data.toString();
        log.trace("Received WebSocket message: {}", message);

        try {
            // Route to message handler
            messageHandler.handleMessage(message);

            // Notify callback if registered
            if (onMessageCallback != null) {
                onMessageCallback.accept(message);
            }

        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
        }

        webSocket.request(1); // Request next message
        return null;
    }

    /**
     * WebSocket.Listener callback: onOpen
     */
    @Override
    public void onOpen(WebSocket webSocket) {
        log.info("WebSocket connection opened");
        webSocket.request(1); // Request first message
    }

    /**
     * WebSocket.Listener callback: onError
     */
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("WebSocket error occurred", error);
        handleDisconnect();
    }

    /**
     * WebSocket.Listener callback: onClose
     */
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.warn("WebSocket closed: statusCode={}, reason={}", statusCode, reason);
        handleDisconnect();
        return null;
    }

    /**
     * Handle disconnect event.
     * Schedules reconnection with exponential backoff.
     */
    public void handleDisconnect() {
        log.warn("WebSocket disconnected");
        connected.set(false);

        if ("LIVE".equalsIgnoreCase(marketDataMode)) {
            scheduleReconnect();
        }
    }

    /**
     * Schedule reconnection with exponential backoff.
     */
    public void scheduleReconnect() {
        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = kisProperties.getWs().getMaxReconnectAttempts();

        if (attempts > maxAttempts) {
            log.error("Max reconnection attempts ({}) exceeded, giving up", maxAttempts);
            return;
        }

        int baseDelay = kisProperties.getWs().getReconnectDelayMs();
        // Exponential backoff: delay * 2^(attempts-1)
        int delay = Math.min(
                baseDelay * (1 << (attempts - 1)),
                30000 // Max 30 seconds
        );

        log.info("Scheduling reconnect attempt {} in {}ms", attempts, delay);

        // Use separate thread for reconnection
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Reconnection interrupted");
            }
        }).start();
    }

    /**
     * Check if WebSocket is connected.
     */
    public boolean isConnected() {
        if ("STUB".equalsIgnoreCase(marketDataMode)) {
            return true; // STUB mode: always connected
        }
        return connected.get() && webSocket != null && !webSocket.isOutputClosed();
    }

    /**
     * Disconnect WebSocket.
     */
    @PreDestroy
    public void disconnect() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Shutting down");
                log.info("WebSocket disconnected gracefully");
            } catch (Exception e) {
                log.warn("Error during WebSocket disconnect", e);
            }
        }
        connected.set(false);
    }

    /**
     * Get WebSocket URL based on active profile.
     */
    private String getWebSocketUrl() {
        if ("live".equalsIgnoreCase(activeProfile)) {
            return kisProperties.getLive().getWsUrl();
        } else {
            return kisProperties.getPaper().getWsUrl();
        }
    }

    /**
     * Get environment config based on active profile.
     */
    private KisProperties.EnvironmentConfig getEnvironmentConfig() {
        if ("live".equalsIgnoreCase(activeProfile)) {
            return kisProperties.getLive();
        } else {
            return kisProperties.getPaper();
        }
    }

    /**
     * Register message callback (for testing/monitoring).
     */
    public void setOnMessageCallback(Consumer<String> callback) {
        this.onMessageCallback = callback;
    }

    /**
     * Reset connection state (for testing).
     */
    public void reset() {
        connected.set("STUB".equalsIgnoreCase(marketDataMode));
        reconnectAttempts.set(0);
    }
}
