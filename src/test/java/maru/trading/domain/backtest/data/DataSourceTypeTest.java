package maru.trading.domain.backtest.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataSourceType Enum Test")
class DataSourceTypeTest {

    @Test
    @DisplayName("Should have DATABASE type")
    void shouldHaveDatabaseType() {
        assertThat(DataSourceType.DATABASE).isNotNull();
    }

    @Test
    @DisplayName("Should have CSV type")
    void shouldHaveCsvType() {
        assertThat(DataSourceType.CSV).isNotNull();
    }

    @Test
    @DisplayName("Should have REALTIME type")
    void shouldHaveRealtimeType() {
        assertThat(DataSourceType.REALTIME).isNotNull();
    }

    @Test
    @DisplayName("Should have exactly 3 types")
    void shouldHaveExactly3Types() {
        assertThat(DataSourceType.values()).hasSize(3);
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        assertThat(DataSourceType.valueOf("DATABASE")).isEqualTo(DataSourceType.DATABASE);
        assertThat(DataSourceType.valueOf("CSV")).isEqualTo(DataSourceType.CSV);
        assertThat(DataSourceType.valueOf("REALTIME")).isEqualTo(DataSourceType.REALTIME);
    }
}
