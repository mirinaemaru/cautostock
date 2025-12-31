package maru.trading.broker.kis.config;

import maru.trading.broker.kis.ws.KisWebSocketClient;
import maru.trading.broker.kis.ws.WebSocketConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for KIS WebSocket client.
 *
 * In MVP (stub mode), this simply enables scheduling for tick simulation.
 * In production, this would configure:
 * - WebSocket client (Spring WebSocket or Java-WebSocket)
 * - Connection parameters
 * - SSL/TLS settings
 * - Message codecs
 */
@Configuration
@EnableScheduling
public class KisWebSocketConfig {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketConfig.class);

    public KisWebSocketConfig() {
        log.info("KIS WebSocket configuration initialized (STUB mode)");
    }

    /**
     * Configure WebSocket connection manager.
     * In stub mode, no actual configuration needed.
     */
    @Bean
    public WebSocketConnectionManager webSocketConnectionManager() {
        log.info("Creating WebSocketConnectionManager (STUB mode)");
        return new WebSocketConnectionManager();
    }

    // In production, additional beans would include:
    // - @Bean WebSocketClient
    // - @Bean SockJsClient
    // - @Bean WebSocketStompClient
    // - @Bean WebSocketConnectionHandler
}
