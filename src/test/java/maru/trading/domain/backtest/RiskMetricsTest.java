package maru.trading.domain.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskMetrics Test")
class RiskMetricsTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create metrics with all fields")
        void shouldCreateMetricsWithAllFields() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .volatility(BigDecimal.valueOf(18.5))
                    .downsideDeviation(BigDecimal.valueOf(12.3))
                    .beta(BigDecimal.valueOf(1.1))
                    .alpha(BigDecimal.valueOf(2.5))
                    .var95(BigDecimal.valueOf(3.2))
                    .cvar95(BigDecimal.valueOf(4.5))
                    .calmarRatio(BigDecimal.valueOf(1.8))
                    .informationRatio(BigDecimal.valueOf(0.9))
                    .ulcerIndex(BigDecimal.valueOf(5.2))
                    .recoveryFactor(BigDecimal.valueOf(3.5))
                    .riskOfRuin(BigDecimal.valueOf(0.001))
                    .omegaRatio(BigDecimal.valueOf(1.5))
                    .skewness(BigDecimal.valueOf(0.3))
                    .kurtosis(BigDecimal.valueOf(3.5))
                    .excessKurtosis(BigDecimal.valueOf(0.5))
                    .kellyFraction(BigDecimal.valueOf(0.25))
                    .halfKelly(BigDecimal.valueOf(0.125))
                    .tailRatio(BigDecimal.valueOf(1.2))
                    .gainToPainRatio(BigDecimal.valueOf(2.0))
                    .build();

            assertThat(metrics.getVolatility()).isEqualTo(BigDecimal.valueOf(18.5));
            assertThat(metrics.getBeta()).isEqualTo(BigDecimal.valueOf(1.1));
            assertThat(metrics.getVar95()).isEqualTo(BigDecimal.valueOf(3.2));
            assertThat(metrics.getKellyFraction()).isEqualTo(BigDecimal.valueOf(0.25));
        }
    }

    @Nested
    @DisplayName("Volatility Metrics Tests")
    class VolatilityMetricsTests {

        @Test
        @DisplayName("Should store volatility metrics")
        void shouldStoreVolatilityMetrics() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .volatility(BigDecimal.valueOf(20))
                    .downsideDeviation(BigDecimal.valueOf(15))
                    .build();

            assertThat(metrics.getVolatility()).isEqualTo(BigDecimal.valueOf(20));
            assertThat(metrics.getDownsideDeviation()).isEqualTo(BigDecimal.valueOf(15));
        }
    }

    @Nested
    @DisplayName("Market Risk Metrics Tests")
    class MarketRiskMetricsTests {

        @Test
        @DisplayName("Should store beta and alpha")
        void shouldStoreBetaAndAlpha() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .beta(BigDecimal.valueOf(1.2))
                    .alpha(BigDecimal.valueOf(3.0))
                    .build();

            assertThat(metrics.getBeta()).isEqualTo(BigDecimal.valueOf(1.2));
            assertThat(metrics.getAlpha()).isEqualTo(BigDecimal.valueOf(3.0));
        }
    }

    @Nested
    @DisplayName("Value at Risk Tests")
    class ValueAtRiskTests {

        @Test
        @DisplayName("Should store VaR metrics")
        void shouldStoreVarMetrics() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .var95(BigDecimal.valueOf(5.0))
                    .cvar95(BigDecimal.valueOf(7.5))
                    .build();

            assertThat(metrics.getVar95()).isEqualTo(BigDecimal.valueOf(5.0));
            assertThat(metrics.getCvar95()).isEqualTo(BigDecimal.valueOf(7.5));
        }
    }

    @Nested
    @DisplayName("Risk-Adjusted Return Metrics Tests")
    class RiskAdjustedReturnMetricsTests {

        @Test
        @DisplayName("Should store risk-adjusted metrics")
        void shouldStoreRiskAdjustedMetrics() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .calmarRatio(BigDecimal.valueOf(2.0))
                    .informationRatio(BigDecimal.valueOf(1.2))
                    .build();

            assertThat(metrics.getCalmarRatio()).isEqualTo(BigDecimal.valueOf(2.0));
            assertThat(metrics.getInformationRatio()).isEqualTo(BigDecimal.valueOf(1.2));
        }
    }

    @Nested
    @DisplayName("Additional Risk Metrics Tests")
    class AdditionalRiskMetricsTests {

        @Test
        @DisplayName("Should store ulcer index and recovery factor")
        void shouldStoreUlcerIndexAndRecoveryFactor() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .ulcerIndex(BigDecimal.valueOf(4.5))
                    .recoveryFactor(BigDecimal.valueOf(4.0))
                    .riskOfRuin(BigDecimal.valueOf(0.0001))
                    .build();

            assertThat(metrics.getUlcerIndex()).isEqualTo(BigDecimal.valueOf(4.5));
            assertThat(metrics.getRecoveryFactor()).isEqualTo(BigDecimal.valueOf(4.0));
            assertThat(metrics.getRiskOfRuin()).isEqualTo(BigDecimal.valueOf(0.0001));
        }
    }

    @Nested
    @DisplayName("Advanced Risk Metrics Tests")
    class AdvancedRiskMetricsTests {

        @Test
        @DisplayName("Should store distribution metrics")
        void shouldStoreDistributionMetrics() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .skewness(BigDecimal.valueOf(0.5))
                    .kurtosis(BigDecimal.valueOf(4.0))
                    .excessKurtosis(BigDecimal.valueOf(1.0))
                    .build();

            assertThat(metrics.getSkewness()).isEqualTo(BigDecimal.valueOf(0.5));
            assertThat(metrics.getKurtosis()).isEqualTo(BigDecimal.valueOf(4.0));
            assertThat(metrics.getExcessKurtosis()).isEqualTo(BigDecimal.valueOf(1.0));
        }

        @Test
        @DisplayName("Should store kelly criterion metrics")
        void shouldStoreKellyCriterionMetrics() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .kellyFraction(BigDecimal.valueOf(0.30))
                    .halfKelly(BigDecimal.valueOf(0.15))
                    .build();

            assertThat(metrics.getKellyFraction()).isEqualTo(BigDecimal.valueOf(0.30));
            assertThat(metrics.getHalfKelly()).isEqualTo(BigDecimal.valueOf(0.15));
        }

        @Test
        @DisplayName("Should store omega and gain-to-pain ratios")
        void shouldStoreOmegaAndGainToPainRatios() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .omegaRatio(BigDecimal.valueOf(1.8))
                    .tailRatio(BigDecimal.valueOf(1.5))
                    .gainToPainRatio(BigDecimal.valueOf(2.5))
                    .build();

            assertThat(metrics.getOmegaRatio()).isEqualTo(BigDecimal.valueOf(1.8));
            assertThat(metrics.getTailRatio()).isEqualTo(BigDecimal.valueOf(1.5));
            assertThat(metrics.getGainToPainRatio()).isEqualTo(BigDecimal.valueOf(2.5));
        }
    }
}
