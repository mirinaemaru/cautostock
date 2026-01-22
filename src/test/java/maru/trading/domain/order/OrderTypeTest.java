package maru.trading.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderType Enum Test")
class OrderTypeTest {

    @Test
    @DisplayName("Should have LIMIT and MARKET values")
    void shouldHaveLimitAndMarketValues() {
        assertThat(OrderType.values()).containsExactly(OrderType.LIMIT, OrderType.MARKET);
    }

    @Test
    @DisplayName("Should return correct name for LIMIT")
    void shouldReturnCorrectNameForLimit() {
        assertThat(OrderType.LIMIT.name()).isEqualTo("LIMIT");
    }

    @Test
    @DisplayName("Should return correct name for MARKET")
    void shouldReturnCorrectNameForMarket() {
        assertThat(OrderType.MARKET.name()).isEqualTo("MARKET");
    }

    @Test
    @DisplayName("Should parse from string")
    void shouldParseFromString() {
        assertThat(OrderType.valueOf("LIMIT")).isEqualTo(OrderType.LIMIT);
        assertThat(OrderType.valueOf("MARKET")).isEqualTo(OrderType.MARKET);
    }
}
