package maru.trading.domain.market;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketTick Domain Test")
class MarketTickTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create tick with all parameters")
        void shouldCreateTickWithAllParameters() {
            LocalDateTime timestamp = LocalDateTime.now();

            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    1000,
                    timestamp,
                    "NORMAL"
            );

            assertThat(tick.getSymbol()).isEqualTo("005930");
            assertThat(tick.getPrice()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(tick.getVolume()).isEqualTo(1000);
            assertThat(tick.getTimestamp()).isEqualTo(timestamp);
            assertThat(tick.getTradingStatus()).isEqualTo("NORMAL");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for valid tick")
        void shouldPassValidationForValidTick() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    1000,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            // Should not throw
            tick.validate();
        }

        @Test
        @DisplayName("Should throw for null symbol")
        void shouldThrowForNullSymbol() {
            MarketTick tick = new MarketTick(
                    null,
                    BigDecimal.valueOf(70000),
                    1000,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            assertThatThrownBy(tick::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for blank symbol")
        void shouldThrowForBlankSymbol() {
            MarketTick tick = new MarketTick(
                    "   ",
                    BigDecimal.valueOf(70000),
                    1000,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            assertThatThrownBy(tick::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for null price")
        void shouldThrowForNullPrice() {
            MarketTick tick = new MarketTick(
                    "005930",
                    null,
                    1000,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            assertThatThrownBy(tick::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price");
        }

        @Test
        @DisplayName("Should throw for non-positive price")
        void shouldThrowForNonPositivePrice() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.ZERO,
                    1000,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            assertThatThrownBy(tick::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price");
        }

        @Test
        @DisplayName("Should throw for negative volume")
        void shouldThrowForNegativeVolume() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    -1,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            assertThatThrownBy(tick::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Volume");
        }

        @Test
        @DisplayName("Should throw for null timestamp")
        void shouldThrowForNullTimestamp() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    1000,
                    null,
                    "NORMAL"
            );

            assertThatThrownBy(tick::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Timestamp");
        }

        @Test
        @DisplayName("Should allow zero volume")
        void shouldAllowZeroVolume() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    0,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            // Should not throw
            tick.validate();
        }
    }

    @Nested
    @DisplayName("isDelayed Tests")
    class IsDelayedTests {

        @Test
        @DisplayName("Should return false for recent tick")
        void shouldReturnFalseForRecentTick() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    1000,
                    LocalDateTime.now(),
                    "NORMAL"
            );

            assertThat(tick.isDelayed(Duration.ofSeconds(5))).isFalse();
        }

        @Test
        @DisplayName("Should return true for old tick")
        void shouldReturnTrueForOldTick() {
            MarketTick tick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    1000,
                    LocalDateTime.now().minusSeconds(10),
                    "NORMAL"
            );

            assertThat(tick.isDelayed(Duration.ofSeconds(5))).isTrue();
        }
    }

    @Nested
    @DisplayName("compareTo Tests")
    class CompareToTests {

        @Test
        @DisplayName("Should compare by timestamp - older first")
        void shouldCompareByTimestampOlderFirst() {
            LocalDateTime now = LocalDateTime.now();
            MarketTick olderTick = new MarketTick(
                    "005930", BigDecimal.valueOf(70000), 1000, now.minusSeconds(10), "NORMAL"
            );
            MarketTick newerTick = new MarketTick(
                    "005930", BigDecimal.valueOf(70100), 500, now, "NORMAL"
            );

            assertThat(olderTick.compareTo(newerTick)).isLessThan(0);
            assertThat(newerTick.compareTo(olderTick)).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should return zero for same timestamp")
        void shouldReturnZeroForSameTimestamp() {
            LocalDateTime now = LocalDateTime.now();
            MarketTick tick1 = new MarketTick(
                    "005930", BigDecimal.valueOf(70000), 1000, now, "NORMAL"
            );
            MarketTick tick2 = new MarketTick(
                    "005930", BigDecimal.valueOf(70100), 500, now, "NORMAL"
            );

            assertThat(tick1.compareTo(tick2)).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should have toString representation")
    void shouldHaveToString() {
        MarketTick tick = new MarketTick(
                "005930",
                BigDecimal.valueOf(70000),
                1000,
                LocalDateTime.now(),
                "NORMAL"
        );

        String toString = tick.toString();

        assertThat(toString).contains("005930");
        assertThat(toString).contains("70000");
        assertThat(toString).contains("NORMAL");
    }
}
