package maru.trading.domain.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderStatus Enum Test")
class OrderStatusTest {

    @Test
    @DisplayName("Should have all expected status values")
    void shouldHaveAllExpectedStatusValues() {
        assertThat(OrderStatus.values()).containsExactly(
                OrderStatus.NEW,
                OrderStatus.SENT,
                OrderStatus.ACCEPTED,
                OrderStatus.PART_FILLED,
                OrderStatus.FILLED,
                OrderStatus.CANCELLED,
                OrderStatus.REJECTED,
                OrderStatus.ERROR
        );
    }

    @Test
    @DisplayName("Should return correct name for each status")
    void shouldReturnCorrectNameForEachStatus() {
        assertThat(OrderStatus.NEW.name()).isEqualTo("NEW");
        assertThat(OrderStatus.SENT.name()).isEqualTo("SENT");
        assertThat(OrderStatus.ACCEPTED.name()).isEqualTo("ACCEPTED");
        assertThat(OrderStatus.PART_FILLED.name()).isEqualTo("PART_FILLED");
        assertThat(OrderStatus.FILLED.name()).isEqualTo("FILLED");
        assertThat(OrderStatus.CANCELLED.name()).isEqualTo("CANCELLED");
        assertThat(OrderStatus.REJECTED.name()).isEqualTo("REJECTED");
        assertThat(OrderStatus.ERROR.name()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("Should parse from string")
    void shouldParseFromString() {
        assertThat(OrderStatus.valueOf("NEW")).isEqualTo(OrderStatus.NEW);
        assertThat(OrderStatus.valueOf("FILLED")).isEqualTo(OrderStatus.FILLED);
        assertThat(OrderStatus.valueOf("CANCELLED")).isEqualTo(OrderStatus.CANCELLED);
    }
}
