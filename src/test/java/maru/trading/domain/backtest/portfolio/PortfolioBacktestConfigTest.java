package maru.trading.domain.backtest.portfolio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioBacktestConfig Test")
class PortfolioBacktestConfigTest {

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have correct default values")
        void shouldHaveCorrectDefaultValues() {
            PortfolioBacktestConfig config = PortfolioBacktestConfig.builder()
                    .portfolioBacktestId("PB001")
                    .build();

            assertThat(config.getStrategyType()).isEqualTo("MA_CROSSOVER");
            assertThat(config.getTimeframe()).isEqualTo("1d");
            assertThat(config.getCommission()).isEqualTo(BigDecimal.valueOf(0.0015));
            assertThat(config.getSlippage()).isEqualTo(BigDecimal.valueOf(0.0005));
            assertThat(config.getRebalancingFrequencyDays()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Should create config with all fields")
        void shouldCreateConfigWithAllFields() {
            Map<String, BigDecimal> weights = Map.of(
                    "005930", BigDecimal.valueOf(0.4),
                    "000660", BigDecimal.valueOf(0.3),
                    "035420", BigDecimal.valueOf(0.3)
            );

            PortfolioBacktestConfig config = PortfolioBacktestConfig.builder()
                    .portfolioBacktestId("PB001")
                    .portfolioName("Korean Tech Portfolio")
                    .symbolWeights(weights)
                    .strategyId("STR001")
                    .strategyType("RSI")
                    .strategyParams(Map.of("period", 14, "overbought", 70))
                    .startDate(LocalDate.of(2023, 1, 1))
                    .endDate(LocalDate.of(2023, 12, 31))
                    .timeframe("1h")
                    .initialCapital(BigDecimal.valueOf(100_000_000))
                    .commission(BigDecimal.valueOf(0.001))
                    .slippage(BigDecimal.valueOf(0.0003))
                    .rebalancingFrequencyDays(30)
                    .portfolioMaxDailyLoss(BigDecimal.valueOf(0.02))
                    .build();

            assertThat(config.getPortfolioBacktestId()).isEqualTo("PB001");
            assertThat(config.getPortfolioName()).isEqualTo("Korean Tech Portfolio");
            assertThat(config.getSymbolWeights()).hasSize(3);
            assertThat(config.getSymbolWeights().get("005930")).isEqualTo(BigDecimal.valueOf(0.4));
            assertThat(config.getStrategyType()).isEqualTo("RSI");
            assertThat(config.getTimeframe()).isEqualTo("1h");
            assertThat(config.getInitialCapital()).isEqualTo(BigDecimal.valueOf(100_000_000));
            assertThat(config.getRebalancingFrequencyDays()).isEqualTo(30);
            assertThat(config.getPortfolioMaxDailyLoss()).isEqualTo(BigDecimal.valueOf(0.02));
        }
    }

    @Nested
    @DisplayName("Symbol Weights Tests")
    class SymbolWeightsTests {

        @Test
        @DisplayName("Should allow various weight allocations")
        void shouldAllowVariousWeightAllocations() {
            // Equal weight
            Map<String, BigDecimal> equalWeights = Map.of(
                    "005930", BigDecimal.valueOf(0.5),
                    "000660", BigDecimal.valueOf(0.5)
            );

            PortfolioBacktestConfig config = PortfolioBacktestConfig.builder()
                    .portfolioBacktestId("PB001")
                    .symbolWeights(equalWeights)
                    .build();

            assertThat(config.getSymbolWeights()).hasSize(2);
            assertThat(config.getSymbolWeights().get("005930"))
                    .isEqualTo(config.getSymbolWeights().get("000660"));
        }
    }
}
