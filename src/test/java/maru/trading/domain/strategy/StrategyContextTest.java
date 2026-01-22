package maru.trading.domain.strategy;

import maru.trading.domain.market.MarketBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StrategyContext Domain Test")
class StrategyContextTest {

    private MarketBar createTestBar(BigDecimal close) {
        return MarketBar.restore(
                "005930",
                "1d",
                LocalDateTime.now(),
                BigDecimal.valueOf(70000),
                BigDecimal.valueOf(71000),
                BigDecimal.valueOf(69000),
                close,
                1000000L,
                true
        );
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid context")
        void shouldPassValidationWithValidContext() {
            StrategyContext context = StrategyContext.builder()
                    .strategyId("STR-001")
                    .symbol("005930")
                    .accountId("ACC-001")
                    .bars(List.of(createTestBar(BigDecimal.valueOf(70000))))
                    .params(Map.of("period", 14))
                    .timeframe("1d")
                    .build();

            // Should not throw
            context.validate();
        }

        @Test
        @DisplayName("Should throw for null strategy ID")
        void shouldThrowForNullStrategyId() {
            StrategyContext context = StrategyContext.builder()
                    .strategyId(null)
                    .symbol("005930")
                    .accountId("ACC-001")
                    .bars(List.of(createTestBar(BigDecimal.valueOf(70000))))
                    .params(Map.of())
                    .timeframe("1d")
                    .build();

            assertThatThrownBy(context::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Strategy ID");
        }

        @Test
        @DisplayName("Should throw for null symbol")
        void shouldThrowForNullSymbol() {
            StrategyContext context = StrategyContext.builder()
                    .strategyId("STR-001")
                    .symbol(null)
                    .accountId("ACC-001")
                    .bars(List.of(createTestBar(BigDecimal.valueOf(70000))))
                    .params(Map.of())
                    .timeframe("1d")
                    .build();

            assertThatThrownBy(context::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol");
        }

        @Test
        @DisplayName("Should throw for empty bars")
        void shouldThrowForEmptyBars() {
            StrategyContext context = StrategyContext.builder()
                    .strategyId("STR-001")
                    .symbol("005930")
                    .accountId("ACC-001")
                    .bars(List.of())
                    .params(Map.of())
                    .timeframe("1d")
                    .build();

            assertThatThrownBy(context::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bars");
        }
    }

    @Nested
    @DisplayName("Bar Access Tests")
    class BarAccessTests {

        @Test
        @DisplayName("Should return bar count")
        void shouldReturnBarCount() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of(
                            createTestBar(BigDecimal.valueOf(70000)),
                            createTestBar(BigDecimal.valueOf(71000)),
                            createTestBar(BigDecimal.valueOf(72000))
                    ))
                    .build();

            assertThat(context.getBarCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero for null bars")
        void shouldReturnZeroForNullBars() {
            StrategyContext context = StrategyContext.builder()
                    .bars(null)
                    .build();

            assertThat(context.getBarCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return latest bar")
        void shouldReturnLatestBar() {
            MarketBar latestBar = createTestBar(BigDecimal.valueOf(72000));
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of(
                            createTestBar(BigDecimal.valueOf(70000)),
                            createTestBar(BigDecimal.valueOf(71000)),
                            latestBar
                    ))
                    .build();

            assertThat(context.getLatestBar()).isEqualTo(latestBar);
        }

        @Test
        @DisplayName("Should return null for empty bars")
        void shouldReturnNullForEmptyBars() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of())
                    .build();

            assertThat(context.getLatestBar()).isNull();
        }
    }

    @Nested
    @DisplayName("Parameter Access Tests")
    class ParameterAccessTests {

        @Test
        @DisplayName("Should get parameter as integer")
        void shouldGetParameterAsInteger() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of("period", 14))
                    .build();

            assertThat(context.getParamAsInt("period")).isEqualTo(14);
        }

        @Test
        @DisplayName("Should convert number to integer")
        void shouldConvertNumberToInteger() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of("period", 14.0))
                    .build();

            assertThat(context.getParamAsInt("period")).isEqualTo(14);
        }

        @Test
        @DisplayName("Should throw for missing integer parameter")
        void shouldThrowForMissingIntegerParameter() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of())
                    .build();

            assertThatThrownBy(() -> context.getParamAsInt("period"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Missing required parameter");
        }

        @Test
        @DisplayName("Should get parameter as double")
        void shouldGetParameterAsDouble() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of("threshold", 0.7))
                    .build();

            assertThat(context.getParamAsDouble("threshold")).isEqualTo(0.7);
        }

        @Test
        @DisplayName("Should throw for non-numeric parameter")
        void shouldThrowForNonNumericParameter() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of("period", "invalid"))
                    .build();

            assertThatThrownBy(() -> context.getParamAsInt("period"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not an integer");
        }
    }
}
