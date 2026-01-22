package maru.trading.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskState Domain Test")
class RiskStateTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {

        @Test
        @DisplayName("Should create risk state with builder")
        void shouldCreateRiskStateWithBuilder() {
            RiskState state = RiskState.builder()
                    .riskStateId("RS-001")
                    .scope("ACCOUNT")
                    .accountId("ACC-001")
                    .killSwitchStatus(KillSwitchStatus.OFF)
                    .dailyPnl(BigDecimal.valueOf(10000))
                    .exposure(BigDecimal.valueOf(500000))
                    .consecutiveOrderFailures(0)
                    .openOrderCount(2)
                    .build();

            assertThat(state.getRiskStateId()).isEqualTo("RS-001");
            assertThat(state.getScope()).isEqualTo("ACCOUNT");
            assertThat(state.getAccountId()).isEqualTo("ACC-001");
            assertThat(state.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
            assertThat(state.getDailyPnl()).isEqualTo(BigDecimal.valueOf(10000));
            assertThat(state.getExposure()).isEqualTo(BigDecimal.valueOf(500000));
            assertThat(state.getConsecutiveOrderFailures()).isEqualTo(0);
            assertThat(state.getOpenOrderCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should create default state")
        void shouldCreateDefaultState() {
            RiskState state = RiskState.defaultState();

            assertThat(state.getScope()).isEqualTo("GLOBAL");
            assertThat(state.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
            assertThat(state.getDailyPnl()).isEqualTo(BigDecimal.ZERO);
            assertThat(state.getExposure()).isEqualTo(BigDecimal.ZERO);
            assertThat(state.getConsecutiveOrderFailures()).isEqualTo(0);
            assertThat(state.getOpenOrderCount()).isEqualTo(0);
            assertThat(state.getOrderFrequencyTracker()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Kill Switch Tests")
    class KillSwitchTests {

        @Test
        @DisplayName("Should toggle kill switch to ON")
        void shouldToggleKillSwitchToOn() {
            RiskState state = RiskState.defaultState();

            state.toggleKillSwitch(KillSwitchStatus.ON, "Daily loss limit exceeded");

            assertThat(state.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.ON);
            assertThat(state.getKillSwitchReason()).isEqualTo("Daily loss limit exceeded");
        }

        @Test
        @DisplayName("Should toggle kill switch to ARMED")
        void shouldToggleKillSwitchToArmed() {
            RiskState state = RiskState.defaultState();

            state.toggleKillSwitch(KillSwitchStatus.ARMED, "Approaching loss limit");

            assertThat(state.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.ARMED);
            assertThat(state.getKillSwitchReason()).isEqualTo("Approaching loss limit");
        }

        @Test
        @DisplayName("Should toggle kill switch to OFF")
        void shouldToggleKillSwitchToOff() {
            RiskState state = RiskState.builder()
                    .killSwitchStatus(KillSwitchStatus.ON)
                    .killSwitchReason("Previous reason")
                    .build();

            state.toggleKillSwitch(KillSwitchStatus.OFF, "Manual reset by admin");

            assertThat(state.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
            assertThat(state.getKillSwitchReason()).isEqualTo("Manual reset by admin");
        }
    }

    @Nested
    @DisplayName("Failure Count Tests")
    class FailureCountTests {

        @Test
        @DisplayName("Should increment failure count")
        void shouldIncrementFailureCount() {
            RiskState state = RiskState.builder()
                    .consecutiveOrderFailures(2)
                    .build();

            state.incrementFailureCount();

            assertThat(state.getConsecutiveOrderFailures()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should reset failure count")
        void shouldResetFailureCount() {
            RiskState state = RiskState.builder()
                    .consecutiveOrderFailures(5)
                    .build();

            state.resetFailureCount();

            assertThat(state.getConsecutiveOrderFailures()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Daily PnL Tests")
    class DailyPnlTests {

        @Test
        @DisplayName("Should update daily PnL with profit")
        void shouldUpdateDailyPnlWithProfit() {
            RiskState state = RiskState.builder()
                    .dailyPnl(BigDecimal.valueOf(10000))
                    .build();

            state.updateDailyPnl(BigDecimal.valueOf(5000));

            assertThat(state.getDailyPnl()).isEqualTo(BigDecimal.valueOf(15000));
        }

        @Test
        @DisplayName("Should update daily PnL with loss")
        void shouldUpdateDailyPnlWithLoss() {
            RiskState state = RiskState.builder()
                    .dailyPnl(BigDecimal.valueOf(10000))
                    .build();

            state.updateDailyPnl(BigDecimal.valueOf(-15000));

            assertThat(state.getDailyPnl()).isEqualTo(BigDecimal.valueOf(-5000));
        }
    }

    @Nested
    @DisplayName("Order Frequency Tests")
    class OrderFrequencyTests {

        @Test
        @DisplayName("Should record order timestamp")
        void shouldRecordOrderTimestamp() {
            RiskState state = RiskState.defaultState();
            LocalDateTime timestamp = LocalDateTime.now();

            state.recordOrderTimestamp(timestamp);

            assertThat(state.getOrderFrequencyTracker().getCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should initialize tracker if null when recording")
        void shouldInitializeTrackerIfNull() {
            RiskState state = RiskState.builder().build();

            state.recordOrderTimestamp(LocalDateTime.now());

            assertThat(state.getOrderFrequencyTracker()).isNotNull();
            assertThat(state.getOrderFrequencyTracker().getCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should check order frequency limit not exceeded")
        void shouldCheckOrderFrequencyLimitNotExceeded() {
            RiskState state = RiskState.defaultState();
            state.recordOrderTimestamp(LocalDateTime.now().minusSeconds(30));
            state.recordOrderTimestamp(LocalDateTime.now().minusSeconds(20));

            boolean wouldExceed = state.wouldExceedOrderFrequencyLimit(10);

            assertThat(wouldExceed).isFalse();
        }

        @Test
        @DisplayName("Should return false when tracker is null")
        void shouldReturnFalseWhenTrackerIsNull() {
            RiskState state = RiskState.builder().build();

            boolean wouldExceed = state.wouldExceedOrderFrequencyLimit(10);

            assertThat(wouldExceed).isFalse();
        }
    }
}
