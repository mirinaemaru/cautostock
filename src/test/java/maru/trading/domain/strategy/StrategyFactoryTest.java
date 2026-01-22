package maru.trading.domain.strategy;

import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StrategyFactory Test")
class StrategyFactoryTest {

    @Nested
    @DisplayName("createStrategy Tests")
    class CreateStrategyTests {

        @Test
        @DisplayName("Should create MA_CROSSOVER strategy")
        void shouldCreateMACrossoverStrategy() {
            StrategyEngine strategy = StrategyFactory.createStrategy("MA_CROSSOVER");

            assertThat(strategy).isNotNull();
            assertThat(strategy.getClass().getSimpleName()).isEqualTo("MACrossoverStrategy");
        }

        @Test
        @DisplayName("Should create RSI strategy")
        void shouldCreateRSIStrategy() {
            StrategyEngine strategy = StrategyFactory.createStrategy("RSI");

            assertThat(strategy).isNotNull();
            assertThat(strategy.getClass().getSimpleName()).isEqualTo("RSIStrategy");
        }

        @Test
        @DisplayName("Should create BOLLINGER_BANDS strategy")
        void shouldCreateBollingerBandsStrategy() {
            StrategyEngine strategy = StrategyFactory.createStrategy("BOLLINGER_BANDS");

            assertThat(strategy).isNotNull();
            assertThat(strategy.getClass().getSimpleName()).isEqualTo("BollingerBandsStrategy");
        }

        @Test
        @DisplayName("Should create MACD strategy")
        void shouldCreateMACDStrategy() {
            StrategyEngine strategy = StrategyFactory.createStrategy("MACD");

            assertThat(strategy).isNotNull();
            assertThat(strategy.getClass().getSimpleName()).isEqualTo("MACDStrategy");
        }

        @Test
        @DisplayName("Should throw for null strategy type")
        void shouldThrowForNullStrategyType() {
            assertThatThrownBy(() -> StrategyFactory.createStrategy(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("Should throw for blank strategy type")
        void shouldThrowForBlankStrategyType() {
            assertThatThrownBy(() -> StrategyFactory.createStrategy("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("Should throw for unknown strategy type")
        void shouldThrowForUnknownStrategyType() {
            assertThatThrownBy(() -> StrategyFactory.createStrategy("UNKNOWN_TYPE"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown strategy type");
        }
    }

    @Nested
    @DisplayName("registerStrategy Tests")
    class RegisterStrategyTests {

        @Test
        @DisplayName("Should register custom strategy")
        void shouldRegisterCustomStrategy() {
            // Given
            String customType = "CUSTOM_TEST_" + System.currentTimeMillis();

            // When
            StrategyFactory.registerStrategy(customType, TestStrategy.class);

            // Then
            assertThat(StrategyFactory.isRegistered(customType)).isTrue();
            assertThat(StrategyFactory.getRegisteredTypes()).contains(customType);
        }

        @Test
        @DisplayName("Should throw for null strategy type in registration")
        void shouldThrowForNullStrategyTypeInRegistration() {
            assertThatThrownBy(() -> StrategyFactory.registerStrategy(null, TestStrategy.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("Should throw for blank strategy type in registration")
        void shouldThrowForBlankStrategyTypeInRegistration() {
            assertThatThrownBy(() -> StrategyFactory.registerStrategy("   ", TestStrategy.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null or blank");
        }

        @Test
        @DisplayName("Should throw for null strategy class")
        void shouldThrowForNullStrategyClass() {
            assertThatThrownBy(() -> StrategyFactory.registerStrategy("TEST_TYPE", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Strategy class cannot be null");
        }
    }

    @Nested
    @DisplayName("isRegistered Tests")
    class IsRegisteredTests {

        @Test
        @DisplayName("Should return true for built-in strategies")
        void shouldReturnTrueForBuiltInStrategies() {
            assertThat(StrategyFactory.isRegistered("MA_CROSSOVER")).isTrue();
            assertThat(StrategyFactory.isRegistered("RSI")).isTrue();
            assertThat(StrategyFactory.isRegistered("BOLLINGER_BANDS")).isTrue();
            assertThat(StrategyFactory.isRegistered("MACD")).isTrue();
        }

        @Test
        @DisplayName("Should return false for unknown type")
        void shouldReturnFalseForUnknownType() {
            assertThat(StrategyFactory.isRegistered("UNKNOWN_TYPE")).isFalse();
        }

        @Test
        @DisplayName("Should return false for null type")
        void shouldReturnFalseForNullType() {
            assertThat(StrategyFactory.isRegistered(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getRegisteredTypes Tests")
    class GetRegisteredTypesTests {

        @Test
        @DisplayName("Should return all built-in strategy types")
        void shouldReturnAllBuiltInStrategyTypes() {
            Set<String> types = StrategyFactory.getRegisteredTypes();

            assertThat(types).contains("MA_CROSSOVER", "RSI", "BOLLINGER_BANDS", "MACD");
        }

        @Test
        @DisplayName("Should have at least 4 registered types")
        void shouldHaveAtLeast4RegisteredTypes() {
            Set<String> types = StrategyFactory.getRegisteredTypes();

            assertThat(types).hasSizeGreaterThanOrEqualTo(4);
        }
    }

    /**
     * Test strategy implementation for registration tests.
     */
    public static class TestStrategy implements StrategyEngine {
        @Override
        public SignalDecision evaluate(StrategyContext context) {
            return SignalDecision.builder()
                    .signalType(SignalType.HOLD)
                    .reason("Test strategy")
                    .build();
        }

        @Override
        public String getStrategyType() {
            return "TEST";
        }

        @Override
        public void validateParams(Map<String, Object> params) {
            // No-op
        }
    }
}
