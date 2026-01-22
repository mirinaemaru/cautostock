package maru.trading.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("IndicatorLibrary Test")
class IndicatorLibraryTest {

    private List<BigDecimal> createPrices(double... values) {
        return Arrays.stream(values)
                .mapToObj(BigDecimal::valueOf)
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("MA (Simple Moving Average) Tests")
    class MATests {

        @Test
        @DisplayName("Should calculate MA correctly")
        void shouldCalculateMACorrectly() {
            List<BigDecimal> prices = createPrices(10, 11, 12, 13, 14);

            List<BigDecimal> ma = IndicatorLibrary.calculateMA(prices, 3);

            assertThat(ma).hasSize(3); // 5 prices - 3 period + 1 = 3 MA values
            assertThat(ma.get(0).doubleValue()).isCloseTo(11.0, within(0.01)); // (10+11+12)/3
            assertThat(ma.get(1).doubleValue()).isCloseTo(12.0, within(0.01)); // (11+12+13)/3
            assertThat(ma.get(2).doubleValue()).isCloseTo(13.0, within(0.01)); // (12+13+14)/3
        }

        @Test
        @DisplayName("Should throw for null prices")
        void shouldThrowForNullPrices() {
            assertThatThrownBy(() -> IndicatorLibrary.calculateMA(null, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("Should throw for insufficient data")
        void shouldThrowForInsufficientData() {
            List<BigDecimal> prices = createPrices(10, 11);

            assertThatThrownBy(() -> IndicatorLibrary.calculateMA(prices, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient data");
        }

        @Test
        @DisplayName("Should throw for invalid period")
        void shouldThrowForInvalidPeriod() {
            List<BigDecimal> prices = createPrices(10, 11, 12);

            assertThatThrownBy(() -> IndicatorLibrary.calculateMA(prices, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("positive");
        }
    }

    @Nested
    @DisplayName("EMA (Exponential Moving Average) Tests")
    class EMATests {

        @Test
        @DisplayName("Should calculate EMA correctly")
        void shouldCalculateEMACorrectly() {
            List<BigDecimal> prices = createPrices(10, 11, 12, 13, 14, 15);

            List<BigDecimal> ema = IndicatorLibrary.calculateEMA(prices, 3);

            assertThat(ema).hasSize(4); // 6 prices - 3 period + 1 = 4 EMA values
            assertThat(ema.get(0).doubleValue()).isCloseTo(11.0, within(0.01)); // First EMA = SMA
        }

        @Test
        @DisplayName("Should throw for insufficient data")
        void shouldThrowForInsufficientData() {
            List<BigDecimal> prices = createPrices(10, 11);

            assertThatThrownBy(() -> IndicatorLibrary.calculateEMA(prices, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient data");
        }
    }

    @Nested
    @DisplayName("RSI (Relative Strength Index) Tests")
    class RSITests {

        @Test
        @DisplayName("Should calculate RSI correctly")
        void shouldCalculateRSICorrectly() {
            // Create prices with clear uptrend
            List<BigDecimal> prices = IntStream.rangeClosed(1, 20)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(Collectors.toList());

            List<BigDecimal> rsi = IndicatorLibrary.calculateRSI(prices, 14);

            assertThat(rsi).isNotEmpty();
            // In strong uptrend, RSI should be high (above 50)
            assertThat(rsi.get(0).doubleValue()).isGreaterThan(50);
        }

        @Test
        @DisplayName("Should return 100 when no losses")
        void shouldReturn100WhenNoLosses() {
            // All gains, no losses
            List<BigDecimal> prices = createPrices(100, 101, 102, 103, 104, 105);

            List<BigDecimal> rsi = IndicatorLibrary.calculateRSI(prices, 3);

            assertThat(rsi.get(0).doubleValue()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should throw for insufficient data")
        void shouldThrowForInsufficientData() {
            List<BigDecimal> prices = createPrices(10, 11, 12);

            assertThatThrownBy(() -> IndicatorLibrary.calculateRSI(prices, 14))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient data");
        }
    }

    @Nested
    @DisplayName("Bollinger Bands Tests")
    class BollingerBandsTests {

        @Test
        @DisplayName("Should calculate Bollinger Bands correctly")
        void shouldCalculateBollingerBandsCorrectly() {
            List<BigDecimal> prices = createPrices(20, 21, 22, 23, 24, 25);

            List<IndicatorLibrary.BollingerBands> bb = IndicatorLibrary.calculateBollingerBands(prices, 3, 2.0);

            assertThat(bb).hasSize(4); // 6 prices - 3 period + 1 = 4 BB values
            assertThat(bb.get(0).getUpper().doubleValue()).isGreaterThan(bb.get(0).getMiddle().doubleValue());
            assertThat(bb.get(0).getLower().doubleValue()).isLessThan(bb.get(0).getMiddle().doubleValue());
        }

        @Test
        @DisplayName("Should throw for invalid multiplier")
        void shouldThrowForInvalidMultiplier() {
            List<BigDecimal> prices = createPrices(20, 21, 22, 23, 24);

            assertThatThrownBy(() -> IndicatorLibrary.calculateBollingerBands(prices, 3, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("multiplier must be positive");
        }
    }

    @Nested
    @DisplayName("MACD Tests")
    class MACDTests {

        @Test
        @DisplayName("Should calculate MACD correctly")
        void shouldCalculateMACDCorrectly() {
            // Need enough data points for MACD (26 + 9 - 1 = 34 minimum)
            List<BigDecimal> prices = IntStream.rangeClosed(1, 40)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(Collectors.toList());

            List<IndicatorLibrary.MACD> macd = IndicatorLibrary.calculateMACD(prices, 12, 26, 9);

            assertThat(macd).isNotEmpty();
            assertThat(macd.get(0).getMacdLine()).isNotNull();
            assertThat(macd.get(0).getSignalLine()).isNotNull();
            assertThat(macd.get(0).getHistogram()).isNotNull();
        }

        @Test
        @DisplayName("Should throw for fast >= slow period")
        void shouldThrowForInvalidPeriods() {
            List<BigDecimal> prices = IntStream.rangeClosed(1, 40)
                    .mapToObj(BigDecimal::valueOf)
                    .collect(Collectors.toList());

            assertThatThrownBy(() -> IndicatorLibrary.calculateMACD(prices, 26, 12, 9))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Fast period must be less than slow period");
        }

        @Test
        @DisplayName("Should throw for insufficient data")
        void shouldThrowForInsufficientData() {
            List<BigDecimal> prices = createPrices(10, 11, 12);

            assertThatThrownBy(() -> IndicatorLibrary.calculateMACD(prices, 12, 26, 9))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient data");
        }
    }
}
