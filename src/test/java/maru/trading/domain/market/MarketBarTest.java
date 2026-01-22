package maru.trading.domain.market;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MarketBar Domain Test")
class MarketBarTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create bar with basic constructor")
        void shouldCreateBarWithBasicConstructor() {
            LocalDateTime timestamp = LocalDateTime.now();

            MarketBar bar = new MarketBar("005930", "1d", timestamp);

            assertThat(bar.getSymbol()).isEqualTo("005930");
            assertThat(bar.getTimeframe()).isEqualTo("1d");
            assertThat(bar.getBarTimestamp()).isEqualTo(timestamp);
            assertThat(bar.isClosed()).isFalse();
            assertThat(bar.getVolume()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should restore bar with all fields")
        void shouldRestoreBarWithAllFields() {
            LocalDateTime timestamp = LocalDateTime.now();

            MarketBar bar = MarketBar.restore(
                    "005930",
                    "1d",
                    timestamp,
                    BigDecimal.valueOf(70000),
                    BigDecimal.valueOf(71000),
                    BigDecimal.valueOf(69000),
                    BigDecimal.valueOf(70500),
                    1000000L,
                    true
            );

            assertThat(bar.getSymbol()).isEqualTo("005930");
            assertThat(bar.getTimeframe()).isEqualTo("1d");
            assertThat(bar.getOpen()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bar.getHigh()).isEqualTo(BigDecimal.valueOf(71000));
            assertThat(bar.getLow()).isEqualTo(BigDecimal.valueOf(69000));
            assertThat(bar.getClose()).isEqualTo(BigDecimal.valueOf(70500));
            assertThat(bar.getVolume()).isEqualTo(1000000L);
            assertThat(bar.isClosed()).isTrue();
        }

        @Test
        @DisplayName("Should handle null values in restore")
        void shouldHandleNullValuesInRestore() {
            MarketBar bar = MarketBar.restore(
                    "005930",
                    "1d",
                    LocalDateTime.now(),
                    BigDecimal.valueOf(70000),
                    BigDecimal.valueOf(71000),
                    BigDecimal.valueOf(69000),
                    BigDecimal.valueOf(70500),
                    null,
                    null
            );

            assertThat(bar.getVolume()).isEqualTo(0L);
            assertThat(bar.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("addTick Tests")
    class AddTickTests {

        @Test
        @DisplayName("Should set open/high/low/close on first tick")
        void shouldSetAllPricesOnFirstTick() {
            MarketBar bar = new MarketBar("005930", "1d", LocalDateTime.now());
            MarketTick tick = new MarketTick(
                    "005930", BigDecimal.valueOf(70000), 100, LocalDateTime.now(), "NORMAL"
            );

            bar.addTick(tick);

            assertThat(bar.getOpen()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bar.getHigh()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bar.getLow()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bar.getClose()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bar.getVolume()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should update high on higher price")
        void shouldUpdateHighOnHigherPrice() {
            MarketBar bar = new MarketBar("005930", "1d", LocalDateTime.now());
            bar.addTick(new MarketTick("005930", BigDecimal.valueOf(70000), 100, LocalDateTime.now(), "NORMAL"));
            bar.addTick(new MarketTick("005930", BigDecimal.valueOf(71000), 50, LocalDateTime.now(), "NORMAL"));

            assertThat(bar.getHigh()).isEqualTo(BigDecimal.valueOf(71000));
            assertThat(bar.getClose()).isEqualTo(BigDecimal.valueOf(71000));
            assertThat(bar.getVolume()).isEqualTo(150);
        }

        @Test
        @DisplayName("Should update low on lower price")
        void shouldUpdateLowOnLowerPrice() {
            MarketBar bar = new MarketBar("005930", "1d", LocalDateTime.now());
            bar.addTick(new MarketTick("005930", BigDecimal.valueOf(70000), 100, LocalDateTime.now(), "NORMAL"));
            bar.addTick(new MarketTick("005930", BigDecimal.valueOf(69000), 50, LocalDateTime.now(), "NORMAL"));

            assertThat(bar.getLow()).isEqualTo(BigDecimal.valueOf(69000));
            assertThat(bar.getClose()).isEqualTo(BigDecimal.valueOf(69000));
        }

        @Test
        @DisplayName("Should throw when adding tick to closed bar")
        void shouldThrowWhenAddingTickToClosedBar() {
            MarketBar bar = MarketBar.restore(
                    "005930", "1d", LocalDateTime.now(),
                    BigDecimal.valueOf(70000), BigDecimal.valueOf(71000),
                    BigDecimal.valueOf(69000), BigDecimal.valueOf(70500),
                    1000L, true
            );
            MarketTick tick = new MarketTick(
                    "005930", BigDecimal.valueOf(70000), 100, LocalDateTime.now(), "NORMAL"
            );

            assertThatThrownBy(() -> bar.addTick(tick))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("closed bar");
        }
    }

    @Nested
    @DisplayName("close Tests")
    class CloseTests {

        @Test
        @DisplayName("Should close bar")
        void shouldCloseBar() {
            MarketBar bar = new MarketBar("005930", "1d", LocalDateTime.now());

            bar.close();

            assertThat(bar.isClosed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for valid bar")
        void shouldPassValidationForValidBar() {
            MarketBar bar = MarketBar.restore(
                    "005930", "1d", LocalDateTime.now(),
                    BigDecimal.valueOf(70000), BigDecimal.valueOf(71000),
                    BigDecimal.valueOf(69000), BigDecimal.valueOf(70500),
                    1000L, true
            );

            // Should not throw
            bar.validate();
        }

        @Test
        @DisplayName("Should throw for null symbol")
        void shouldThrowForNullSymbol() {
            MarketBar bar = new MarketBar(null, "1d", LocalDateTime.now());

            assertThatThrownBy(bar::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for blank symbol")
        void shouldThrowForBlankSymbol() {
            MarketBar bar = new MarketBar("   ", "1d", LocalDateTime.now());

            assertThatThrownBy(bar::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for null timeframe")
        void shouldThrowForNullTimeframe() {
            MarketBar bar = new MarketBar("005930", null, LocalDateTime.now());

            assertThatThrownBy(bar::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Timeframe");
        }

        @Test
        @DisplayName("Should throw for null timestamp")
        void shouldThrowForNullTimestamp() {
            MarketBar bar = new MarketBar("005930", "1d", null);

            assertThatThrownBy(bar::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("timestamp");
        }

        @Test
        @DisplayName("Should throw for bar without ticks")
        void shouldThrowForBarWithoutTicks() {
            MarketBar bar = new MarketBar("005930", "1d", LocalDateTime.now());

            assertThatThrownBy(bar::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tick");
        }
    }

    @Test
    @DisplayName("Should have toString representation")
    void shouldHaveToString() {
        MarketBar bar = MarketBar.restore(
                "005930", "1d", LocalDateTime.now(),
                BigDecimal.valueOf(70000), BigDecimal.valueOf(71000),
                BigDecimal.valueOf(69000), BigDecimal.valueOf(70500),
                1000L, true
        );

        String toString = bar.toString();

        assertThat(toString).contains("005930");
        assertThat(toString).contains("1d");
        assertThat(toString).contains("70000");
    }
}
