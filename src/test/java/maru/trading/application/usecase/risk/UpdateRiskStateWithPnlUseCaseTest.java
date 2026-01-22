package maru.trading.application.usecase.risk;

import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.risk.KillSwitchStatus;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.domain.risk.RiskState;
import maru.trading.domain.risk.OrderFrequencyTracker;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRiskStateWithPnlUseCase Test")
class UpdateRiskStateWithPnlUseCaseTest {

    @Mock
    private RiskStateRepository riskStateRepository;

    @Mock
    private RiskRuleRepository riskRuleRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UpdateRiskStateWithPnlUseCase updateRiskStateWithPnlUseCase;

    private RiskState testRiskState;
    private RiskRule testRiskRule;

    @BeforeEach
    void setUp() {
        testRiskState = RiskState.builder()
                .scope("ACCOUNT")
                .accountId("ACC_001")
                .killSwitchStatus(KillSwitchStatus.OFF)
                .dailyPnl(BigDecimal.ZERO)
                .exposure(BigDecimal.ZERO)
                .consecutiveOrderFailures(0)
                .openOrderCount(0)
                .orderFrequencyTracker(new OrderFrequencyTracker())
                .build();

        testRiskRule = RiskRule.builder()
                .riskRuleId("RULE_001")
                .scope(RiskRuleScope.GLOBAL)
                .dailyLossLimit(BigDecimal.valueOf(1_000_000))
                .maxPositionValuePerSymbol(BigDecimal.valueOf(100_000_000))
                .consecutiveOrderFailuresLimit(5)
                .build();
    }

    @Nested
    @DisplayName("Execute Tests")
    class ExecuteTests {

        @Test
        @DisplayName("Should update daily PnL with positive delta")
        void shouldUpdateDailyPnlWithPositiveDelta() {
            // Given
            String accountId = "ACC_001";
            BigDecimal pnlDelta = BigDecimal.valueOf(50000);

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, pnlDelta);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDailyPnl()).isEqualTo(BigDecimal.valueOf(50000));
            assertThat(result.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
            verify(riskStateRepository).save(any());
            verify(outboxService, never()).save(any());
        }

        @Test
        @DisplayName("Should update daily PnL with negative delta")
        void shouldUpdateDailyPnlWithNegativeDelta() {
            // Given
            String accountId = "ACC_001";
            BigDecimal pnlDelta = BigDecimal.valueOf(-30000);

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, pnlDelta);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDailyPnl()).isEqualTo(BigDecimal.valueOf(-30000));
            assertThat(result.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
        }

        @Test
        @DisplayName("Should create new risk state when not exists")
        void shouldCreateNewRiskStateWhenNotExists() {
            // Given
            String accountId = "NEW_ACC";

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, BigDecimal.valueOf(10000));

            // Then
            assertThat(result).isNotNull();
            verify(riskStateRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should use default rule when no rule found")
        void shouldUseDefaultRuleWhenNoRuleFound() {
            // Given
            String accountId = "ACC_001";

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.empty());
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, BigDecimal.valueOf(10000));

            // Then
            assertThat(result).isNotNull();
            verify(riskStateRepository).save(any());
        }
    }

    @Nested
    @DisplayName("Kill Switch Trigger Tests")
    class KillSwitchTriggerTests {

        @Test
        @DisplayName("Should trigger kill switch when daily loss limit exceeded")
        void shouldTriggerKillSwitchWhenDailyLossLimitExceeded() {
            // Given
            String accountId = "ACC_001";
            BigDecimal hugeLoss = BigDecimal.valueOf(-1_500_000); // Exceeds 1M limit

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, hugeLoss);

            // Then
            assertThat(result.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.ON);
            verify(outboxService).save(any());
        }

        @Test
        @DisplayName("Should publish KillSwitchTriggered event")
        void shouldPublishKillSwitchTriggeredEvent() {
            // Given
            String accountId = "ACC_001";
            BigDecimal hugeLoss = BigDecimal.valueOf(-2_000_000);

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            updateRiskStateWithPnlUseCase.execute(accountId, hugeLoss);

            // Then
            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxService).save(eventCaptor.capture());

            OutboxEvent event = eventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo("KillSwitchTriggered");
            assertThat(event.getPayload()).containsEntry("accountId", accountId);
            assertThat(event.getPayload()).containsKey("reason");
        }

        @Test
        @DisplayName("Should not trigger kill switch when already ON")
        void shouldNotTriggerKillSwitchWhenAlreadyOn() {
            // Given
            String accountId = "ACC_001";
            testRiskState = RiskState.builder()
                    .scope("ACCOUNT")
                    .accountId(accountId)
                    .killSwitchStatus(KillSwitchStatus.ON)
                    .dailyPnl(BigDecimal.valueOf(-500_000))
                    .exposure(BigDecimal.ZERO)
                    .consecutiveOrderFailures(0)
                    .openOrderCount(0)
                    .orderFrequencyTracker(new OrderFrequencyTracker())
                    .build();

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            updateRiskStateWithPnlUseCase.execute(accountId, BigDecimal.valueOf(-600_000));

            // Then
            verify(outboxService, never()).save(any());
        }

        @Test
        @DisplayName("Should not trigger kill switch when within limits")
        void shouldNotTriggerKillSwitchWhenWithinLimits() {
            // Given
            String accountId = "ACC_001";
            BigDecimal moderateLoss = BigDecimal.valueOf(-500_000); // Within 1M limit

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, moderateLoss);

            // Then
            assertThat(result.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
            verify(outboxService, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Cumulative PnL Tests")
    class CumulativePnlTests {

        @Test
        @DisplayName("Should accumulate PnL across multiple updates")
        void shouldAccumulatePnlAcrossMultipleUpdates() {
            // Given
            String accountId = "ACC_001";
            testRiskState = RiskState.builder()
                    .scope("ACCOUNT")
                    .accountId(accountId)
                    .killSwitchStatus(KillSwitchStatus.OFF)
                    .dailyPnl(BigDecimal.valueOf(100_000)) // Already has some PnL
                    .exposure(BigDecimal.ZERO)
                    .consecutiveOrderFailures(0)
                    .openOrderCount(0)
                    .orderFrequencyTracker(new OrderFrequencyTracker())
                    .build();

            when(riskStateRepository.findByAccountId(accountId)).thenReturn(Optional.of(testRiskState));
            when(riskRuleRepository.findApplicableRule(accountId, null)).thenReturn(Optional.of(testRiskRule));
            when(riskStateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            RiskState result = updateRiskStateWithPnlUseCase.execute(accountId, BigDecimal.valueOf(50_000));

            // Then
            assertThat(result.getDailyPnl()).isEqualTo(BigDecimal.valueOf(150_000));
        }
    }
}
