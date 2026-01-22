package maru.trading.domain.signal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SignalDecision Domain Test")
class SignalDecisionTest {

    @Nested
    @DisplayName("Factory Method Tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create HOLD decision with hold() method")
        void shouldCreateHoldDecision() {
            SignalDecision decision = SignalDecision.hold("No clear signal");

            assertThat(decision.getSignalType()).isEqualTo(SignalType.HOLD);
            assertThat(decision.getReason()).isEqualTo("No clear signal");
            assertThat(decision.getTargetType()).isNull();
            assertThat(decision.getTargetValue()).isNull();
        }

        @Test
        @DisplayName("Should create BUY decision with buy() method")
        void shouldCreateBuyDecision() {
            BigDecimal quantity = BigDecimal.valueOf(10);
            String reason = "MA crossover detected";
            Integer ttl = 300;

            SignalDecision decision = SignalDecision.buy(quantity, reason, ttl);

            assertThat(decision.getSignalType()).isEqualTo(SignalType.BUY);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualTo(quantity);
            assertThat(decision.getReason()).isEqualTo(reason);
            assertThat(decision.getTtlSeconds()).isEqualTo(ttl);
        }

        @Test
        @DisplayName("Should create SELL decision with sell() method")
        void shouldCreateSellDecision() {
            BigDecimal quantity = BigDecimal.valueOf(5);
            String reason = "RSI overbought";
            Integer ttl = 180;

            SignalDecision decision = SignalDecision.sell(quantity, reason, ttl);

            assertThat(decision.getSignalType()).isEqualTo(SignalType.SELL);
            assertThat(decision.getTargetType()).isEqualTo("QTY");
            assertThat(decision.getTargetValue()).isEqualTo(quantity);
            assertThat(decision.getReason()).isEqualTo(reason);
            assertThat(decision.getTtlSeconds()).isEqualTo(ttl);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation for valid HOLD signal")
        void shouldPassValidationForHold() {
            SignalDecision decision = SignalDecision.hold("No action");

            // Should not throw
            decision.validate();
        }

        @Test
        @DisplayName("Should pass validation for valid BUY signal")
        void shouldPassValidationForBuy() {
            SignalDecision decision = SignalDecision.buy(BigDecimal.TEN, "Bullish", 300);

            // Should not throw
            decision.validate();
        }

        @Test
        @DisplayName("Should throw for null signal type")
        void shouldThrowForNullSignalType() {
            SignalDecision decision = SignalDecision.builder().build();

            assertThatThrownBy(decision::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Signal type cannot be null");
        }

        @Test
        @DisplayName("Should throw for BUY signal without target type")
        void shouldThrowForBuyWithoutTargetType() {
            SignalDecision decision = SignalDecision.builder()
                    .signalType(SignalType.BUY)
                    .targetValue(BigDecimal.TEN)
                    .build();

            assertThatThrownBy(decision::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target type cannot be null");
        }

        @Test
        @DisplayName("Should throw for SELL signal without target value")
        void shouldThrowForSellWithoutTargetValue() {
            SignalDecision decision = SignalDecision.builder()
                    .signalType(SignalType.SELL)
                    .targetType("QTY")
                    .build();

            assertThatThrownBy(decision::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target value must be positive");
        }

        @Test
        @DisplayName("Should throw for negative target value")
        void shouldThrowForNegativeTargetValue() {
            SignalDecision decision = SignalDecision.builder()
                    .signalType(SignalType.BUY)
                    .targetType("QTY")
                    .targetValue(BigDecimal.valueOf(-5))
                    .build();

            assertThatThrownBy(decision::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target value must be positive");
        }

        @Test
        @DisplayName("Should throw for zero target value")
        void shouldThrowForZeroTargetValue() {
            SignalDecision decision = SignalDecision.builder()
                    .signalType(SignalType.BUY)
                    .targetType("QTY")
                    .targetValue(BigDecimal.ZERO)
                    .build();

            assertThatThrownBy(decision::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target value must be positive");
        }

        @Test
        @DisplayName("Should throw for non-positive TTL")
        void shouldThrowForNonPositiveTtl() {
            SignalDecision decision = SignalDecision.builder()
                    .signalType(SignalType.BUY)
                    .targetType("QTY")
                    .targetValue(BigDecimal.TEN)
                    .ttlSeconds(0)
                    .build();

            assertThatThrownBy(decision::validate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TTL seconds must be positive");
        }
    }

    @Nested
    @DisplayName("isTradeable Tests")
    class IsTradeableTests {

        @Test
        @DisplayName("Should return true for BUY signal")
        void shouldReturnTrueForBuy() {
            SignalDecision decision = SignalDecision.buy(BigDecimal.TEN, "Buy", 300);

            assertThat(decision.isTradeable()).isTrue();
        }

        @Test
        @DisplayName("Should return true for SELL signal")
        void shouldReturnTrueForSell() {
            SignalDecision decision = SignalDecision.sell(BigDecimal.TEN, "Sell", 300);

            assertThat(decision.isTradeable()).isTrue();
        }

        @Test
        @DisplayName("Should return false for HOLD signal")
        void shouldReturnFalseForHold() {
            SignalDecision decision = SignalDecision.hold("Hold");

            assertThat(decision.isTradeable()).isFalse();
        }
    }

    @Test
    @DisplayName("Should have toString representation")
    void shouldHaveToString() {
        SignalDecision decision = SignalDecision.buy(BigDecimal.TEN, "Test", 300);

        String toString = decision.toString();

        assertThat(toString).contains("BUY");
        assertThat(toString).contains("QTY");
        assertThat(toString).contains("10");
    }
}
