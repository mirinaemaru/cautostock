package maru.trading.application.scheduler;

import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.application.usecase.strategy.ExecuteStrategyUseCase;
import maru.trading.domain.shared.Environment;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.Strategy;
import maru.trading.infra.persistence.jpa.entity.StrategySymbolEntity;
import maru.trading.infra.persistence.jpa.repository.StrategySymbolJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StrategyScheduler.
 *
 * Tests the scheduler logic in isolation using mocks.
 * Verifies:
 * - Active strategies are fetched and executed
 * - Strategy-symbol mappings are used correctly
 * - Fallback to default symbol when no mappings exist
 * - Error handling for individual strategy failures
 * - Manual trigger functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StrategyScheduler Unit Tests")
class StrategySchedulerTest {

    @Mock
    private StrategyRepository strategyRepository;

    @Mock
    private StrategySymbolJpaRepository strategySymbolRepository;

    @Mock
    private ExecuteStrategyUseCase executeStrategyUseCase;

    @InjectMocks
    private StrategyScheduler strategyScheduler;

    @Captor
    private ArgumentCaptor<String> strategyIdCaptor;

    @Captor
    private ArgumentCaptor<String> symbolCaptor;

    @Captor
    private ArgumentCaptor<String> accountIdCaptor;

    private Strategy activeStrategy1;
    private Strategy activeStrategy2;
    private Strategy inactiveStrategy;

    @BeforeEach
    void setUp() {
        activeStrategy1 = Strategy.builder()
                .strategyId("STR_001")
                .name("MA Crossover")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId("VER_001")
                .build();

        activeStrategy2 = Strategy.builder()
                .strategyId("STR_002")
                .name("RSI Strategy")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId("VER_002")
                .build();

        inactiveStrategy = Strategy.builder()
                .strategyId("STR_003")
                .name("Inactive Strategy")
                .status("STOPPED")
                .mode(Environment.PAPER)
                .activeVersionId("VER_003")
                .build();
    }

    @Nested
    @DisplayName("executeStrategies() - Scheduled Execution")
    class ExecuteStrategiesTests {

        @Test
        @DisplayName("Should skip execution when no active strategies exist")
        void shouldSkipWhenNoActiveStrategies() {
            // Given
            given(strategyRepository.findActiveStrategies()).willReturn(Collections.emptyList());

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(strategyRepository).findActiveStrategies();
            verify(executeStrategyUseCase, never()).execute(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should execute all active strategies")
        void shouldExecuteAllActiveStrategies() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1, activeStrategy2);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);
            given(strategySymbolRepository.findActiveByStrategyId(anyString())).willReturn(Collections.emptyList());

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(strategyRepository).findActiveStrategies();
            verify(executeStrategyUseCase, times(2)).execute(
                    strategyIdCaptor.capture(),
                    eq("005930"), // Default symbol
                    eq("ACC_DEMO_001") // Default account
            );

            List<String> executedStrategies = strategyIdCaptor.getAllValues();
            assertThat(executedStrategies).containsExactly("STR_001", "STR_002");
        }

        @Test
        @DisplayName("Should use strategy-symbol mappings when available")
        void shouldUseStrategySymbolMappings() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);

            List<StrategySymbolEntity> mappings = List.of(
                    createStrategySymbolEntity("STR_001", "005930", "ACC_001"),
                    createStrategySymbolEntity("STR_001", "035420", "ACC_001"),
                    createStrategySymbolEntity("STR_001", "000660", "ACC_002")
            );
            given(strategySymbolRepository.findActiveByStrategyId("STR_001")).willReturn(mappings);

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(executeStrategyUseCase, times(3)).execute(
                    strategyIdCaptor.capture(),
                    symbolCaptor.capture(),
                    accountIdCaptor.capture()
            );

            assertThat(strategyIdCaptor.getAllValues()).containsOnly("STR_001");
            assertThat(symbolCaptor.getAllValues()).containsExactly("005930", "035420", "000660");
            assertThat(accountIdCaptor.getAllValues()).containsExactly("ACC_001", "ACC_001", "ACC_002");
        }

        @Test
        @DisplayName("Should use default symbol when no mappings exist")
        void shouldUseDefaultSymbolWhenNoMappings() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);
            given(strategySymbolRepository.findActiveByStrategyId("STR_001")).willReturn(Collections.emptyList());

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(executeStrategyUseCase).execute("STR_001", "005930", "ACC_DEMO_001");
        }

        @Test
        @DisplayName("Should continue with next strategy when one fails")
        void shouldContinueWhenStrategyFails() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1, activeStrategy2);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);
            given(strategySymbolRepository.findActiveByStrategyId(anyString())).willReturn(Collections.emptyList());

            // First strategy throws exception
            doThrow(new RuntimeException("Strategy execution failed"))
                    .when(executeStrategyUseCase).execute(eq("STR_001"), anyString(), anyString());

            // When
            strategyScheduler.executeStrategies();

            // Then - Second strategy should still be executed
            verify(executeStrategyUseCase).execute(eq("STR_001"), anyString(), anyString());
            verify(executeStrategyUseCase).execute(eq("STR_002"), anyString(), anyString());
        }

        @Test
        @DisplayName("Should continue with next symbol when one symbol fails")
        void shouldContinueWhenSymbolFails() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);

            List<StrategySymbolEntity> mappings = List.of(
                    createStrategySymbolEntity("STR_001", "005930", "ACC_001"),
                    createStrategySymbolEntity("STR_001", "035420", "ACC_001")
            );
            given(strategySymbolRepository.findActiveByStrategyId("STR_001")).willReturn(mappings);

            // First symbol throws exception
            doThrow(new RuntimeException("Symbol execution failed"))
                    .when(executeStrategyUseCase).execute(eq("STR_001"), eq("005930"), anyString());

            // When
            strategyScheduler.executeStrategies();

            // Then - Second symbol should still be executed
            verify(executeStrategyUseCase).execute("STR_001", "005930", "ACC_001");
            verify(executeStrategyUseCase).execute("STR_001", "035420", "ACC_001");
        }

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void shouldHandleRepositoryException() {
            // Given
            given(strategyRepository.findActiveStrategies())
                    .willThrow(new RuntimeException("Database connection failed"));

            // When & Then
            assertThatCode(() -> strategyScheduler.executeStrategies())
                    .doesNotThrowAnyException();

            verify(executeStrategyUseCase, never()).execute(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should process signal returned from execute")
        void shouldProcessSignalFromExecution() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);
            given(strategySymbolRepository.findActiveByStrategyId("STR_001")).willReturn(Collections.emptyList());

            Signal generatedSignal = Signal.builder()
                    .signalId("SIG_001")
                    .strategyId("STR_001")
                    .symbol("005930")
                    .signalType(SignalType.BUY)
                    .targetValue(BigDecimal.valueOf(10))
                    .build();

            given(executeStrategyUseCase.execute("STR_001", "005930", "ACC_DEMO_001"))
                    .willReturn(generatedSignal);

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(executeStrategyUseCase).execute("STR_001", "005930", "ACC_DEMO_001");
        }
    }

    @Nested
    @DisplayName("triggerManually() - Manual Execution")
    class TriggerManuallyTests {

        @Test
        @DisplayName("Should execute strategy with provided parameters")
        void shouldExecuteWithProvidedParameters() {
            // Given
            String strategyId = "STR_MANUAL";
            String symbol = "066570";
            String accountId = "ACC_LIVE_001";

            Signal expectedSignal = Signal.builder()
                    .signalId("SIG_MANUAL")
                    .strategyId(strategyId)
                    .symbol(symbol)
                    .signalType(SignalType.SELL)
                    .build();

            given(executeStrategyUseCase.execute(strategyId, symbol, accountId))
                    .willReturn(expectedSignal);

            // When
            strategyScheduler.triggerManually(strategyId, symbol, accountId);

            // Then
            verify(executeStrategyUseCase).execute(strategyId, symbol, accountId);
        }

        @Test
        @DisplayName("Should propagate exception from manual trigger")
        void shouldPropagateExceptionFromManualTrigger() {
            // Given
            String strategyId = "STR_ERROR";
            String symbol = "005930";
            String accountId = "ACC_001";

            doThrow(new IllegalArgumentException("Strategy not found"))
                    .when(executeStrategyUseCase).execute(strategyId, symbol, accountId);

            // When & Then
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> strategyScheduler.triggerManually(strategyId, symbol, accountId)
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single strategy with single symbol")
        void shouldHandleSingleStrategyWithSingleSymbol() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);

            List<StrategySymbolEntity> mappings = List.of(
                    createStrategySymbolEntity("STR_001", "005930", "ACC_001")
            );
            given(strategySymbolRepository.findActiveByStrategyId("STR_001")).willReturn(mappings);

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(executeStrategyUseCase, times(1)).execute("STR_001", "005930", "ACC_001");
        }

        @Test
        @DisplayName("Should handle multiple strategies with different symbol configurations")
        void shouldHandleMultipleStrategiesWithDifferentConfigurations() {
            // Given
            List<Strategy> activeStrategies = List.of(activeStrategy1, activeStrategy2);
            given(strategyRepository.findActiveStrategies()).willReturn(activeStrategies);

            // Strategy 1 has mappings
            List<StrategySymbolEntity> mappings1 = List.of(
                    createStrategySymbolEntity("STR_001", "005930", "ACC_001")
            );
            given(strategySymbolRepository.findActiveByStrategyId("STR_001")).willReturn(mappings1);

            // Strategy 2 has no mappings (use default)
            given(strategySymbolRepository.findActiveByStrategyId("STR_002")).willReturn(Collections.emptyList());

            // When
            strategyScheduler.executeStrategies();

            // Then
            verify(executeStrategyUseCase).execute("STR_001", "005930", "ACC_001");
            verify(executeStrategyUseCase).execute("STR_002", "005930", "ACC_DEMO_001");
        }
    }

    // Helper methods
    private StrategySymbolEntity createStrategySymbolEntity(String strategyId, String symbol, String accountId) {
        return StrategySymbolEntity.builder()
                .strategySymbolId("SS_" + System.nanoTime())
                .strategyId(strategyId)
                .symbol(symbol)
                .accountId(accountId)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
