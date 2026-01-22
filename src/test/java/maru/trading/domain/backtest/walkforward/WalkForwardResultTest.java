package maru.trading.domain.backtest.walkforward;

import maru.trading.domain.backtest.BacktestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WalkForwardResult Test")
class WalkForwardResultTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create result with builder")
        void shouldCreateResultWithBuilder() {
            LocalDateTime startTime = LocalDateTime.now().minusHours(5);
            LocalDateTime endTime = LocalDateTime.now();

            WalkForwardResult result = WalkForwardResult.builder()
                    .walkForwardId("WF001")
                    .combinedOutOfSampleReturn(BigDecimal.valueOf(25))
                    .avgOutOfSampleSharpeRatio(BigDecimal.valueOf(1.5))
                    .stabilityScore(BigDecimal.valueOf(0.85))
                    .totalWindows(10)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(18000000)
                    .build();

            assertThat(result.getWalkForwardId()).isEqualTo("WF001");
            assertThat(result.getCombinedOutOfSampleReturn()).isEqualTo(BigDecimal.valueOf(25));
            assertThat(result.getAvgOutOfSampleSharpeRatio()).isEqualTo(BigDecimal.valueOf(1.5));
            assertThat(result.getStabilityScore()).isEqualTo(BigDecimal.valueOf(0.85));
            assertThat(result.getTotalWindows()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("WalkForwardWindow Tests")
    class WalkForwardWindowTests {

        @Test
        @DisplayName("Should create window with all fields")
        void shouldCreateWindowWithAllFields() {
            BacktestResult inSampleResult = BacktestResult.builder()
                    .backtestId("BT_IN_1")
                    .build();

            BacktestResult outOfSampleResult = BacktestResult.builder()
                    .backtestId("BT_OUT_1")
                    .build();

            WalkForwardResult.WalkForwardWindow window = WalkForwardResult.WalkForwardWindow.builder()
                    .windowNumber(1)
                    .inSampleStart(LocalDate.of(2023, 1, 1))
                    .inSampleEnd(LocalDate.of(2023, 6, 30))
                    .outOfSampleStart(LocalDate.of(2023, 7, 1))
                    .outOfSampleEnd(LocalDate.of(2023, 9, 30))
                    .optimizedParameters(Map.of("period", 20))
                    .inSampleResult(inSampleResult)
                    .outOfSampleResult(outOfSampleResult)
                    .inSampleMetric(BigDecimal.valueOf(2.0))
                    .outOfSampleMetric(BigDecimal.valueOf(1.5))
                    .performanceDegradation(BigDecimal.valueOf(0.5))
                    .build();

            assertThat(window.getWindowNumber()).isEqualTo(1);
            assertThat(window.getInSampleStart()).isEqualTo(LocalDate.of(2023, 1, 1));
            assertThat(window.getOutOfSampleEnd()).isEqualTo(LocalDate.of(2023, 9, 30));
            assertThat(window.getOptimizedParameters()).containsEntry("period", 20);
            assertThat(window.getPerformanceDegradation()).isEqualTo(BigDecimal.valueOf(0.5));
        }
    }

    @Nested
    @DisplayName("Windows Collection Tests")
    class WindowsCollectionTests {

        @Test
        @DisplayName("Should store multiple windows")
        void shouldStoreMultipleWindows() {
            List<WalkForwardResult.WalkForwardWindow> windows = List.of(
                    WalkForwardResult.WalkForwardWindow.builder()
                            .windowNumber(1)
                            .outOfSampleMetric(BigDecimal.valueOf(1.5))
                            .build(),
                    WalkForwardResult.WalkForwardWindow.builder()
                            .windowNumber(2)
                            .outOfSampleMetric(BigDecimal.valueOf(1.8))
                            .build(),
                    WalkForwardResult.WalkForwardWindow.builder()
                            .windowNumber(3)
                            .outOfSampleMetric(BigDecimal.valueOf(1.2))
                            .build()
            );

            WalkForwardResult result = WalkForwardResult.builder()
                    .walkForwardId("WF001")
                    .windows(windows)
                    .totalWindows(3)
                    .build();

            assertThat(result.getWindows()).hasSize(3);
            assertThat(result.getTotalWindows()).isEqualTo(3);
        }
    }
}
