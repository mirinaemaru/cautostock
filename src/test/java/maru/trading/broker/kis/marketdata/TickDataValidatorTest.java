package maru.trading.broker.kis.marketdata;

import maru.trading.domain.market.MarketTick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TickDataValidator Test")
class TickDataValidatorTest {

    private TickDataValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TickDataValidator();
    }

    @Nested
    @DisplayName("Valid Tick Tests")
    class ValidTickTests {

        @Test
        @DisplayName("Should return valid for correct tick data")
        void shouldReturnValidForCorrectTickData() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should return valid for minimum allowed price")
        void shouldReturnValidForMinimumAllowedPrice() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(100), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return valid for maximum allowed price")
        void shouldReturnValidForMaximumAllowedPrice() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(10_000_000), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should return valid for zero volume")
        void shouldReturnValidForZeroVolume() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), 0L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Null and Empty Tests")
    class NullAndEmptyTests {

        @Test
        @DisplayName("Should return invalid for null tick")
        void shouldReturnInvalidForNullTick() {
            // When
            TickDataValidator.ValidationResult result = validator.validate(null);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("Tick is null");
        }

        @Test
        @DisplayName("Should return invalid for null symbol")
        void shouldReturnInvalidForNullSymbol() {
            // Given
            MarketTick tick = new MarketTick(null, BigDecimal.valueOf(70000), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Symbol is null or empty");
        }

        @Test
        @DisplayName("Should return invalid for empty symbol")
        void shouldReturnInvalidForEmptySymbol() {
            // Given
            MarketTick tick = new MarketTick("", BigDecimal.valueOf(70000), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Symbol is null or empty");
        }

        @Test
        @DisplayName("Should return invalid for null timestamp")
        void shouldReturnInvalidForNullTimestamp() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), 1000L,
                    null, "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Timestamp is null");
        }

        @Test
        @DisplayName("Should return invalid for null price")
        void shouldReturnInvalidForNullPrice() {
            // Given
            MarketTick tick = new MarketTick("005930", null, 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Price is null");
        }
    }

    @Nested
    @DisplayName("Price Validation Tests")
    class PriceValidationTests {

        @Test
        @DisplayName("Should return invalid for zero price")
        void shouldReturnInvalidForZeroPrice() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.ZERO, 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Price is zero or negative");
        }

        @Test
        @DisplayName("Should return invalid for negative price")
        void shouldReturnInvalidForNegativePrice() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(-100), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Price is zero or negative");
        }

        @Test
        @DisplayName("Should return invalid for price below minimum")
        void shouldReturnInvalidForPriceBelowMinimum() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(50), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Price too low");
        }

        @Test
        @DisplayName("Should return invalid for price above maximum")
        void shouldReturnInvalidForPriceAboveMaximum() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(15_000_000), 1000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Price too high");
        }
    }

    @Nested
    @DisplayName("Volume Validation Tests")
    class VolumeValidationTests {

        @Test
        @DisplayName("Should return invalid for negative volume")
        void shouldReturnInvalidForNegativeVolume() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), -100L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Volume is negative");
        }

        @Test
        @DisplayName("Should return invalid for volume above maximum")
        void shouldReturnInvalidForVolumeAboveMaximum() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), 200_000_000L,
                    LocalDateTime.now(), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Volume too high");
        }
    }

    @Nested
    @DisplayName("Timestamp Validation Tests")
    class TimestampValidationTests {

        @Test
        @DisplayName("Should return invalid for future timestamp")
        void shouldReturnInvalidForFutureTimestamp() {
            // Given
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), 1000L,
                    LocalDateTime.now().plusMinutes(5), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Timestamp is in future");
        }

        @Test
        @DisplayName("Should return valid for timestamp within 1 minute in future")
        void shouldReturnValidForTimestampWithin1MinuteInFuture() {
            // Given - timestamp within 1 minute tolerance
            MarketTick tick = new MarketTick("005930", BigDecimal.valueOf(70000), 1000L,
                    LocalDateTime.now().plusSeconds(30), "NORMAL");

            // When
            TickDataValidator.ValidationResult result = validator.validate(tick);

            // Then
            assertThat(result.isValid()).isTrue();
        }
    }
}
