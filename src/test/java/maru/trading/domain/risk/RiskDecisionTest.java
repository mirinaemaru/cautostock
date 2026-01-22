package maru.trading.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskDecision Domain Test")
class RiskDecisionTest {

    @Test
    @DisplayName("Should create approved decision with approve() factory method")
    void shouldCreateApprovedDecision() {
        RiskDecision decision = RiskDecision.approve();

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).isEqualTo("Risk check passed");
        assertThat(decision.getRuleViolated()).isNull();
    }

    @Test
    @DisplayName("Should create rejected decision with reject() factory method")
    void shouldCreateRejectedDecision() {
        String reason = "Daily loss limit exceeded";
        String ruleViolated = "DAILY_LOSS_LIMIT";

        RiskDecision decision = RiskDecision.reject(reason, ruleViolated);

        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).isEqualTo(reason);
        assertThat(decision.getRuleViolated()).isEqualTo(ruleViolated);
    }

    @Test
    @DisplayName("Should create decision with builder")
    void shouldCreateDecisionWithBuilder() {
        RiskDecision decision = RiskDecision.builder()
                .approved(true)
                .reason("Custom reason")
                .ruleViolated(null)
                .build();

        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getReason()).isEqualTo("Custom reason");
    }

    @Test
    @DisplayName("Should create rejected decision for max orders exceeded")
    void shouldCreateRejectedDecisionForMaxOrdersExceeded() {
        RiskDecision decision = RiskDecision.reject(
                "Max open orders exceeded: 10 > 5",
                "MAX_OPEN_ORDERS"
        );

        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getReason()).contains("Max open orders exceeded");
        assertThat(decision.getRuleViolated()).isEqualTo("MAX_OPEN_ORDERS");
    }
}
