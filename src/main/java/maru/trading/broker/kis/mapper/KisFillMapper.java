package maru.trading.broker.kis.mapper;

import maru.trading.broker.kis.dto.KisFillMessage;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Mapper for KIS fill DTO to Fill domain model.
 */
@Component
public class KisFillMapper {

    private static final Logger log = LoggerFactory.getLogger(KisFillMapper.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final UlidGenerator ulidGenerator;

    public KisFillMapper(UlidGenerator ulidGenerator) {
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Convert KIS fill message to domain model.
     *
     * @param message KIS fill DTO
     * @param orderId Internal order ID (from order lookup by brokerOrderNo)
     * @param accountId Account ID
     * @return Fill domain model
     */
    public Fill toDomain(KisFillMessage message, String orderId, String accountId) {
        try {
            String fillId = ulidGenerator.generateInstance();
            String symbol = message.getSymbol();
            Side side = parseSide(message.getSideCode());
            BigDecimal fillPrice = new BigDecimal(message.getFillPrice());
            int fillQty = Integer.parseInt(message.getFillQty());

            // TODO: In production, calculate fees and taxes based on broker rules
            BigDecimal fee = calculateFee(fillPrice, fillQty);
            BigDecimal tax = calculateTax(fillPrice, fillQty, side);

            LocalDateTime fillTimestamp = parseTime(message.getFillTime());
            String brokerOrderNo = message.getBrokerOrderNo();

            return new Fill(
                    fillId,
                    orderId,
                    accountId,
                    symbol,
                    side,
                    fillPrice,
                    fillQty,
                    fee,
                    tax,
                    fillTimestamp,
                    brokerOrderNo
            );

        } catch (Exception e) {
            log.error("Error mapping KIS fill message to domain: {}", message, e);
            throw new RuntimeException("Failed to map KIS fill message", e);
        }
    }

    /**
     * Parse KIS side code to Side enum.
     * "01" = BUY, "02" = SELL
     */
    private Side parseSide(String sideCode) {
        if ("01".equals(sideCode)) {
            return Side.BUY;
        } else if ("02".equals(sideCode)) {
            return Side.SELL;
        } else {
            log.warn("Unknown side code: {}, defaulting to BUY", sideCode);
            return Side.BUY;
        }
    }

    /**
     * Calculate fee (stub implementation).
     * In production, use broker-specific fee schedule.
     */
    private BigDecimal calculateFee(BigDecimal price, int qty) {
        // Simple fee: 0.015% of transaction value
        BigDecimal transactionValue = price.multiply(BigDecimal.valueOf(qty));
        return transactionValue.multiply(new BigDecimal("0.00015"));
    }

    /**
     * Calculate tax (stub implementation).
     * In production, apply Korea securities transaction tax (0.23% for sells).
     */
    private BigDecimal calculateTax(BigDecimal price, int qty, Side side) {
        if (side == Side.SELL) {
            // Korea securities transaction tax: 0.23% on sells
            BigDecimal transactionValue = price.multiply(BigDecimal.valueOf(qty));
            return transactionValue.multiply(new BigDecimal("0.0023"));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Parse KIS time format (HHMMSS) to LocalDateTime.
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
