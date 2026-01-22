package maru.trading.domain.backtest.montecarlo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonteCarloResult Test")
class MonteCarloResultTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create result with builder")
        void shouldCreateResultWithBuilder() {
            LocalDateTime startTime = LocalDateTime.now().minusMinutes(30);
            LocalDateTime endTime = LocalDateTime.now();

            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .numSimulations(1000)
                    .meanReturn(BigDecimal.valueOf(15.5))
                    .medianReturn(BigDecimal.valueOf(14.0))
                    .stdDevReturn(BigDecimal.valueOf(8.5))
                    .minReturn(BigDecimal.valueOf(-25))
                    .maxReturn(BigDecimal.valueOf(55))
                    .valueAtRisk(BigDecimal.valueOf(10))
                    .conditionalVaR(BigDecimal.valueOf(15))
                    .probabilityOfProfit(BigDecimal.valueOf(0.72))
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(1800000)
                    .build();

            assertThat(result.getSimulationId()).isEqualTo("MC001");
            assertThat(result.getNumSimulations()).isEqualTo(1000);
            assertThat(result.getMeanReturn()).isEqualTo(BigDecimal.valueOf(15.5));
            assertThat(result.getMedianReturn()).isEqualTo(BigDecimal.valueOf(14.0));
            assertThat(result.getProbabilityOfProfit()).isEqualTo(BigDecimal.valueOf(0.72));
        }
    }

    @Nested
    @DisplayName("DrawdownStatistics Tests")
    class DrawdownStatisticsTests {

        @Test
        @DisplayName("Should create drawdown statistics")
        void shouldCreateDrawdownStatistics() {
            MonteCarloResult.DrawdownStatistics stats = MonteCarloResult.DrawdownStatistics.builder()
                    .meanMaxDrawdown(BigDecimal.valueOf(12))
                    .medianMaxDrawdown(BigDecimal.valueOf(10))
                    .stdDevMaxDrawdown(BigDecimal.valueOf(5))
                    .worstMaxDrawdown(BigDecimal.valueOf(35))
                    .bestMaxDrawdown(BigDecimal.valueOf(3))
                    .build();

            assertThat(stats.getMeanMaxDrawdown()).isEqualTo(BigDecimal.valueOf(12));
            assertThat(stats.getMedianMaxDrawdown()).isEqualTo(BigDecimal.valueOf(10));
            assertThat(stats.getWorstMaxDrawdown()).isEqualTo(BigDecimal.valueOf(35));
            assertThat(stats.getBestMaxDrawdown()).isEqualTo(BigDecimal.valueOf(3));
        }

        @Test
        @DisplayName("Should associate drawdown stats with result")
        void shouldAssociateDrawdownStatsWithResult() {
            MonteCarloResult.DrawdownStatistics stats = MonteCarloResult.DrawdownStatistics.builder()
                    .meanMaxDrawdown(BigDecimal.valueOf(10))
                    .build();

            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .maxDrawdownStats(stats)
                    .build();

            assertThat(result.getMaxDrawdownStats()).isNotNull();
            assertThat(result.getMaxDrawdownStats().getMeanMaxDrawdown())
                    .isEqualTo(BigDecimal.valueOf(10));
        }
    }

    @Nested
    @DisplayName("DistributionBin Tests")
    class DistributionBinTests {

        @Test
        @DisplayName("Should create distribution bin")
        void shouldCreateDistributionBin() {
            MonteCarloResult.DistributionBin bin = MonteCarloResult.DistributionBin.builder()
                    .binStart(BigDecimal.valueOf(10))
                    .binEnd(BigDecimal.valueOf(15))
                    .binCenter(BigDecimal.valueOf(12.5))
                    .count(50)
                    .frequency(BigDecimal.valueOf(0.05))
                    .build();

            assertThat(bin.getBinStart()).isEqualTo(BigDecimal.valueOf(10));
            assertThat(bin.getBinEnd()).isEqualTo(BigDecimal.valueOf(15));
            assertThat(bin.getBinCenter()).isEqualTo(BigDecimal.valueOf(12.5));
            assertThat(bin.getCount()).isEqualTo(50);
            assertThat(bin.getFrequency()).isEqualTo(BigDecimal.valueOf(0.05));
        }

        @Test
        @DisplayName("Should store distribution in result")
        void shouldStoreDistributionInResult() {
            List<MonteCarloResult.DistributionBin> distribution = List.of(
                    MonteCarloResult.DistributionBin.builder()
                            .binCenter(BigDecimal.valueOf(-10))
                            .count(100)
                            .build(),
                    MonteCarloResult.DistributionBin.builder()
                            .binCenter(BigDecimal.valueOf(0))
                            .count(200)
                            .build(),
                    MonteCarloResult.DistributionBin.builder()
                            .binCenter(BigDecimal.valueOf(10))
                            .count(300)
                            .build()
            );

            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .returnDistribution(distribution)
                    .build();

            assertThat(result.getReturnDistribution()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("SimulationPath Tests")
    class SimulationPathTests {

        @Test
        @DisplayName("Should create simulation path")
        void shouldCreateSimulationPath() {
            List<BigDecimal> curve = List.of(
                    BigDecimal.valueOf(100000),
                    BigDecimal.valueOf(105000),
                    BigDecimal.valueOf(110000)
            );

            MonteCarloResult.SimulationPath path = MonteCarloResult.SimulationPath.builder()
                    .simulationNumber(1)
                    .totalReturn(BigDecimal.valueOf(10))
                    .maxDrawdown(BigDecimal.valueOf(5))
                    .finalEquity(BigDecimal.valueOf(110000))
                    .equityCurve(curve)
                    .build();

            assertThat(path.getSimulationNumber()).isEqualTo(1);
            assertThat(path.getTotalReturn()).isEqualTo(BigDecimal.valueOf(10));
            assertThat(path.getMaxDrawdown()).isEqualTo(BigDecimal.valueOf(5));
            assertThat(path.getFinalEquity()).isEqualTo(BigDecimal.valueOf(110000));
            assertThat(path.getEquityCurve()).hasSize(3);
        }

        @Test
        @DisplayName("Should store best/worst/median cases")
        void shouldStoreBestWorstMedianCases() {
            MonteCarloResult.SimulationPath bestCase = MonteCarloResult.SimulationPath.builder()
                    .simulationNumber(100)
                    .totalReturn(BigDecimal.valueOf(50))
                    .build();

            MonteCarloResult.SimulationPath worstCase = MonteCarloResult.SimulationPath.builder()
                    .simulationNumber(500)
                    .totalReturn(BigDecimal.valueOf(-20))
                    .build();

            MonteCarloResult.SimulationPath medianCase = MonteCarloResult.SimulationPath.builder()
                    .simulationNumber(300)
                    .totalReturn(BigDecimal.valueOf(15))
                    .build();

            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .bestCase(bestCase)
                    .worstCase(worstCase)
                    .medianCase(medianCase)
                    .build();

            assertThat(result.getBestCase().getTotalReturn()).isEqualTo(BigDecimal.valueOf(50));
            assertThat(result.getWorstCase().getTotalReturn()).isEqualTo(BigDecimal.valueOf(-20));
            assertThat(result.getMedianCase().getTotalReturn()).isEqualTo(BigDecimal.valueOf(15));
        }
    }

    @Nested
    @DisplayName("Percentiles Tests")
    class PercentilesTests {

        @Test
        @DisplayName("Should store return percentiles")
        void shouldStoreReturnPercentiles() {
            Map<Integer, BigDecimal> percentiles = Map.of(
                    5, BigDecimal.valueOf(-15),
                    25, BigDecimal.valueOf(5),
                    50, BigDecimal.valueOf(15),
                    75, BigDecimal.valueOf(25),
                    95, BigDecimal.valueOf(40)
            );

            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .returnPercentiles(percentiles)
                    .build();

            assertThat(result.getReturnPercentiles()).hasSize(5);
            assertThat(result.getReturnPercentiles().get(50)).isEqualTo(BigDecimal.valueOf(15));
        }
    }

    @Nested
    @DisplayName("Confidence Intervals Tests")
    class ConfidenceIntervalsTests {

        @Test
        @DisplayName("Should store confidence intervals")
        void shouldStoreConfidenceIntervals() {
            BigDecimal[] ci95 = {BigDecimal.valueOf(10), BigDecimal.valueOf(20)};
            BigDecimal[] ci99 = {BigDecimal.valueOf(5), BigDecimal.valueOf(25)};

            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .returnConfidenceInterval95(ci95)
                    .returnConfidenceInterval99(ci99)
                    .build();

            assertThat(result.getReturnConfidenceInterval95()).hasSize(2);
            assertThat(result.getReturnConfidenceInterval95()[0]).isEqualTo(BigDecimal.valueOf(10));
            assertThat(result.getReturnConfidenceInterval99()[1]).isEqualTo(BigDecimal.valueOf(25));
        }
    }

    @Nested
    @DisplayName("Probability Metrics Tests")
    class ProbabilityMetricsTests {

        @Test
        @DisplayName("Should store probability metrics")
        void shouldStoreProbabilityMetrics() {
            MonteCarloResult result = MonteCarloResult.builder()
                    .simulationId("MC001")
                    .probabilityOfProfit(BigDecimal.valueOf(0.75))
                    .probabilityOfTargetReturn(BigDecimal.valueOf(0.60))
                    .targetReturn(BigDecimal.valueOf(20))
                    .probabilityOfRuin(BigDecimal.valueOf(0.02))
                    .ruinThreshold(BigDecimal.valueOf(50))
                    .build();

            assertThat(result.getProbabilityOfProfit()).isEqualTo(BigDecimal.valueOf(0.75));
            assertThat(result.getProbabilityOfTargetReturn()).isEqualTo(BigDecimal.valueOf(0.60));
            assertThat(result.getTargetReturn()).isEqualTo(BigDecimal.valueOf(20));
            assertThat(result.getProbabilityOfRuin()).isEqualTo(BigDecimal.valueOf(0.02));
            assertThat(result.getRuinThreshold()).isEqualTo(BigDecimal.valueOf(50));
        }
    }
}
