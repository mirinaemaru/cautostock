package maru.trading.domain.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerformanceMetrics Test")
class PerformanceMetricsTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create metrics with all fields")
        void shouldCreateMetricsWithAllFields() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .totalReturn(BigDecimal.valueOf(25.5))
                    .annualReturn(BigDecimal.valueOf(12.3))
                    .sharpeRatio(BigDecimal.valueOf(1.5))
                    .sortinoRatio(BigDecimal.valueOf(2.0))
                    .maxDrawdown(BigDecimal.valueOf(10.2))
                    .maxDrawdownDuration(45)
                    .totalTrades(100)
                    .winningTrades(55)
                    .losingTrades(45)
                    .winRate(BigDecimal.valueOf(55))
                    .profitFactor(BigDecimal.valueOf(1.8))
                    .avgWin(BigDecimal.valueOf(50000))
                    .avgLoss(BigDecimal.valueOf(30000))
                    .avgTrade(BigDecimal.valueOf(15000))
                    .largestWin(BigDecimal.valueOf(200000))
                    .largestLoss(BigDecimal.valueOf(80000))
                    .totalProfit(BigDecimal.valueOf(2750000))
                    .totalLoss(BigDecimal.valueOf(1350000))
                    .maxConsecutiveWins(8)
                    .maxConsecutiveLosses(5)
                    .build();

            assertThat(metrics.getTotalReturn()).isEqualTo(BigDecimal.valueOf(25.5));
            assertThat(metrics.getSharpeRatio()).isEqualTo(BigDecimal.valueOf(1.5));
            assertThat(metrics.getTotalTrades()).isEqualTo(100);
            assertThat(metrics.getWinRate()).isEqualTo(BigDecimal.valueOf(55));
            assertThat(metrics.getMaxConsecutiveWins()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Return Metrics Tests")
    class ReturnMetricsTests {

        @Test
        @DisplayName("Should store return metrics")
        void shouldStoreReturnMetrics() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .totalReturn(BigDecimal.valueOf(30))
                    .annualReturn(BigDecimal.valueOf(15))
                    .sharpeRatio(BigDecimal.valueOf(2.0))
                    .sortinoRatio(BigDecimal.valueOf(2.5))
                    .build();

            assertThat(metrics.getTotalReturn()).isEqualTo(BigDecimal.valueOf(30));
            assertThat(metrics.getAnnualReturn()).isEqualTo(BigDecimal.valueOf(15));
            assertThat(metrics.getSharpeRatio()).isEqualTo(BigDecimal.valueOf(2.0));
            assertThat(metrics.getSortinoRatio()).isEqualTo(BigDecimal.valueOf(2.5));
        }
    }

    @Nested
    @DisplayName("Drawdown Metrics Tests")
    class DrawdownMetricsTests {

        @Test
        @DisplayName("Should store drawdown metrics")
        void shouldStoreDrawdownMetrics() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .maxDrawdown(BigDecimal.valueOf(15.5))
                    .maxDrawdownDuration(60)
                    .build();

            assertThat(metrics.getMaxDrawdown()).isEqualTo(BigDecimal.valueOf(15.5));
            assertThat(metrics.getMaxDrawdownDuration()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("Trade Statistics Tests")
    class TradeStatisticsTests {

        @Test
        @DisplayName("Should store trade statistics")
        void shouldStoreTradeStatistics() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .totalTrades(200)
                    .winningTrades(120)
                    .losingTrades(80)
                    .winRate(BigDecimal.valueOf(60))
                    .profitFactor(BigDecimal.valueOf(2.1))
                    .build();

            assertThat(metrics.getTotalTrades()).isEqualTo(200);
            assertThat(metrics.getWinningTrades()).isEqualTo(120);
            assertThat(metrics.getLosingTrades()).isEqualTo(80);
            assertThat(metrics.getWinRate()).isEqualTo(BigDecimal.valueOf(60));
            assertThat(metrics.getProfitFactor()).isEqualTo(BigDecimal.valueOf(2.1));
        }

        @Test
        @DisplayName("Should store average trade metrics")
        void shouldStoreAverageTradeMetrics() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .avgWin(BigDecimal.valueOf(100000))
                    .avgLoss(BigDecimal.valueOf(50000))
                    .avgTrade(BigDecimal.valueOf(35000))
                    .largestWin(BigDecimal.valueOf(500000))
                    .largestLoss(BigDecimal.valueOf(150000))
                    .build();

            assertThat(metrics.getAvgWin()).isEqualTo(BigDecimal.valueOf(100000));
            assertThat(metrics.getAvgLoss()).isEqualTo(BigDecimal.valueOf(50000));
            assertThat(metrics.getAvgTrade()).isEqualTo(BigDecimal.valueOf(35000));
            assertThat(metrics.getLargestWin()).isEqualTo(BigDecimal.valueOf(500000));
            assertThat(metrics.getLargestLoss()).isEqualTo(BigDecimal.valueOf(150000));
        }
    }

    @Nested
    @DisplayName("Additional Metrics Tests")
    class AdditionalMetricsTests {

        @Test
        @DisplayName("Should store streak metrics")
        void shouldStoreStreakMetrics() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .maxConsecutiveWins(12)
                    .maxConsecutiveLosses(7)
                    .build();

            assertThat(metrics.getMaxConsecutiveWins()).isEqualTo(12);
            assertThat(metrics.getMaxConsecutiveLosses()).isEqualTo(7);
        }

        @Test
        @DisplayName("Should store profit/loss totals")
        void shouldStoreProfitLossTotals() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .totalProfit(BigDecimal.valueOf(5000000))
                    .totalLoss(BigDecimal.valueOf(2000000))
                    .build();

            assertThat(metrics.getTotalProfit()).isEqualTo(BigDecimal.valueOf(5000000));
            assertThat(metrics.getTotalLoss()).isEqualTo(BigDecimal.valueOf(2000000));
        }
    }
}
