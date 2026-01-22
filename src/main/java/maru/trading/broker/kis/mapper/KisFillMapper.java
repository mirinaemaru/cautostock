package maru.trading.broker.kis.mapper;

import maru.trading.broker.kis.dto.KisFillMessage;
import maru.trading.domain.execution.FeeCalculator;
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
    private final FeeCalculator feeCalculator;

    public KisFillMapper(UlidGenerator ulidGenerator, FeeCalculator feeCalculator) {
        this.ulidGenerator = ulidGenerator;
        this.feeCalculator = feeCalculator;
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

            // Calculate fees and taxes using Korea securities rules
            BigDecimal fee = feeCalculator.calculateFee(symbol, fillPrice, fillQty, side);
            BigDecimal tax = feeCalculator.calculateTax(symbol, fillPrice, fillQty, side);

            LocalDateTime fillTimestamp = parseTime(message.getFillTime());
            String brokerOrderNo = message.getBrokerOrderNo();

            log.debug("Fill mapped: symbol={}, price={}, qty={}, fee={}, tax={}",
                    symbol, fillPrice, fillQty, fee, tax);

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
