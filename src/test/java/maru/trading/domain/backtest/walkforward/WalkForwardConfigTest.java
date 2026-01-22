package maru.trading.domain.backtest.walkforward;

import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.optimization.OptimizationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WalkForwardConfig Test")
class WalkForwardConfigTest {

    @Nested
    @DisplayName("Mode Enum Tests")
    class ModeEnumTests {

        @Test
        @DisplayName("Should have all modes")
        void shouldHaveAllModes() {
            assertThat(WalkForwardConfig.WalkForwardMode.values()).hasSize(2);
            assertThat(WalkForwardConfig.WalkForwardMode.ROLLING).isNotNull();
            assertThat(WalkForwardConfig.WalkForwardMode.ANCHORED).isNotNull();
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            WalkForwardConfig config = WalkForwardConfig.builder()
                    .walkForwardId("WF001")
                    .build();

            assertThat(config.getInSampleDays()).isEqualTo(180);
            assertThat(config.getOutOfSampleDays()).isEqualTo(90);
            assertThat(config.getStepDays()).isEqualTo(30);
            assertThat(config.getMinWindows()).isEqualTo(3);
            assertThat(config.getMode()).isEqualTo(WalkForwardConfig.WalkForwardMode.ROLLING);
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

            OptimizationConfig optConfig = OptimizationConfig.builder()
                    .optimizationId("OPT001")
                    .build();

            WalkForwardConfig config = WalkForwardConfig.builder()
                    .walkForwardId("WF001")
                    .baseConfig(baseConfig)
                    .optimizationConfig(optConfig)
                    .analysisStartDate(LocalDate.of(2020, 1, 1))
                    .analysisEndDate(LocalDate.of(2024, 12, 31))
                    .inSampleDays(250)
                    .outOfSampleDays(60)
                    .stepDays(60)
                    .minWindows(5)
                    .mode(WalkForwardConfig.WalkForwardMode.ANCHORED)
                    .build();

            assertThat(config.getWalkForwardId()).isEqualTo("WF001");
            assertThat(config.getBaseConfig()).isEqualTo(baseConfig);
            assertThat(config.getOptimizationConfig()).isEqualTo(optConfig);
            assertThat(config.getAnalysisStartDate()).isEqualTo(LocalDate.of(2020, 1, 1));
            assertThat(config.getAnalysisEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
            assertThat(config.getInSampleDays()).isEqualTo(250);
            assertThat(config.getOutOfSampleDays()).isEqualTo(60);
            assertThat(config.getStepDays()).isEqualTo(60);
            assertThat(config.getMinWindows()).isEqualTo(5);
            assertThat(config.getMode()).isEqualTo(WalkForwardConfig.WalkForwardMode.ANCHORED);
        }
    }
}
