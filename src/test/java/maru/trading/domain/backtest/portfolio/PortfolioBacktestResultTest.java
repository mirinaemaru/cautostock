package maru.trading.domain.backtest.portfolio;

import maru.trading.domain.backtest.BacktestResult;
import maru.trading.domain.backtest.PerformanceMetrics;
import maru.trading.domain.backtest.RiskMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioBacktestResult Test")
class PortfolioBacktestResultTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create result with builder")
        void shouldCreateResultWithBuilder() {
            LocalDateTime startTime = LocalDateTime.now().minusHours(3);
            LocalDateTime endTime = LocalDateTime.now();

            PortfolioBacktestResult result = PortfolioBacktestResult.builder()
                    .portfolioBacktestId("PB001")
                    .finalCapital(BigDecimal.valueOf(120_000_000))
                    .totalReturn(BigDecimal.valueOf(20))
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(10800000)
                    .build();

            assertThat(result.getPortfolioBacktestId()).isEqualTo("PB001");
            assertThat(result.getFinalCapital()).isEqualTo(BigDecimal.valueOf(120_000_000));
            assertThat(result.getTotalReturn()).isEqualTo(BigDecimal.valueOf(20));
            assertThat(result.getDurationMs()).isEqualTo(10800000);
        }
    }

    @Nested
    @DisplayName("Metrics Association Tests")
    class MetricsAssociationTests {

        @Test
        @DisplayName("Should associate portfolio metrics")
        void shouldAssociatePortfolioMetrics() {
            PerformanceMetrics perfMetrics = PerformanceMetrics.builder()
                    .totalReturn(BigDecimal.valueOf(25))
                    .sharpeRatio(BigDecimal.valueOf(1.8))
                    .build();

            RiskMetrics riskMetrics = RiskMetrics.builder()
                    .volatility(BigDecimal.valueOf(12))
                    .var95(BigDecimal.valueOf(3))
                    .cvar95(BigDecimal.valueOf(5))
                    .build();

            PortfolioBacktestResult result = PortfolioBacktestResult.builder()
                    .portfolioBacktestId("PB001")
                    .portfolioMetrics(perfMetrics)
                    .portfolioRiskMetrics(riskMetrics)
                    .build();

            assertThat(result.getPortfolioMetrics()).isNotNull();
            assertThat(result.getPortfolioMetrics().getSharpeRatio()).isEqualTo(BigDecimal.valueOf(1.8));
            assertThat(result.getPortfolioRiskMetrics()).isNotNull();
            assertThat(result.getPortfolioRiskMetrics().getVar95()).isEqualTo(BigDecimal.valueOf(3));
        }
    }

    @Nested
    @DisplayName("Symbol Results Tests")
    class SymbolResultsTests {

        @Test
        @DisplayName("Should store symbol results")
        void shouldStoreSymbolResults() {
            Map<String, BacktestResult> symbolResults = Map.of(
                    "005930", BacktestResult.builder()
                            .backtestId("BT_005930")
                            .totalReturn(BigDecimal.valueOf(30))
                            .build(),
                    "000660", BacktestResult.builder()
                            .backtestId("BT_000660")
                            .totalReturn(BigDecimal.valueOf(15))
                            .build()
            );

            PortfolioBacktestResult result = PortfolioBacktestResult.builder()
                    .portfolioBacktestId("PB001")
                    .symbolResults(symbolResults)
                    .build();

            assertThat(result.getSymbolResults()).hasSize(2);
            assertThat(result.getSymbolResults().get("005930").getTotalReturn())
                    .isEqualTo(BigDecimal.valueOf(30));
        }
    }

    @Nested
    @DisplayName("PortfolioEquityPoint Tests")
    class PortfolioEquityPointTests {

        @Test
        @DisplayName("Should create equity point")
        void shouldCreateEquityPoint() {
            LocalDateTime timestamp = LocalDateTime.now();
            Map<String, BigDecimal> symbolEquities = Map.of(
                    "005930", BigDecimal.valueOf(50_000_000),
                    "000660", BigDecimal.valueOf(30_000_000)
            );

            PortfolioBacktestResult.PortfolioEquityPoint point = PortfolioBacktestResult.PortfolioEquityPoint.builder()
                    .timestamp(timestamp)
                    .totalEquity(BigDecimal.valueOf(80_000_000))
                    .symbolEquities(symbolEquities)
                    .build();

            assertThat(point.getTimestamp()).isEqualTo(timestamp);
            assertThat(point.getTotalEquity()).isEqualTo(BigDecimal.valueOf(80_000_000));
            assertThat(point.getSymbolEquities()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Equity Curve Tests")
    class EquityCurveTests {

        @Test
        @DisplayName("Should store equity curve")
        void shouldStoreEquityCurve() {
            List<PortfolioBacktestResult.PortfolioEquityPoint> curve = List.of(
                    PortfolioBacktestResult.PortfolioEquityPoint.builder()
                            .timestamp(LocalDateTime.now().minusDays(2))
                            .totalEquity(BigDecimal.valueOf(100_000_000))
                            .build(),
                    PortfolioBacktestResult.PortfolioEquityPoint.builder()
                            .timestamp(LocalDateTime.now())
                            .totalEquity(BigDecimal.valueOf(120_000_000))
                            .build()
            );

            PortfolioBacktestResult result = PortfolioBacktestResult.builder()
                    .portfolioBacktestId("PB001")
                    .equityCurve(curve)
                    .build();

            assertThat(result.getEquityCurve()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Correlation Matrix Tests")
    class CorrelationMatrixTests {

        @Test
        @DisplayName("Should store correlation matrix")
        void shouldStoreCorrelationMatrix() {
            Map<String, Map<String, BigDecimal>> correlationMatrix = Map.of(
                    "005930", Map.of("005930", BigDecimal.ONE, "000660", BigDecimal.valueOf(0.7)),
                    "000660", Map.of("005930", BigDecimal.valueOf(0.7), "000660", BigDecimal.ONE)
            );

            PortfolioBacktestResult result = PortfolioBacktestResult.builder()
                    .portfolioBacktestId("PB001")
                    .correlationMatrix(correlationMatrix)
                    .build();

            assertThat(result.getCorrelationMatrix()).hasSize(2);
            assertThat(result.getCorrelationMatrix().get("005930").get("000660"))
                    .isEqualTo(BigDecimal.valueOf(0.7));
        }
    }
}
