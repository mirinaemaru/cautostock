package maru.trading.domain.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StrategyVersion Domain Test")
class StrategyVersionTest {

    @Test
    @DisplayName("Should create strategy version with builder")
    void shouldCreateStrategyVersionWithBuilder() {
        StrategyVersion version = StrategyVersion.builder()
                .strategyVersionId("SV-001")
                .strategyId("STR-001")
                .versionNo(1)
                .paramsJson("{\"shortPeriod\": 5, \"longPeriod\": 20}")
                .build();

        assertThat(version.getStrategyVersionId()).isEqualTo("SV-001");
        assertThat(version.getStrategyId()).isEqualTo("STR-001");
        assertThat(version.getVersionNo()).isEqualTo(1);
        assertThat(version.getParamsJson()).isEqualTo("{\"shortPeriod\": 5, \"longPeriod\": 20}");
    }

    @Test
    @DisplayName("Should parse params JSON to map")
    void shouldParseParamsJsonToMap() {
        StrategyVersion version = StrategyVersion.builder()
                .paramsJson("{\"shortPeriod\": 5, \"longPeriod\": 20, \"threshold\": 0.5}")
                .build();

        Map<String, Object> params = version.getParamsAsMap();

        assertThat(params).hasSize(3);
        assertThat(params.get("shortPeriod")).isEqualTo(5);
        assertThat(params.get("longPeriod")).isEqualTo(20);
        assertThat(params.get("threshold")).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should return empty map for null params")
    void shouldReturnEmptyMapForNullParams() {
        StrategyVersion version = StrategyVersion.builder()
                .paramsJson(null)
                .build();

        Map<String, Object> params = version.getParamsAsMap();

        assertThat(params).isEmpty();
    }

    @Test
    @DisplayName("Should return empty map for blank params")
    void shouldReturnEmptyMapForBlankParams() {
        StrategyVersion version = StrategyVersion.builder()
                .paramsJson("   ")
                .build();

        Map<String, Object> params = version.getParamsAsMap();

        assertThat(params).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON")
    void shouldThrowExceptionForInvalidJson() {
        StrategyVersion version = StrategyVersion.builder()
                .paramsJson("invalid json")
                .build();

        assertThatThrownBy(version::getParamsAsMap)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse paramsJson");
    }
}
