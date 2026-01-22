package maru.trading.domain.backtest.optimization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OptimizationException Test")
class OptimizationExceptionTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            OptimizationException exception = new OptimizationException("Optimization failed");

            assertThat(exception.getMessage()).isEqualTo("Optimization failed");
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            OptimizationException exception = new OptimizationException("Optimization failed", cause);

            assertThat(exception.getMessage()).isEqualTo("Optimization failed");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }

    @Nested
    @DisplayName("Exception Type Tests")
    class ExceptionTypeTests {

        @Test
        @DisplayName("Should be checked exception")
        void shouldBeCheckedException() {
            OptimizationException exception = new OptimizationException("test");

            assertThat(exception).isInstanceOf(Exception.class);
            assertThat(exception).isNotInstanceOf(RuntimeException.class);
        }
    }
}
