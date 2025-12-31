package maru.trading.broker.kis.mapper;

import maru.trading.broker.kis.dto.KisTickMessage;
import maru.trading.domain.market.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper for KIS tick DTO to MarketTick domain model.
 */
@Component
public class KisTickMapper {

    private static final Logger log = LoggerFactory.getLogger(KisTickMapper.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    /**
     * Convert KIS tick message to domain model.
     *
     * @param message KIS tick DTO
     * @return MarketTick domain model
     */
    public MarketTick toDomain(KisTickMessage message) {
        try {
            String symbol = message.getSymbol();
            BigDecimal price = new BigDecimal(message.getPrice());
            long volume = Long.parseLong(message.getVolume());
            String tradingStatus = message.getTradingStatus() != null ? message.getTradingStatus() : "NORMAL";

            // Parse time (HHMMSS format)
            LocalDateTime timestamp = parseTime(message.getTime());

            return new MarketTick(symbol, price, volume, timestamp, tradingStatus);

        } catch (Exception e) {
            log.error("Error mapping KIS tick message to domain: {}", message, e);
            throw new RuntimeException("Failed to map KIS tick message", e);
        }
    }

    /**
     * Parse KIS time format (HHMMSS) to LocalDateTime.
     * Uses today's date with the provided time.
     */
    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.length() != 6) {
            log.warn("Invalid time format: {}, using current time", timeStr);
            return LocalDateTime.now();
        }

        try {
            LocalTime time = LocalTime.parse(timeStr, TIME_FORMATTER);
            return LocalDateTime.now().with(time);
        } catch (Exception e) {
            log.warn("Failed to parse time: {}, using current time", timeStr, e);
            return LocalDateTime.now();
        }
    }
}
