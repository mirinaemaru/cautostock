package maru.trading.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Side Enum Test")
class SideTest {

    @Test
    @DisplayName("Should have BUY and SELL values")
    void shouldHaveBuyAndSellValues() {
        assertThat(Side.values()).containsExactly(Side.BUY, Side.SELL);
    }

    @Test
    @DisplayName("Should return correct name for BUY")
    void shouldReturnCorrectNameForBuy() {
        assertThat(Side.BUY.name()).isEqualTo("BUY");
    }

    @Test
    @DisplayName("Should return correct name for SELL")
    void shouldReturnCorrectNameForSell() {
        assertThat(Side.SELL.name()).isEqualTo("SELL");
    }

    @Test
    @DisplayName("Should parse from string")
    void shouldParseFromString() {
        assertThat(Side.valueOf("BUY")).isEqualTo(Side.BUY);
        assertThat(Side.valueOf("SELL")).isEqualTo(Side.SELL);
    }
}
