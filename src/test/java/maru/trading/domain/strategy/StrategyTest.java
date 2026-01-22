package maru.trading.domain.strategy;

import maru.trading.domain.shared.Environment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Strategy Domain Test")
class StrategyTest {

    @Test
    @DisplayName("Should create strategy with builder")
    void shouldCreateStrategyWithBuilder() {
        Strategy strategy = Strategy.builder()
                .strategyId("STR-001")
                .name("MA Crossover")
                .description("Moving Average Crossover Strategy")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId("V-001")
                .build();

        assertThat(strategy.getStrategyId()).isEqualTo("STR-001");
        assertThat(strategy.getName()).isEqualTo("MA Crossover");
        assertThat(strategy.getDescription()).isEqualTo("Moving Average Crossover Strategy");
        assertThat(strategy.getStatus()).isEqualTo("ACTIVE");
        assertThat(strategy.getMode()).isEqualTo(Environment.PAPER);
        assertThat(strategy.getActiveVersionId()).isEqualTo("V-001");
    }

    @Test
    @DisplayName("Should return true when strategy is active")
    void shouldReturnTrueWhenStrategyIsActive() {
        Strategy strategy = Strategy.builder()
                .status("ACTIVE")
                .build();

        assertThat(strategy.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should return false when strategy is inactive")
    void shouldReturnFalseWhenStrategyIsInactive() {
        Strategy strategy = Strategy.builder()
                .status("INACTIVE")
                .build();

        assertThat(strategy.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should activate strategy with version")
    void shouldActivateStrategyWithVersion() {
        Strategy strategy = Strategy.builder()
                .status("INACTIVE")
                .build();

        strategy.activate("V-002");

        assertThat(strategy.isActive()).isTrue();
        assertThat(strategy.getStatus()).isEqualTo("ACTIVE");
        assertThat(strategy.getActiveVersionId()).isEqualTo("V-002");
    }

    @Test
    @DisplayName("Should deactivate strategy")
    void shouldDeactivateStrategy() {
        Strategy strategy = Strategy.builder()
                .status("ACTIVE")
                .activeVersionId("V-001")
                .build();

        strategy.deactivate();

        assertThat(strategy.isActive()).isFalse();
        assertThat(strategy.getStatus()).isEqualTo("INACTIVE");
    }
}
