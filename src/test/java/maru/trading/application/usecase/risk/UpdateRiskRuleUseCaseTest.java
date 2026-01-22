package maru.trading.application.usecase.risk;

import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRiskRuleUseCase Test")
class UpdateRiskRuleUseCaseTest {

    @Mock
    private RiskRuleRepository riskRuleRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UpdateRiskRuleUseCase updateRiskRuleUseCase;

    @Nested
    @DisplayName("Global Rule Tests")
    class GlobalRuleTests {

        @Test
        @DisplayName("Should create global risk rule")
        void shouldCreateGlobalRiskRule() {
            // Given
            RiskRule savedRule = RiskRule.builder()
                    .riskRuleId("RULE_001")
                    .scope(RiskRuleScope.GLOBAL)
                    .maxPositionValuePerSymbol(BigDecimal.valueOf(10_000_000))
                    .maxOpenOrders(10)
                    .build();

            when(riskRuleRepository.save(any())).thenReturn(savedRule);

            // When
            RiskRule result = updateRiskRuleUseCase.updateGlobalRule(
                    BigDecimal.valueOf(10_000_000),
                    10, 5,
                    BigDecimal.valueOf(500_000),
                    3
            );

            // Then
            assertThat(result.getScope()).isEqualTo(RiskRuleScope.GLOBAL);
            verify(riskRuleRepository).save(any());
        }

        @Test
        @DisplayName("Should publish RiskRuleUpdated event")
        void shouldPublishRiskRuleUpdatedEvent() {
            // Given
            RiskRule savedRule = RiskRule.builder()
                    .riskRuleId("RULE_001")
                    .scope(RiskRuleScope.GLOBAL)
                    .build();

            when(riskRuleRepository.save(any())).thenReturn(savedRule);

            // When
            updateRiskRuleUseCase.updateGlobalRule(
                    BigDecimal.valueOf(10_000_000),
                    10, 5, null, null
            );

            // Then
            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxService).save(eventCaptor.capture());

            OutboxEvent event = eventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo("RiskRuleUpdated");
            assertThat(event.getPayload()).containsEntry("scope", "GLOBAL");
        }
    }

    @Nested
    @DisplayName("Account Rule Tests")
    class AccountRuleTests {

        @Test
        @DisplayName("Should create account-specific risk rule")
        void shouldCreateAccountSpecificRiskRule() {
            // Given
            String accountId = "ACC_001";
            RiskRule savedRule = RiskRule.builder()
                    .riskRuleId("RULE_002")
                    .scope(RiskRuleScope.PER_ACCOUNT)
                    .accountId(accountId)
                    .build();

            when(riskRuleRepository.save(any())).thenReturn(savedRule);

            // When
            RiskRule result = updateRiskRuleUseCase.updateAccountRule(
                    accountId,
                    BigDecimal.valueOf(5_000_000),
                    5, 3, null, null
            );

            // Then
            assertThat(result.getScope()).isEqualTo(RiskRuleScope.PER_ACCOUNT);
            assertThat(result.getAccountId()).isEqualTo(accountId);
        }

        @Test
        @DisplayName("Should throw exception when account ID is null")
        void shouldThrowExceptionWhenAccountIdIsNull() {
            // When & Then
            assertThatThrownBy(() -> updateRiskRuleUseCase.updateAccountRule(
                    null, BigDecimal.TEN, 10, 5, null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Account ID cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw exception when account ID is blank")
        void shouldThrowExceptionWhenAccountIdIsBlank() {
            // When & Then
            assertThatThrownBy(() -> updateRiskRuleUseCase.updateAccountRule(
                    "  ", BigDecimal.TEN, 10, 5, null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Symbol Rule Tests")
    class SymbolRuleTests {

        @Test
        @DisplayName("Should create symbol-specific risk rule")
        void shouldCreateSymbolSpecificRiskRule() {
            // Given
            String accountId = "ACC_001";
            String symbol = "005930";
            RiskRule savedRule = RiskRule.builder()
                    .riskRuleId("RULE_003")
                    .scope(RiskRuleScope.PER_SYMBOL)
                    .accountId(accountId)
                    .symbol(symbol)
                    .build();

            when(riskRuleRepository.save(any())).thenReturn(savedRule);

            // When
            RiskRule result = updateRiskRuleUseCase.updateSymbolRule(
                    accountId, symbol,
                    BigDecimal.valueOf(3_000_000),
                    3, 2, null, null
            );

            // Then
            assertThat(result.getScope()).isEqualTo(RiskRuleScope.PER_SYMBOL);
            assertThat(result.getAccountId()).isEqualTo(accountId);
            assertThat(result.getSymbol()).isEqualTo(symbol);
        }

        @Test
        @DisplayName("Should throw exception when symbol is null")
        void shouldThrowExceptionWhenSymbolIsNull() {
            // When & Then
            assertThatThrownBy(() -> updateRiskRuleUseCase.updateSymbolRule(
                    "ACC_001", null, BigDecimal.TEN, 10, 5, null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Symbol cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Delete Rule Tests")
    class DeleteRuleTests {

        @Test
        @DisplayName("Should delete risk rule")
        void shouldDeleteRiskRule() {
            // Given
            String ruleId = "RULE_001";
            doNothing().when(riskRuleRepository).delete(ruleId);

            // When
            updateRiskRuleUseCase.deleteRule(ruleId);

            // Then
            verify(riskRuleRepository).delete(ruleId);
        }

        @Test
        @DisplayName("Should publish RiskRuleDeleted event")
        void shouldPublishRiskRuleDeletedEvent() {
            // Given
            String ruleId = "RULE_001";
            doNothing().when(riskRuleRepository).delete(ruleId);

            // When
            updateRiskRuleUseCase.deleteRule(ruleId);

            // Then
            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxService).save(eventCaptor.capture());

            OutboxEvent event = eventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo("RiskRuleDeleted");
            assertThat(event.getPayload()).containsEntry("riskRuleId", ruleId);
        }
    }
}
