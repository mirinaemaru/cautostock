package maru.trading.broker.kis.marketdata;

import lombok.Getter;
import maru.trading.domain.market.MarketTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tick Data Validator.
 *
 * Validates incoming tick data for quality and correctness.
 */
@Component
public class TickDataValidator {

    private static final Logger log = LoggerFactory.getLogger(TickDataValidator.class);

    // Validation rules
    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(10_000_000);
    private static final long MIN_VOLUME = 0;
    private static final long MAX_VOLUME = 100_000_000;

    /**
     * Validate tick data.
     *
     * @param tick Market tick to validate
     * @return Validation result
     */
    public ValidationResult validate(MarketTick tick) {
        // 1. Check for null
        if (tick == null) {
            return ValidationResult.invalid("Tick is null");
        }

        // 2. Check symbol
        if (tick.getSymbol() == null || tick.getSymbol().isEmpty()) {
            return ValidationResult.invalid("Symbol is null or empty");
        }

        // 3. Check timestamp
        if (tick.getTimestamp() == null) {
            return ValidationResult.invalid("Timestamp is null");
        }

        // Check timestamp is not in future
        if (tick.getTimestamp().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return ValidationResult.invalid("Timestamp is in future: " + tick.getTimestamp());
        }

        // 4. Check price
        if (tick.getPrice() == null) {
            return ValidationResult.invalid("Price is null");
        }

        if (tick.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Price is zero or negative: " + tick.getPrice());
        }

        if (tick.getPrice().compareTo(MIN_PRICE) < 0) {
            return ValidationResult.invalid("Price too low: " + tick.getPrice());
        }

        if (tick.getPrice().compareTo(MAX_PRICE) > 0) {
            return ValidationResult.invalid("Price too high: " + tick.getPrice());
        }

        // 5. Check volume
        if (tick.getVolume() < MIN_VOLUME) {
            return ValidationResult.invalid("Volume is negative: " + tick.getVolume());
        }

        if (tick.getVolume() > MAX_VOLUME) {
            return ValidationResult.invalid("Volume too high: " + tick.getVolume());
        }

        // All checks passed
        return ValidationResult.valid();
    }

    /**
     * Validation result.
     */
    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
