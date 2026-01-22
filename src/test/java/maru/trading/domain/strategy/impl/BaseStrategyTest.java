package maru.trading.domain.strategy.impl;

import maru.trading.domain.market.MarketBar;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.StrategyContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BaseStrategy Test")
class BaseStrategyTest {

    /**
     * Concrete implementation of BaseStrategy for testing
     */
    private static class TestStrategy extends BaseStrategy {
        @Override
        public SignalDecision evaluate(StrategyContext context) {
            return SignalDecision.hold("Test");
        }

        @Override
        public String getStrategyType() {
            return "TEST";
        }

        @Override
        public void validateParams(Map<String, Object> params) {
            // No-op for test
        }

        // Expose protected methods for testing
        public void testValidateMinimumBars(StrategyContext context, int minBars) {
            validateMinimumBars(context, minBars);
        }

        public List<BigDecimal> testExtractClosePrices(List<MarketBar> bars) {
            return extractClosePrices(bars);
        }

        public List<BigDecimal> testExtractOpenPrices(List<MarketBar> bars) {
            return extractOpenPrices(bars);
        }

        public List<BigDecimal> testExtractHighPrices(List<MarketBar> bars) {
            return extractHighPrices(bars);
        }

        public List<BigDecimal> testExtractLowPrices(List<MarketBar> bars) {
            return extractLowPrices(bars);
        }

        public BigDecimal testGetLatestPrice(StrategyContext context) {
            return getLatestPrice(context);
        }

        public BigDecimal testGetDefaultQuantity(StrategyContext context) {
            return getDefaultQuantity(context);
        }

        public int testGetTtlSeconds(StrategyContext context) {
            return getTtlSeconds(context);
        }
    }

    private final TestStrategy strategy = new TestStrategy();

    private MarketBar createTestBar(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
        return MarketBar.restore(
                "005930", "1d", LocalDateTime.now(),
                open, high, low, close, 1000L, true
        );
    }

    @Nested
    @DisplayName("validateMinimumBars Tests")
    class ValidateMinimumBarsTests {

        @Test
        @DisplayName("Should pass when bars count equals minimum")
        void shouldPassWhenBarsCountEqualsMinimum() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of(
                            createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                            createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(105))
                    ))
                    .build();

            // Should not throw
            strategy.testValidateMinimumBars(context, 2);
        }

        @Test
        @DisplayName("Should pass when bars count exceeds minimum")
        void shouldPassWhenBarsCountExceedsMinimum() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of(
                            createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                            createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(105)),
                            createTestBar(BigDecimal.valueOf(105), BigDecimal.valueOf(110), BigDecimal.valueOf(103), BigDecimal.valueOf(108))
                    ))
                    .build();

            // Should not throw
            strategy.testValidateMinimumBars(context, 2);
        }

        @Test
        @DisplayName("Should throw when bars count is less than minimum")
        void shouldThrowWhenBarsCountIsLessThanMinimum() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of(
                            createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102))
                    ))
                    .build();

            assertThatThrownBy(() -> strategy.testValidateMinimumBars(context, 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient bars")
                    .hasMessageContaining("need 5")
                    .hasMessageContaining("got 1");
        }
    }

    @Nested
    @DisplayName("extractPrices Tests")
    class ExtractPricesTests {

        @Test
        @DisplayName("Should extract close prices")
        void shouldExtractClosePrices() {
            List<MarketBar> bars = List.of(
                    createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                    createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(105))
            );

            List<BigDecimal> closePrices = strategy.testExtractClosePrices(bars);

            assertThat(closePrices).hasSize(2);
            assertThat(closePrices.get(0)).isEqualTo(BigDecimal.valueOf(102));
            assertThat(closePrices.get(1)).isEqualTo(BigDecimal.valueOf(105));
        }

        @Test
        @DisplayName("Should extract open prices")
        void shouldExtractOpenPrices() {
            List<MarketBar> bars = List.of(
                    createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                    createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(105))
            );

            List<BigDecimal> openPrices = strategy.testExtractOpenPrices(bars);

            assertThat(openPrices).hasSize(2);
            assertThat(openPrices.get(0)).isEqualTo(BigDecimal.valueOf(100));
            assertThat(openPrices.get(1)).isEqualTo(BigDecimal.valueOf(102));
        }

        @Test
        @DisplayName("Should extract high prices")
        void shouldExtractHighPrices() {
            List<MarketBar> bars = List.of(
                    createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                    createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(105))
            );

            List<BigDecimal> highPrices = strategy.testExtractHighPrices(bars);

            assertThat(highPrices).hasSize(2);
            assertThat(highPrices.get(0)).isEqualTo(BigDecimal.valueOf(105));
            assertThat(highPrices.get(1)).isEqualTo(BigDecimal.valueOf(108));
        }

        @Test
        @DisplayName("Should extract low prices")
        void shouldExtractLowPrices() {
            List<MarketBar> bars = List.of(
                    createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                    createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(105))
            );

            List<BigDecimal> lowPrices = strategy.testExtractLowPrices(bars);

            assertThat(lowPrices).hasSize(2);
            assertThat(lowPrices.get(0)).isEqualTo(BigDecimal.valueOf(95));
            assertThat(lowPrices.get(1)).isEqualTo(BigDecimal.valueOf(100));
        }
    }

    @Nested
    @DisplayName("getLatestPrice Tests")
    class GetLatestPriceTests {

        @Test
        @DisplayName("Should return latest close price")
        void shouldReturnLatestClosePrice() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of(
                            createTestBar(BigDecimal.valueOf(100), BigDecimal.valueOf(105), BigDecimal.valueOf(95), BigDecimal.valueOf(102)),
                            createTestBar(BigDecimal.valueOf(102), BigDecimal.valueOf(108), BigDecimal.valueOf(100), BigDecimal.valueOf(107))
                    ))
                    .build();

            BigDecimal latestPrice = strategy.testGetLatestPrice(context);

            assertThat(latestPrice).isEqualTo(BigDecimal.valueOf(107));
        }

        @Test
        @DisplayName("Should throw when no bars available")
        void shouldThrowWhenNoBarsAvailable() {
            StrategyContext context = StrategyContext.builder()
                    .bars(List.of())
                    .build();

            assertThatThrownBy(() -> strategy.testGetLatestPrice(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No bars available");
        }
    }

    @Nested
    @DisplayName("getDefaultQuantity Tests")
    class GetDefaultQuantityTests {

        @Test
        @DisplayName("Should return ONE as default quantity")
        void shouldReturnOneAsDefaultQuantity() {
            StrategyContext context = StrategyContext.builder().build();

            BigDecimal quantity = strategy.testGetDefaultQuantity(context);

            assertThat(quantity).isEqualTo(BigDecimal.ONE);
        }
    }

    @Nested
    @DisplayName("getTtlSeconds Tests")
    class GetTtlSecondsTests {

        @Test
        @DisplayName("Should return TTL from params")
        void shouldReturnTtlFromParams() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of("ttlSeconds", 600))
                    .build();

            int ttl = strategy.testGetTtlSeconds(context);

            assertThat(ttl).isEqualTo(600);
        }

        @Test
        @DisplayName("Should return default TTL when param not set")
        void shouldReturnDefaultTtlWhenParamNotSet() {
            StrategyContext context = StrategyContext.builder()
                    .params(Map.of())
                    .build();

            int ttl = strategy.testGetTtlSeconds(context);

            assertThat(ttl).isEqualTo(300); // DEFAULT_TTL_SECONDS
        }
    }

    @Test
    @DisplayName("Should return strategy type")
    void shouldReturnStrategyType() {
        assertThat(strategy.getStrategyType()).isEqualTo("TEST");
    }
}
