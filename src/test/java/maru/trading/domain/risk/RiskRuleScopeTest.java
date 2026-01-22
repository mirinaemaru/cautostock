package maru.trading.domain.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskRuleScope Enum Test")
class RiskRuleScopeTest {

    @Test
    @DisplayName("Should have GLOBAL scope")
    void shouldHaveGlobalScope() {
        assertThat(RiskRuleScope.GLOBAL).isNotNull();
        assertThat(RiskRuleScope.GLOBAL.name()).isEqualTo("GLOBAL");
    }

    @Test
    @DisplayName("Should have PER_ACCOUNT scope")
    void shouldHavePerAccountScope() {
        assertThat(RiskRuleScope.PER_ACCOUNT).isNotNull();
        assertThat(RiskRuleScope.PER_ACCOUNT.name()).isEqualTo("PER_ACCOUNT");
    }

    @Test
    @DisplayName("Should have PER_SYMBOL scope")
    void shouldHavePerSymbolScope() {
        assertThat(RiskRuleScope.PER_SYMBOL).isNotNull();
        assertThat(RiskRuleScope.PER_SYMBOL.name()).isEqualTo("PER_SYMBOL");
    }

    @Test
    @DisplayName("Should have exactly 3 values")
    void shouldHaveExactly3Values() {
        assertThat(RiskRuleScope.values()).hasSize(3);
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        assertThat(RiskRuleScope.valueOf("GLOBAL")).isEqualTo(RiskRuleScope.GLOBAL);
        assertThat(RiskRuleScope.valueOf("PER_ACCOUNT")).isEqualTo(RiskRuleScope.PER_ACCOUNT);
        assertThat(RiskRuleScope.valueOf("PER_SYMBOL")).isEqualTo(RiskRuleScope.PER_SYMBOL);
    }
}
