package maru.trading.domain.backtest;

import maru.trading.domain.execution.Fill;
import maru.trading.domain.execution.Position;
import maru.trading.domain.order.Order;
import maru.trading.domain.signal.Signal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BacktestResult Test")
class BacktestResultTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create result with builder")
        void shouldCreateResultWithBuilder() {
            LocalDateTime startTime = LocalDateTime.now().minusDays(30);
            LocalDateTime endTime = LocalDateTime.now();

            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .startTime(startTime)
                    .endTime(endTime)
                    .finalCapital(BigDecimal.valueOf(11_000_000))
                    .totalReturn(BigDecimal.valueOf(10))
                    .build();

            assertThat(result.getBacktestId()).isEqualTo("BT001");
            assertThat(result.getStartTime()).isEqualTo(startTime);
            assertThat(result.getEndTime()).isEqualTo(endTime);
            assertThat(result.getFinalCapital()).isEqualTo(BigDecimal.valueOf(11_000_000));
            assertThat(result.getTotalReturn()).isEqualTo(BigDecimal.valueOf(10));
        }

        @Test
        @DisplayName("Should use default empty lists")
        void shouldUseDefaultEmptyLists() {
            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .build();

            assertThat(result.getSignals()).isEmpty();
            assertThat(result.getOrders()).isEmpty();
            assertThat(result.getFills()).isEmpty();
            assertThat(result.getPositions()).isEmpty();
            assertThat(result.getTrades()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Config Association Tests")
    class ConfigAssociationTests {

        @Test
        @DisplayName("Should associate config with result")
        void shouldAssociateConfigWithResult() {
            BacktestConfig config = BacktestConfig.builder()
                    .backtestId("BT001")
                    .strategyType("MA_CROSSOVER")
                    .build();

            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .config(config)
                    .build();

            assertThat(result.getConfig()).isNotNull();
            assertThat(result.getConfig().getStrategyType()).isEqualTo("MA_CROSSOVER");
        }
    }

    @Nested
    @DisplayName("Metrics Association Tests")
    class MetricsAssociationTests {

        @Test
        @DisplayName("Should associate performance metrics")
        void shouldAssociatePerformanceMetrics() {
            PerformanceMetrics metrics = PerformanceMetrics.builder()
                    .totalReturn(BigDecimal.valueOf(15))
                    .winRate(BigDecimal.valueOf(60))
                    .build();

            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .performanceMetrics(metrics)
                    .build();

            assertThat(result.getPerformanceMetrics()).isNotNull();
            assertThat(result.getPerformanceMetrics().getTotalReturn())
                    .isEqualTo(BigDecimal.valueOf(15));
        }

        @Test
        @DisplayName("Should associate risk metrics")
        void shouldAssociateRiskMetrics() {
            RiskMetrics metrics = RiskMetrics.builder()
                    .volatility(BigDecimal.valueOf(15))
                    .var95(BigDecimal.valueOf(3))
                    .cvar95(BigDecimal.valueOf(5))
                    .build();

            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .riskMetrics(metrics)
                    .build();

            assertThat(result.getRiskMetrics()).isNotNull();
            assertThat(result.getRiskMetrics().getVolatility())
                    .isEqualTo(BigDecimal.valueOf(15));
            assertThat(result.getRiskMetrics().getVar95())
                    .isEqualTo(BigDecimal.valueOf(3));
        }

        @Test
        @DisplayName("Should associate equity curve")
        void shouldAssociateEquityCurve() {
            EquityCurve equityCurve = EquityCurve.builder().build();
            equityCurve.addPoint(LocalDateTime.now(), BigDecimal.valueOf(10_000_000));

            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .equityCurve(equityCurve)
                    .build();

            assertThat(result.getEquityCurve()).isNotNull();
            assertThat(result.getEquityCurve().getPoints()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Trades Collection Tests")
    class TradesCollectionTests {

        @Test
        @DisplayName("Should store trades")
        void shouldStoreTrades() {
            List<BacktestTrade> trades = new ArrayList<>();
            trades.add(BacktestTrade.builder()
                    .tradeId("T1")
                    .netPnl(BigDecimal.valueOf(1000))
                    .build());
            trades.add(BacktestTrade.builder()
                    .tradeId("T2")
                    .netPnl(BigDecimal.valueOf(-500))
                    .build());

            BacktestResult result = BacktestResult.builder()
                    .backtestId("BT001")
                    .trades(trades)
                    .build();

            assertThat(result.getTrades()).hasSize(2);
            assertThat(result.getTrades().get(0).getTradeId()).isEqualTo("T1");
        }
    }
}
