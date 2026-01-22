package maru.trading.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KillSwitchStatus Enum Test")
class KillSwitchStatusTest {

    @Test
    @DisplayName("Should have OFF status")
    void shouldHaveOffStatus() {
        assertThat(KillSwitchStatus.OFF).isNotNull();
        assertThat(KillSwitchStatus.OFF.name()).isEqualTo("OFF");
    }

    @Test
    @DisplayName("Should have ARMED status")
    void shouldHaveArmedStatus() {
        assertThat(KillSwitchStatus.ARMED).isNotNull();
        assertThat(KillSwitchStatus.ARMED.name()).isEqualTo("ARMED");
    }

    @Test
    @DisplayName("Should have ON status")
    void shouldHaveOnStatus() {
        assertThat(KillSwitchStatus.ON).isNotNull();
        assertThat(KillSwitchStatus.ON.name()).isEqualTo("ON");
    }

    @Test
    @DisplayName("Should have exactly 3 values")
    void shouldHaveExactly3Values() {
        assertThat(KillSwitchStatus.values()).hasSize(3);
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        assertThat(KillSwitchStatus.valueOf("OFF")).isEqualTo(KillSwitchStatus.OFF);
        assertThat(KillSwitchStatus.valueOf("ARMED")).isEqualTo(KillSwitchStatus.ARMED);
        assertThat(KillSwitchStatus.valueOf("ON")).isEqualTo(KillSwitchStatus.ON);
    }
}
