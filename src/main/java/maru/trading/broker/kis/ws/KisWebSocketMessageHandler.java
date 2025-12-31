package maru.trading.broker.kis.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.broker.kis.dto.KisFillMessage;
import maru.trading.broker.kis.dto.KisTickMessage;
import maru.trading.broker.kis.mapper.KisFillMapper;
import maru.trading.broker.kis.mapper.KisTickMapper;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.market.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler for parsing incoming KIS WebSocket messages.
 *
 * In MVP (stub mode), this handles simulated messages.
 * In production, this would parse real KIS binary/JSON format.
 */
@Component
public class KisWebSocketMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(KisWebSocketMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final KisTickMapper tickMapper;
    private final KisFillMapper fillMapper;

    public KisWebSocketMessageHandler(
            ObjectMapper objectMapper,
            KisTickMapper tickMapper,
            KisFillMapper fillMapper) {
        this.objectMapper = objectMapper;
        this.tickMapper = tickMapper;
        this.fillMapper = fillMapper;
    }

    /**
     * Handle incoming WebSocket message.
     * Routes to appropriate parser based on message type.
     *
     * @param rawMessage Raw message string
     */
    public void handleMessage(String rawMessage) {
        try {
            log.debug("Received WebSocket message: {}", rawMessage);

            // In production, parse message header to determine type
            // For MVP stub, we'll use simple prefix detection
            if (rawMessage.contains("\"type\":\"tick\"")) {
                handleTickMessage(rawMessage);
            } else if (rawMessage.contains("\"type\":\"fill\"")) {
                handleFillMessage(rawMessage);
            } else {
                log.warn("Unknown message type, ignoring: {}", rawMessage);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message: {}", rawMessage, e);
        }
    }

    /**
     * Parse tick message and return MarketTick domain model.
     */
    public MarketTick parseTickMessage(String message) {
        try {
            // In production, parse KIS-specific format (binary or JSON)
            // For MVP stub, parse simple JSON
            KisTickMessage dto = objectMapper.readValue(message, KisTickMessage.class);
            return tickMapper.toDomain(dto);

        } catch (Exception e) {
            log.error("Error parsing tick message: {}", message, e);
            throw new RuntimeException("Failed to parse tick message", e);
        }
    }

    /**
     * Parse fill message and return Fill domain model.
     */
    public Fill parseFillMessage(String message, String orderId, String accountId) {
        try {
            // In production, parse KIS-specific format
            // For MVP stub, parse simple JSON
            KisFillMessage dto = objectMapper.readValue(message, KisFillMessage.class);
            return fillMapper.toDomain(dto, orderId, accountId);

        } catch (Exception e) {
            log.error("Error parsing fill message: {}", message, e);
            throw new RuntimeException("Failed to parse fill message", e);
        }
    }

    private void handleTickMessage(String message) {
        log.debug("Handling tick message");
        // Actual handling delegated to KisWebSocketClient callbacks
    }

    private void handleFillMessage(String message) {
        log.debug("Handling fill message");
        // Actual handling delegated to KisWebSocketClient callbacks
    }
}
