package maru.trading.domain.backtest.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BarData Test")
class BarDataTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create bar data with builder")
        void shouldCreateBarDataWithBuilder() {
            LocalDateTime timestamp = LocalDateTime.now();

            BarData bar = BarData.builder()
                    .symbol("005930")
                    .timeframe("1d")
                    .timestamp(timestamp)
                    .open(BigDecimal.valueOf(70000))
                    .high(BigDecimal.valueOf(72000))
                    .low(BigDecimal.valueOf(69000))
                    .close(BigDecimal.valueOf(71000))
                    .volume(1000000L)
                    .build();

            assertThat(bar.getSymbol()).isEqualTo("005930");
            assertThat(bar.getTimeframe()).isEqualTo("1d");
            assertThat(bar.getTimestamp()).isEqualTo(timestamp);
            assertThat(bar.getOpen()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bar.getHigh()).isEqualTo(BigDecimal.valueOf(72000));
            assertThat(bar.getLow()).isEqualTo(BigDecimal.valueOf(69000));
            assertThat(bar.getClose()).isEqualTo(BigDecimal.valueOf(71000));
            assertThat(bar.getVolume()).isEqualTo(1000000L);
        }

        @Test
        @DisplayName("Should create bar data with factory method")
        void shouldCreateBarDataWithFactoryMethod() {
            LocalDateTime timestamp = LocalDateTime.now();

            BarData bar = BarData.of(
                    "005930",
                    "1d",
                    timestamp,
                    BigDecimal.valueOf(70000),
                    BigDecimal.valueOf(72000),
                    BigDecimal.valueOf(69000),
                    BigDecimal.valueOf(71000),
                    1000000L
            );

            assertThat(bar.getSymbol()).isEqualTo("005930");
            assertThat(bar.getClose()).isEqualTo(BigDecimal.valueOf(71000));
        }
    }

    @Nested
    @DisplayName("isBullish Tests")
    class IsBullishTests {

        @Test
        @DisplayName("Should return true when close > open")
        void shouldReturnTrueWhenCloseGreaterThanOpen() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .close(BigDecimal.valueOf(72000))
                    .build();

            assertThat(bar.isBullish()).isTrue();
        }

        @Test
        @DisplayName("Should return false when close < open")
        void shouldReturnFalseWhenCloseLessThanOpen() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(72000))
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.isBullish()).isFalse();
        }

        @Test
        @DisplayName("Should return false when close equals open")
        void shouldReturnFalseWhenCloseEqualsOpen() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.isBullish()).isFalse();
        }

        @Test
        @DisplayName("Should return false when close is null")
        void shouldReturnFalseWhenCloseIsNull() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.isBullish()).isFalse();
        }

        @Test
        @DisplayName("Should return false when open is null")
        void shouldReturnFalseWhenOpenIsNull() {
            BarData bar = BarData.builder()
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.isBullish()).isFalse();
        }
    }

    @Nested
    @DisplayName("isBearish Tests")
    class IsBearishTests {

        @Test
        @DisplayName("Should return true when close < open")
        void shouldReturnTrueWhenCloseLessThanOpen() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(72000))
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.isBearish()).isTrue();
        }

        @Test
        @DisplayName("Should return false when close > open")
        void shouldReturnFalseWhenCloseGreaterThanOpen() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .close(BigDecimal.valueOf(72000))
                    .build();

            assertThat(bar.isBearish()).isFalse();
        }

        @Test
        @DisplayName("Should return false when close equals open")
        void shouldReturnFalseWhenCloseEqualsOpen() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.isBearish()).isFalse();
        }
    }

    @Nested
    @DisplayName("getRange Tests")
    class GetRangeTests {

        @Test
        @DisplayName("Should calculate range correctly")
        void shouldCalculateRangeCorrectly() {
            BarData bar = BarData.builder()
                    .high(BigDecimal.valueOf(72000))
                    .low(BigDecimal.valueOf(69000))
                    .build();

            assertThat(bar.getRange()).isEqualTo(BigDecimal.valueOf(3000));
        }

        @Test
        @DisplayName("Should return zero when high is null")
        void shouldReturnZeroWhenHighIsNull() {
            BarData bar = BarData.builder()
                    .low(BigDecimal.valueOf(69000))
                    .build();

            assertThat(bar.getRange()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return zero when low is null")
        void shouldReturnZeroWhenLowIsNull() {
            BarData bar = BarData.builder()
                    .high(BigDecimal.valueOf(72000))
                    .build();

            assertThat(bar.getRange()).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("getBody Tests")
    class GetBodyTests {

        @Test
        @DisplayName("Should calculate body for bullish bar")
        void shouldCalculateBodyForBullishBar() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .close(BigDecimal.valueOf(72000))
                    .build();

            assertThat(bar.getBody()).isEqualTo(BigDecimal.valueOf(2000));
        }

        @Test
        @DisplayName("Should calculate body for bearish bar")
        void shouldCalculateBodyForBearishBar() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(72000))
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.getBody()).isEqualTo(BigDecimal.valueOf(2000));
        }

        @Test
        @DisplayName("Should return zero for doji")
        void shouldReturnZeroForDoji() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .close(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.getBody()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return zero when close is null")
        void shouldReturnZeroWhenCloseIsNull() {
            BarData bar = BarData.builder()
                    .open(BigDecimal.valueOf(70000))
                    .build();

            assertThat(bar.getBody()).isEqualTo(BigDecimal.ZERO);
        }
    }
}
