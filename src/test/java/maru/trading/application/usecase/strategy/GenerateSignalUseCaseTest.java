package maru.trading.application.usecase.strategy;

import maru.trading.application.ports.repo.SignalRepository;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalPolicy;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyVersion;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateSignalUseCase Test")
class GenerateSignalUseCaseTest {

    @Mock
    private SignalRepository signalRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private SignalPolicy signalPolicy;

    @InjectMocks
    private GenerateSignalUseCase generateSignalUseCase;

    private Strategy testStrategy;
    private StrategyVersion testVersion;
    private SignalDecision buyDecision;

    @BeforeEach
    void setUp() {
        testStrategy = Strategy.builder()
                .strategyId("STR_001")
                .name("MA Crossover")
                .status("ACTIVE")
                .build();

        testVersion = StrategyVersion.builder()
                .strategyVersionId("VER_001")
                .strategyId("STR_001")
                .build();

        buyDecision = SignalDecision.builder()
                .signalType(SignalType.BUY)
                .targetType("QTY")
                .targetValue(BigDecimal.valueOf(10))
                .reason("Golden cross detected")
                .build();
    }

    @Test
    @DisplayName("Should return null for HOLD signal")
    void shouldReturnNullForHoldSignal() {
        // Given
        SignalDecision holdDecision = SignalDecision.builder()
                .signalType(SignalType.HOLD)
                .reason("No signal")
                .build();

        doNothing().when(signalPolicy).validateSignal(holdDecision);

        // When
        Signal result = generateSignalUseCase.execute(
                holdDecision, testStrategy, testVersion, "005930", "ACC_001");

        // Then
        assertThat(result).isNull();
        verify(signalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate and save signal for BUY decision")
    void shouldGenerateAndSaveSignalForBuyDecision() {
        // Given
        doNothing().when(signalPolicy).validateSignal(buyDecision);
        when(signalRepository.findRecentSignals(any(), any(), any())).thenReturn(Collections.emptyList());
        when(signalPolicy.isDuplicate(any(), any(), anyInt(), any())).thenReturn(false);
        when(ulidGenerator.generateInstance()).thenReturn("SIG_001");

        Signal savedSignal = Signal.builder()
                .signalId("SIG_001")
                .strategyId("STR_001")
                .symbol("005930")
                .signalType(SignalType.BUY)
                .build();

        when(signalRepository.save(any())).thenReturn(savedSignal);

        // When
        Signal result = generateSignalUseCase.execute(
                buyDecision, testStrategy, testVersion, "005930", "ACC_001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSignalId()).isEqualTo("SIG_001");
        assertThat(result.getSignalType()).isEqualTo(SignalType.BUY);
        verify(signalRepository).save(any());
    }

    @Test
    @DisplayName("Should skip duplicate signal")
    void shouldSkipDuplicateSignal() {
        // Given
        doNothing().when(signalPolicy).validateSignal(buyDecision);
        when(signalRepository.findRecentSignals(any(), any(), any())).thenReturn(Collections.emptyList());
        when(signalPolicy.isDuplicate(any(), any(), anyInt(), any())).thenReturn(true);

        // When
        Signal result = generateSignalUseCase.execute(
                buyDecision, testStrategy, testVersion, "005930", "ACC_001");

        // Then
        assertThat(result).isNull();
        verify(signalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should publish SignalGenerated event")
    void shouldPublishSignalGeneratedEvent() {
        // Given
        doNothing().when(signalPolicy).validateSignal(buyDecision);
        when(signalRepository.findRecentSignals(any(), any(), any())).thenReturn(Collections.emptyList());
        when(signalPolicy.isDuplicate(any(), any(), anyInt(), any())).thenReturn(false);
        when(ulidGenerator.generateInstance()).thenReturn("SIG_001", "EVENT_001");

        Signal savedSignal = Signal.builder()
                .signalId("SIG_001")
                .strategyId("STR_001")
                .strategyVersionId("VER_001")
                .symbol("005930")
                .signalType(SignalType.BUY)
                .targetValue(BigDecimal.valueOf(10))
                .reason("Golden cross")
                .build();

        when(signalRepository.save(any())).thenReturn(savedSignal);

        // When
        generateSignalUseCase.execute(buyDecision, testStrategy, testVersion, "005930", "ACC_001");

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("SignalGenerated");
        assertThat(event.getPayload()).containsEntry("signalId", "SIG_001");
        assertThat(event.getPayload()).containsEntry("signalType", "BUY");
    }
}
