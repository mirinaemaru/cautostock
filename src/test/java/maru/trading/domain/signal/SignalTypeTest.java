package maru.trading.domain.signal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignalType Enum Test")
class SignalTypeTest {

    @Test
    @DisplayName("Should have BUY type")
    void shouldHaveBuyType() {
        assertThat(SignalType.BUY).isNotNull();
        assertThat(SignalType.BUY.name()).isEqualTo("BUY");
    }

    @Test
    @DisplayName("Should have SELL type")
    void shouldHaveSellType() {
        assertThat(SignalType.SELL).isNotNull();
        assertThat(SignalType.SELL.name()).isEqualTo("SELL");
    }

    @Test
    @DisplayName("Should have HOLD type")
    void shouldHaveHoldType() {
        assertThat(SignalType.HOLD).isNotNull();
        assertThat(SignalType.HOLD.name()).isEqualTo("HOLD");
    }

    @Test
    @DisplayName("Should have exactly 3 values")
    void shouldHaveExactly3Values() {
        assertThat(SignalType.values()).hasSize(3);
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        assertThat(SignalType.valueOf("BUY")).isEqualTo(SignalType.BUY);
        assertThat(SignalType.valueOf("SELL")).isEqualTo(SignalType.SELL);
        assertThat(SignalType.valueOf("HOLD")).isEqualTo(SignalType.HOLD);
    }
}
