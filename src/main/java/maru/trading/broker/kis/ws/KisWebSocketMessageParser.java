package maru.trading.broker.kis.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import maru.trading.broker.kis.ws.dto.KisTickMessage;
import maru.trading.domain.market.MarketTick;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * KIS WebSocket message parser (Stage 2).
 *
 * Parses incoming WebSocket messages from KIS API and converts them
 * to domain model MarketTick objects.
 *
 * Supports:
 * - Real-time tick data (H0STCNT0)
 * - Quote data (H0STASP0)
 */
@Slf4j
@Component
public class KisWebSocketMessageParser {

    private final ObjectMapper objectMapper;

    public KisWebSocketMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse KIS WebSocket message to MarketTick.
     *
     * @param message Raw JSON message from KIS WebSocket
     * @return MarketTick domain object, or null if parsing fails
     */
    public MarketTick parseTickMessage(String message) {
        if (message == null || message.isBlank()) {
            log.warn("Received empty message");
            return null;
        }

        try {
            // Handle different message formats
            // KIS sends messages in two formats:
            // 1. Plain JSON (initial messages, errors)
            // 2. Delimited format: "0|H0STCNT0|005930|..." (real-time data)

            if (message.startsWith("{")) {
                // JSON format - parse as KisTickMessage
                return parseJsonMessage(message);
            } else {
                // Delimited format - parse as pipe-separated values
                return parseDelimitedMessage(message);
            }

        } catch (Exception e) {
            log.error("Failed to parse KIS message: {}", message, e);
            return null;
        }
    }

    /**
     * Parse JSON format message.
     */
    private MarketTick parseJsonMessage(String message) throws Exception {
        KisTickMessage kisMsg = objectMapper.readValue(message, KisTickMessage.class);

        if (!kisMsg.isSuccess()) {
            log.warn("KIS message indicates failure: {}", kisMsg.getErrorMessage());
            return null;
        }

        if (kisMsg.getBody() == null || kisMsg.getBody().getOutput() == null) {
            log.debug("KIS message has no output data (possibly control message)");
            return null;
        }

        KisTickMessage.Output output = kisMsg.getBody().getOutput();

        // Extract tick data
        String symbol = output.getMKSC_SHRN_ISCD();
        BigDecimal price = parseBigDecimal(output.getSTCK_PRPR());
        long volume = parseLong(output.getCNTG_VOL());
        LocalDateTime timestamp = parseKisTime(output.getSTCK_CNTG_HOUR());

        // Determine trading status based on price change sign
        String tradingStatus = determineTradingStatus(output.getPRDY_VRSS_SIGN());

        MarketTick tick = new MarketTick(symbol, price, volume, timestamp, tradingStatus);

        log.trace("Parsed JSON tick: symbol={}, price={}, volume={}", symbol, price, volume);

        return tick;
    }

    /**
     * Parse delimited format message.
     *
     * Format: "0|H0STCNT0|005930|72000|100|153000|..."
     * Fields: response_code|tr_id|symbol|price|volume|time|...
     */
    private MarketTick parseDelimitedMessage(String message) {
        String[] fields = message.split("\\|");

        if (fields.length < 6) {
            log.warn("Delimited message has insufficient fields: {}", message);
            return null;
        }

        try {
            String responseCode = fields[0];
            String trId = fields[1];
            String symbol = fields[2];
            BigDecimal price = new BigDecimal(fields[3]);
            long volume = Long.parseLong(fields[4]);
            LocalDateTime timestamp = parseKisTime(fields[5]);

            String tradingStatus = "NORMAL";

            MarketTick tick = new MarketTick(symbol, price, volume, timestamp, tradingStatus);

            log.trace("Parsed delimited tick: symbol={}, price={}, volume={}", symbol, price, volume);

            return tick;

        } catch (Exception e) {
            log.error("Error parsing delimited message: {}", message, e);
            return null;
        }
    }

    /**
     * Parse KIS time format to LocalDateTime.
     *
     * KIS time format: "HHMMSS" (6 digits)
     * Example: "153000" -> 15:30:00
     *
     * @param kisTime KIS time string (HHMMSS)
     * @return LocalDateTime with current date and parsed time
     */
    private LocalDateTime parseKisTime(String kisTime) {
        if (kisTime == null || kisTime.length() != 6) {
            log.warn("Invalid KIS time format: {}", kisTime);
            return LocalDateTime.now();
        }

        try {
            int hour = Integer.parseInt(kisTime.substring(0, 2));
            int minute = Integer.parseInt(kisTime.substring(2, 4));
            int second = Integer.parseInt(kisTime.substring(4, 6));

            LocalDate today = LocalDate.now();
            LocalTime time = LocalTime.of(hour, minute, second);

            return LocalDateTime.of(today, time);

        } catch (Exception e) {
            log.error("Error parsing KIS time: {}", kisTime, e);
            return LocalDateTime.now();
        }
    }

    /**
     * Determine trading status from price change sign.
     *
     * @param sign Price change sign (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)
     * @return Trading status string
     */
    private String determineTradingStatus(String sign) {
        if (sign == null) {
            return "NORMAL";
        }

        return switch (sign) {
            case "1" -> "UPPER_LIMIT";  // 상한가
            case "4" -> "LOWER_LIMIT";  // 하한가
            case "2", "5" -> "NORMAL";  // 상승/하락
            case "3" -> "NORMAL";       // 보합
            default -> "NORMAL";
        };
    }

    /**
     * Safely parse BigDecimal from string.
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid number format: {}", value);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Safely parse long from string.
     */
    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid number format: {}", value);
            return 0L;
        }
    }
}
