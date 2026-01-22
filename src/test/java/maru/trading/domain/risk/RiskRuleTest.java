package maru.trading.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskRule Domain Test")
class RiskRuleTest {

    @Test
    @DisplayName("Should create risk rule with builder")
    void shouldCreateRiskRuleWithBuilder() {
        RiskRule rule = RiskRule.builder()
                .riskRuleId("RR-001")
                .scope(RiskRuleScope.PER_ACCOUNT)
                .accountId("ACC-001")
                .symbol("005930")
                .maxPositionValuePerSymbol(BigDecimal.valueOf(500000))
                .maxOpenOrders(3)
                .maxOrdersPerMinute(5)
                .dailyLossLimit(BigDecimal.valueOf(30000))
                .consecutiveOrderFailuresLimit(3)
                .build();

        assertThat(rule.getRiskRuleId()).isEqualTo("RR-001");
        assertThat(rule.getScope()).isEqualTo(RiskRuleScope.PER_ACCOUNT);
        assertThat(rule.getAccountId()).isEqualTo("ACC-001");
        assertThat(rule.getSymbol()).isEqualTo("005930");
        assertThat(rule.getMaxPositionValuePerSymbol()).isEqualTo(BigDecimal.valueOf(500000));
        assertThat(rule.getMaxOpenOrders()).isEqualTo(3);
        assertThat(rule.getMaxOrdersPerMinute()).isEqualTo(5);
        assertThat(rule.getDailyLossLimit()).isEqualTo(BigDecimal.valueOf(30000));
        assertThat(rule.getConsecutiveOrderFailuresLimit()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should create default global rule")
    void shouldCreateDefaultGlobalRule() {
        RiskRule rule = RiskRule.defaultGlobalRule();

        assertThat(rule.getScope()).isEqualTo(RiskRuleScope.GLOBAL);
        assertThat(rule.getMaxPositionValuePerSymbol()).isEqualTo(BigDecimal.valueOf(1000000));
        assertThat(rule.getMaxOpenOrders()).isEqualTo(5);
        assertThat(rule.getMaxOrdersPerMinute()).isEqualTo(10);
        assertThat(rule.getDailyLossLimit()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(rule.getConsecutiveOrderFailuresLimit()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should create GLOBAL scope rule")
    void shouldCreateGlobalScopeRule() {
        RiskRule rule = RiskRule.builder()
                .scope(RiskRuleScope.GLOBAL)
                .build();

        assertThat(rule.getScope()).isEqualTo(RiskRuleScope.GLOBAL);
        assertThat(rule.getAccountId()).isNull();
        assertThat(rule.getSymbol()).isNull();
    }

    @Test
    @DisplayName("Should create PER_SYMBOL scope rule")
    void shouldCreatePerSymbolScopeRule() {
        RiskRule rule = RiskRule.builder()
                .scope(RiskRuleScope.PER_SYMBOL)
                .accountId("ACC-001")
                .symbol("005930")
                .maxPositionValuePerSymbol(BigDecimal.valueOf(200000))
                .build();

        assertThat(rule.getScope()).isEqualTo(RiskRuleScope.PER_SYMBOL);
        assertThat(rule.getAccountId()).isEqualTo("ACC-001");
        assertThat(rule.getSymbol()).isEqualTo("005930");
        assertThat(rule.getMaxPositionValuePerSymbol()).isEqualTo(BigDecimal.valueOf(200000));
    }
}
