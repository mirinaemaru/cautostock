package maru.trading.domain.backtest.optimization;

import maru.trading.domain.backtest.BacktestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptimizationConfig Test")
class OptimizationConfigTest {

    @Nested
    @DisplayName("Enum Tests")
    class EnumTests {

        @Test
        @DisplayName("Should have all optimization methods")
        void shouldHaveAllOptimizationMethods() {
            assertThat(OptimizationConfig.OptimizationMethod.values()).hasSize(3);
            assertThat(OptimizationConfig.OptimizationMethod.GRID_SEARCH).isNotNull();
            assertThat(OptimizationConfig.OptimizationMethod.RANDOM_SEARCH).isNotNull();
            assertThat(OptimizationConfig.OptimizationMethod.BAYESIAN).isNotNull();
        }

        @Test
        @DisplayName("Should have all optimization objectives")
        void shouldHaveAllOptimizationObjectives() {
            assertThat(OptimizationConfig.OptimizationObjective.values()).hasSize(5);
            assertThat(OptimizationConfig.OptimizationObjective.TOTAL_RETURN).isNotNull();
            assertThat(OptimizationConfig.OptimizationObjective.SHARPE_RATIO).isNotNull();
            assertThat(OptimizationConfig.OptimizationObjective.SORTINO_RATIO).isNotNull();
            assertThat(OptimizationConfig.OptimizationObjective.PROFIT_FACTOR).isNotNull();
            assertThat(OptimizationConfig.OptimizationObjective.CALMAR_RATIO).isNotNull();
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            OptimizationConfig config = OptimizationConfig.builder()
                    .optimizationId("OPT001")
                    .build();

            assertThat(config.getMethod())
                    .isEqualTo(OptimizationConfig.OptimizationMethod.GRID_SEARCH);
            assertThat(config.getObjective())
                    .isEqualTo(OptimizationConfig.OptimizationObjective.SHARPE_RATIO);
            assertThat(config.getMaxRuns()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create config with all fields")
        void shouldCreateConfigWithAllFields() {
            BacktestConfig baseConfig = BacktestConfig.builder()
                    .backtestId("BT001")
                    .build();

            Map<String, List<Object>> paramRanges = Map.of(
                    "shortPeriod", List.of(5, 10, 15, 20),
                    "longPeriod", List.of(20, 30, 50, 100)
            );

            OptimizationConfig config = OptimizationConfig.builder()
                    .optimizationId("OPT001")
                    .baseConfig(baseConfig)
                    .parameterRanges(paramRanges)
                    .method(OptimizationConfig.OptimizationMethod.RANDOM_SEARCH)
                    .objective(OptimizationConfig.OptimizationObjective.CALMAR_RATIO)
                    .maxRuns(500)
                    .build();

            assertThat(config.getOptimizationId()).isEqualTo("OPT001");
            assertThat(config.getBaseConfig()).isEqualTo(baseConfig);
            assertThat(config.getParameterRanges()).hasSize(2);
            assertThat(config.getMethod())
                    .isEqualTo(OptimizationConfig.OptimizationMethod.RANDOM_SEARCH);
            assertThat(config.getObjective())
                    .isEqualTo(OptimizationConfig.OptimizationObjective.CALMAR_RATIO);
            assertThat(config.getMaxRuns()).isEqualTo(500);
        }
    }
}
