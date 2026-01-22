package maru.trading.domain.backtest.optimization;

import maru.trading.domain.backtest.BacktestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptimizationResult Test")
class OptimizationResultTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create result with builder")
        void shouldCreateResultWithBuilder() {
            LocalDateTime startTime = LocalDateTime.now().minusHours(2);
            LocalDateTime endTime = LocalDateTime.now();

            OptimizationResult result = OptimizationResult.builder()
                    .optimizationId("OPT001")
                    .bestParameters(Map.of("shortPeriod", 10, "longPeriod", 50))
                    .bestObjectiveValue(BigDecimal.valueOf(2.5))
                    .totalRuns(100)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(7200000)
                    .build();

            assertThat(result.getOptimizationId()).isEqualTo("OPT001");
            assertThat(result.getBestParameters())
                    .containsEntry("shortPeriod", 10)
                    .containsEntry("longPeriod", 50);
            assertThat(result.getBestObjectiveValue()).isEqualTo(BigDecimal.valueOf(2.5));
            assertThat(result.getTotalRuns()).isEqualTo(100);
            assertThat(result.getDurationMs()).isEqualTo(7200000);
        }
    }

    @Nested
    @DisplayName("OptimizationRun Tests")
    class OptimizationRunTests {

        @Test
        @DisplayName("Should create optimization run")
        void shouldCreateOptimizationRun() {
            BacktestResult backtestResult = BacktestResult.builder()
                    .backtestId("BT001")
                    .build();

            OptimizationResult.OptimizationRun run = OptimizationResult.OptimizationRun.builder()
                    .parameters(Map.of("period", 20))
                    .backtestResult(backtestResult)
                    .objectiveValue(BigDecimal.valueOf(1.8))
                    .runNumber(5)
                    .build();

            assertThat(run.getParameters()).containsEntry("period", 20);
            assertThat(run.getBacktestResult()).isNotNull();
            assertThat(run.getObjectiveValue()).isEqualTo(BigDecimal.valueOf(1.8));
            assertThat(run.getRunNumber()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("All Runs Tests")
    class AllRunsTests {

        @Test
        @DisplayName("Should store all runs")
        void shouldStoreAllRuns() {
            List<OptimizationResult.OptimizationRun> runs = List.of(
                    OptimizationResult.OptimizationRun.builder()
                            .runNumber(1)
                            .objectiveValue(BigDecimal.valueOf(1.5))
                            .build(),
                    OptimizationResult.OptimizationRun.builder()
                            .runNumber(2)
                            .objectiveValue(BigDecimal.valueOf(2.0))
                            .build()
            );

            OptimizationResult result = OptimizationResult.builder()
                    .optimizationId("OPT001")
                    .allRuns(runs)
                    .totalRuns(2)
                    .build();

            assertThat(result.getAllRuns()).hasSize(2);
            assertThat(result.getAllRuns().get(0).getRunNumber()).isEqualTo(1);
        }
    }
}
