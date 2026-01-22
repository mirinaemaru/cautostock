package maru.trading.domain.market;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Instrument Domain Test")
class InstrumentTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create instrument with all parameters")
        void shouldCreateInstrumentWithAllParameters() {
            Instrument instrument = new Instrument(
                    "005930",
                    "KOSPI",
                    true,
                    false,
                    BigDecimal.ONE,
                    1
            );

            assertThat(instrument.getSymbol()).isEqualTo("005930");
            assertThat(instrument.getMarket()).isEqualTo("KOSPI");
            assertThat(instrument.isTradable()).isTrue();
            assertThat(instrument.isHalted()).isFalse();
            assertThat(instrument.getTickSize()).isEqualTo(BigDecimal.ONE);
            assertThat(instrument.getLotSize()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isTradeAllowed Tests")
    class IsTradeAllowedTests {

        @Test
        @DisplayName("Should return true when tradable and not halted")
        void shouldReturnTrueWhenTradableAndNotHalted() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", true, false, BigDecimal.ONE, 1
            );

            assertThat(instrument.isTradeAllowed()).isTrue();
        }

        @Test
        @DisplayName("Should return false when not tradable")
        void shouldReturnFalseWhenNotTradable() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", false, false, BigDecimal.ONE, 1
            );

            assertThat(instrument.isTradeAllowed()).isFalse();
        }

        @Test
        @DisplayName("Should return false when halted")
        void shouldReturnFalseWhenHalted() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", true, true, BigDecimal.ONE, 1
            );

            assertThat(instrument.isTradeAllowed()).isFalse();
        }

        @Test
        @DisplayName("Should return false when not tradable and halted")
        void shouldReturnFalseWhenNotTradableAndHalted() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", false, true, BigDecimal.ONE, 1
            );

            assertThat(instrument.isTradeAllowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for valid instrument")
        void shouldPassValidationForValidInstrument() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", true, false, BigDecimal.ONE, 1
            );

            // Should not throw
            instrument.validate();
        }

        @Test
        @DisplayName("Should throw for null symbol")
        void shouldThrowForNullSymbol() {
            Instrument instrument = new Instrument(
                    null, "KOSPI", true, false, BigDecimal.ONE, 1
            );

            assertThatThrownBy(instrument::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for blank symbol")
        void shouldThrowForBlankSymbol() {
            Instrument instrument = new Instrument(
                    "   ", "KOSPI", true, false, BigDecimal.ONE, 1
            );

            assertThatThrownBy(instrument::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for null market")
        void shouldThrowForNullMarket() {
            Instrument instrument = new Instrument(
                    "005930", null, true, false, BigDecimal.ONE, 1
            );

            assertThatThrownBy(instrument::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Market");
        }

        @Test
        @DisplayName("Should throw for null tick size")
        void shouldThrowForNullTickSize() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", true, false, null, 1
            );

            assertThatThrownBy(instrument::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tick size");
        }

        @Test
        @DisplayName("Should throw for non-positive tick size")
        void shouldThrowForNonPositiveTickSize() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", true, false, BigDecimal.ZERO, 1
            );

            assertThatThrownBy(instrument::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tick size");
        }

        @Test
        @DisplayName("Should throw for non-positive lot size")
        void shouldThrowForNonPositiveLotSize() {
            Instrument instrument = new Instrument(
                    "005930", "KOSPI", true, false, BigDecimal.ONE, 0
            );

            assertThatThrownBy(instrument::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Lot size");
        }
    }

    @Test
    @DisplayName("Should have toString representation")
    void shouldHaveToString() {
        Instrument instrument = new Instrument(
                "005930", "KOSPI", true, false, BigDecimal.ONE, 1
        );

        String toString = instrument.toString();

        assertThat(toString).contains("005930");
        assertThat(toString).contains("KOSPI");
    }
}
