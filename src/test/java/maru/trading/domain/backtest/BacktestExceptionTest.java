package maru.trading.domain.backtest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BacktestException Test")
class BacktestExceptionTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            BacktestException exception = new BacktestException("Backtest failed");

            assertThat(exception.getMessage()).isEqualTo("Backtest failed");
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            BacktestException exception = new BacktestException("Backtest failed", cause);

            assertThat(exception.getMessage()).isEqualTo("Backtest failed");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Exception Type Tests")
    class ExceptionTypeTests {

        @Test
        @DisplayName("Should be checked exception")
        void shouldBeCheckedException() {
            BacktestException exception = new BacktestException("test");

            assertThat(exception).isInstanceOf(Exception.class);
            assertThat(exception).isNotInstanceOf(RuntimeException.class);
        }
    }
}
