package maru.trading.domain.backtest;

import maru.trading.domain.backtest.data.DataSourceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BacktestConfig Test")
class BacktestConfigTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create config with builder")
        void shouldCreateConfigWithBuilder() {
            BacktestConfig config = BacktestConfig.builder()
                    .backtestId("BT001")
                    .strategyId("STR001")
                    .strategyType("MA_CROSSOVER")
                    .strategyParams(Map.of("shortPeriod", 5, "longPeriod", 20))
                    .startDate(LocalDate.of(2024, 1, 1))
                    .endDate(LocalDate.of(2024, 12, 31))
                    .symbols(List.of("005930", "000660"))
                    .timeframe("1d")
                    .initialCapital(BigDecimal.valueOf(100_000_000))
                    .commission(BigDecimal.valueOf(0.0015))
                    .slippage(BigDecimal.valueOf(0.001))
                    .build();

            assertThat(config.getBacktestId()).isEqualTo("BT001");
            assertThat(config.getStrategyId()).isEqualTo("STR001");
            assertThat(config.getStrategyType()).isEqualTo("MA_CROSSOVER");
            assertThat(config.getSymbols()).hasSize(2);
            assertThat(config.getTimeframe()).isEqualTo("1d");
            assertThat(config.getInitialCapital()).isEqualTo(BigDecimal.valueOf(100_000_000));
        }

        @Test
        @DisplayName("Should use default values")
        void shouldUseDefaultValues() {
            BacktestConfig config = BacktestConfig.builder()
                    .backtestId("BT001")
                    .build();

            assertThat(config.getStrategyType()).isEqualTo("MA_CROSSOVER");
            assertThat(config.getTimeframe()).isEqualTo("1m");
            assertThat(config.getInitialCapital()).isEqualTo(BigDecimal.valueOf(10_000_000));
            assertThat(config.getCommission()).isEqualTo(BigDecimal.valueOf(0.001));
            assertThat(config.getSlippage()).isEqualTo(BigDecimal.valueOf(0.0005));
        }
    }

    @Nested
    @DisplayName("DataSourceConfig Tests")
    class DataSourceConfigTests {

        @Test
        @DisplayName("Should set data source config")
        void shouldSetDataSourceConfig() {
            DataSourceConfig dataSourceConfig = DataSourceConfig.database();

            BacktestConfig config = BacktestConfig.builder()
                    .backtestId("BT001")
                    .dataSourceConfig(dataSourceConfig)
                    .build();

            assertThat(config.getDataSourceConfig()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Strategy Params Tests")
    class StrategyParamsTests {

        @Test
        @DisplayName("Should handle various strategy params types")
        void shouldHandleVariousParamsTypes() {
            BacktestConfig config = BacktestConfig.builder()
                    .backtestId("BT001")
                    .strategyParams(Map.of(
                            "intParam", 10,
                            "doubleParam", 0.5,
                            "stringParam", "test"
                    ))
                    .build();

            assertThat(config.getStrategyParams())
                    .containsEntry("intParam", 10)
                    .containsEntry("doubleParam", 0.5)
                    .containsEntry("stringParam", "test");
        }
    }
}
