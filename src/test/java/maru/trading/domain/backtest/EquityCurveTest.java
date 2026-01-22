package maru.trading.domain.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquityCurve Test")
class EquityCurveTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create empty equity curve")
        void shouldCreateEmptyEquityCurve() {
            EquityCurve curve = EquityCurve.builder().build();

            assertThat(curve.getPoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addPoint Tests")
    class AddPointTests {

        @Test
        @DisplayName("Should add equity point")
        void shouldAddEquityPoint() {
            EquityCurve curve = EquityCurve.builder().build();
            LocalDateTime timestamp = LocalDateTime.now();

            curve.addPoint(timestamp, BigDecimal.valueOf(10_000_000));

            assertThat(curve.getPoints()).hasSize(1);
            assertThat(curve.getPoints().get(0).getTimestamp()).isEqualTo(timestamp);
            assertThat(curve.getPoints().get(0).getEquity()).isEqualTo(BigDecimal.valueOf(10_000_000));
        }

        @Test
        @DisplayName("Should add multiple points in order")
        void shouldAddMultiplePointsInOrder() {
            EquityCurve curve = EquityCurve.builder().build();
            LocalDateTime t1 = LocalDateTime.now().minusDays(2);
            LocalDateTime t2 = LocalDateTime.now().minusDays(1);
            LocalDateTime t3 = LocalDateTime.now();

            curve.addPoint(t1, BigDecimal.valueOf(10_000_000));
            curve.addPoint(t2, BigDecimal.valueOf(10_500_000));
            curve.addPoint(t3, BigDecimal.valueOf(11_000_000));

            assertThat(curve.getPoints()).hasSize(3);
            assertThat(curve.getPoints().get(0).getEquity()).isEqualTo(BigDecimal.valueOf(10_000_000));
            assertThat(curve.getPoints().get(2).getEquity()).isEqualTo(BigDecimal.valueOf(11_000_000));
        }
    }

    @Nested
    @DisplayName("getCurrentEquity Tests")
    class GetCurrentEquityTests {

        @Test
        @DisplayName("Should return zero for empty curve")
        void shouldReturnZeroForEmptyCurve() {
            EquityCurve curve = EquityCurve.builder().build();

            assertThat(curve.getCurrentEquity()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return last equity value")
        void shouldReturnLastEquityValue() {
            EquityCurve curve = EquityCurve.builder().build();
            curve.addPoint(LocalDateTime.now().minusDays(1), BigDecimal.valueOf(10_000_000));
            curve.addPoint(LocalDateTime.now(), BigDecimal.valueOf(12_000_000));

            assertThat(curve.getCurrentEquity()).isEqualTo(BigDecimal.valueOf(12_000_000));
        }

        @Test
        @DisplayName("Should return single point equity")
        void shouldReturnSinglePointEquity() {
            EquityCurve curve = EquityCurve.builder().build();
            curve.addPoint(LocalDateTime.now(), BigDecimal.valueOf(10_000_000));

            assertThat(curve.getCurrentEquity()).isEqualTo(BigDecimal.valueOf(10_000_000));
        }
    }

    @Nested
    @DisplayName("getInitialEquity Tests")
    class GetInitialEquityTests {

        @Test
        @DisplayName("Should return zero for empty curve")
        void shouldReturnZeroForEmptyCurve() {
            EquityCurve curve = EquityCurve.builder().build();

            assertThat(curve.getInitialEquity()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return first equity value")
        void shouldReturnFirstEquityValue() {
            EquityCurve curve = EquityCurve.builder().build();
            curve.addPoint(LocalDateTime.now().minusDays(1), BigDecimal.valueOf(10_000_000));
            curve.addPoint(LocalDateTime.now(), BigDecimal.valueOf(12_000_000));

            assertThat(curve.getInitialEquity()).isEqualTo(BigDecimal.valueOf(10_000_000));
        }
    }

    @Nested
    @DisplayName("EquityPoint Tests")
    class EquityPointTests {

        @Test
        @DisplayName("Should create equity point")
        void shouldCreateEquityPoint() {
            LocalDateTime timestamp = LocalDateTime.now();
            EquityCurve.EquityPoint point = new EquityCurve.EquityPoint(timestamp, BigDecimal.valueOf(15_000_000));

            assertThat(point.getTimestamp()).isEqualTo(timestamp);
            assertThat(point.getEquity()).isEqualTo(BigDecimal.valueOf(15_000_000));
        }

        @Test
        @DisplayName("Should create equity point with no-arg constructor")
        void shouldCreateEquityPointWithNoArgConstructor() {
            EquityCurve.EquityPoint point = new EquityCurve.EquityPoint();

            assertThat(point.getTimestamp()).isNull();
            assertThat(point.getEquity()).isNull();
        }
    }
}
