package maru.trading.broker.kis.fill;

import lombok.Getter;
import maru.trading.domain.execution.Fill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fill Data Validator.
 *
 * Validates incoming fill data for quality and correctness.
 */
@Component
public class FillDataValidator {

    private static final Logger log = LoggerFactory.getLogger(FillDataValidator.class);

    // Validation rules
    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(10_000_000);
    private static final int MIN_QTY = 1;
    private static final int MAX_QTY = 1_000_000;

    /**
     * Validate fill data.
     *
     * @param fill Fill to validate
     * @return Validation result
     */
    public ValidationResult validate(Fill fill) {
        // 1. Check for null
        if (fill == null) {
            return ValidationResult.invalid("Fill is null");
        }

        // 2. Check fill ID
        if (fill.getFillId() == null || fill.getFillId().isEmpty()) {
            return ValidationResult.invalid("Fill ID is null or empty");
        }

        // 3. Check order ID
        if (fill.getOrderId() == null || fill.getOrderId().isEmpty()) {
            return ValidationResult.invalid("Order ID is null or empty");
        }

        // 4. Check timestamp
        if (fill.getFillTimestamp() == null) {
            return ValidationResult.invalid("Timestamp is null");
        }

        // Check timestamp is not in future
        if (fill.getFillTimestamp().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return ValidationResult.invalid("Timestamp is in future: " + fill.getFillTimestamp());
        }

        // 5. Check fill price
        if (fill.getFillPrice() == null) {
            return ValidationResult.invalid("Fill price is null");
        }

        if (fill.getFillPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.invalid("Fill price is zero or negative: " + fill.getFillPrice());
        }

        if (fill.getFillPrice().compareTo(MIN_PRICE) < 0) {
            return ValidationResult.invalid("Fill price too low: " + fill.getFillPrice());
        }

        if (fill.getFillPrice().compareTo(MAX_PRICE) > 0) {
            return ValidationResult.invalid("Fill price too high: " + fill.getFillPrice());
        }

        // 6. Check filled quantity
        if (fill.getFillQty() < MIN_QTY) {
            return ValidationResult.invalid("Filled quantity too low: " + fill.getFillQty());
        }

        if (fill.getFillQty() > MAX_QTY) {
            return ValidationResult.invalid("Filled quantity too high: " + fill.getFillQty());
        }

        // 7. Check symbol (if available)
        if (fill.getSymbol() != null && fill.getSymbol().isEmpty()) {
            return ValidationResult.invalid("Symbol is empty");
        }

        // 8. Check account ID (if available)
        if (fill.getAccountId() != null && fill.getAccountId().isEmpty()) {
            return ValidationResult.invalid("Account ID is empty");
        }

        // All checks passed
        return ValidationResult.valid();
    }

    /**
     * Validate that fill matches the order it's associated with.
     *
     * @param fill Fill to validate
     * @param expectedOrderId Expected order ID
     * @param expectedSymbol Expected symbol
     * @return Validation result
     */
    public ValidationResult validateAgainstOrder(Fill fill, String expectedOrderId, String expectedSymbol) {
        // Basic validation first
        ValidationResult basicResult = validate(fill);
        if (!basicResult.isValid()) {
            return basicResult;
        }

        // Check order ID match
        if (expectedOrderId != null && !expectedOrderId.equals(fill.getOrderId())) {
            return ValidationResult.invalid(
                    String.format("Fill order ID mismatch: expected=%s, actual=%s",
                            expectedOrderId, fill.getOrderId()));
        }

        // Check symbol match
        if (expectedSymbol != null && fill.getSymbol() != null && !expectedSymbol.equals(fill.getSymbol())) {
            return ValidationResult.invalid(
                    String.format("Fill symbol mismatch: expected=%s, actual=%s",
                            expectedSymbol, fill.getSymbol()));
        }

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
