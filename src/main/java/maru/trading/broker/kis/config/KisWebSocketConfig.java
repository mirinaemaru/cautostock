package maru.trading.broker.kis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for KIS WebSocket client.
 *
 * WebSocketConnectionManager is registered as @Component and injected automatically.
 * This config class enables scheduling for WebSocket operations.
 */
@Configuration
@EnableScheduling
public class KisWebSocketConfig {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketConfig.class);

    public KisWebSocketConfig() {
        log.info("KIS WebSocket configuration initialized");
    }
}
