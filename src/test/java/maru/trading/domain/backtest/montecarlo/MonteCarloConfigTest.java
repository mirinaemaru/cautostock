package maru.trading.domain.backtest.montecarlo;

import maru.trading.domain.backtest.BacktestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonteCarloConfig Test")
class MonteCarloConfigTest {

    @Nested
    @DisplayName("SimulationMethod Enum Tests")
    class SimulationMethodEnumTests {

        @Test
        @DisplayName("Should have all simulation methods")
        void shouldHaveAllSimulationMethods() {
            assertThat(MonteCarloConfig.SimulationMethod.values()).hasSize(3);
            assertThat(MonteCarloConfig.SimulationMethod.BOOTSTRAP).isNotNull();
            assertThat(MonteCarloConfig.SimulationMethod.PERMUTATION).isNotNull();
            assertThat(MonteCarloConfig.SimulationMethod.PARAMETRIC).isNotNull();
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            MonteCarloConfig config = MonteCarloConfig.builder()
                    .simulationId("MC001")
                    .build();

            assertThat(config.getNumSimulations()).isEqualTo(1000);
            assertThat(config.getMethod())
                    .isEqualTo(MonteCarloConfig.SimulationMethod.BOOTSTRAP);
            assertThat(config.getConfidenceLevel()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(config.isPreserveCorrelation()).isFalse();
            assertThat(config.getBlockSize()).isEqualTo(5);
            assertThat(config.getDistributionBins()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create config with all fields")
        void shouldCreateConfigWithAllFields() {
            BacktestResult baseResult = BacktestResult.builder()
                    .backtestId("BT001")
                    .build();

            MonteCarloConfig config = MonteCarloConfig.builder()
                    .simulationId("MC001")
                    .baseBacktestResult(baseResult)
                    .numSimulations(5000)
                    .method(MonteCarloConfig.SimulationMethod.PARAMETRIC)
                    .confidenceLevel(new BigDecimal("0.99"))
                    .preserveCorrelation(true)
                    .blockSize(10)
                    .randomSeed(12345L)
                    .distributionBins(100)
                    .build();

            assertThat(config.getSimulationId()).isEqualTo("MC001");
            assertThat(config.getBaseBacktestResult()).isNotNull();
            assertThat(config.getNumSimulations()).isEqualTo(5000);
            assertThat(config.getMethod())
                    .isEqualTo(MonteCarloConfig.SimulationMethod.PARAMETRIC);
            assertThat(config.getConfidenceLevel()).isEqualByComparingTo(new BigDecimal("0.99"));
            assertThat(config.isPreserveCorrelation()).isTrue();
            assertThat(config.getBlockSize()).isEqualTo(10);
            assertThat(config.getRandomSeed()).isEqualTo(12345L);
            assertThat(config.getDistributionBins()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should allow null random seed for random behavior")
        void shouldAllowNullRandomSeedForRandomBehavior() {
            MonteCarloConfig config = MonteCarloConfig.builder()
                    .simulationId("MC001")
                    .randomSeed(null)
                    .build();

            assertThat(config.getRandomSeed()).isNull();
        }
    }

    @Nested
    @DisplayName("Correlation Preservation Tests")
    class CorrelationPreservationTests {

        @Test
        @DisplayName("Should configure block bootstrap")
        void shouldConfigureBlockBootstrap() {
            MonteCarloConfig config = MonteCarloConfig.builder()
                    .simulationId("MC001")
                    .method(MonteCarloConfig.SimulationMethod.BOOTSTRAP)
                    .preserveCorrelation(true)
                    .blockSize(20)
                    .build();

            assertThat(config.isPreserveCorrelation()).isTrue();
            assertThat(config.getBlockSize()).isEqualTo(20);
        }
    }
}
