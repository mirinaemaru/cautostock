package maru.trading.application.usecase.strategy;

import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyContext;
import maru.trading.domain.strategy.StrategyVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteStrategyUseCase Test")
class ExecuteStrategyUseCaseTest {

    @Mock
    private StrategyRepository strategyRepository;

    @Mock
    private LoadStrategyContextUseCase loadContextUseCase;

    @Mock
    private GenerateSignalUseCase generateSignalUseCase;

    @InjectMocks
    private ExecuteStrategyUseCase executeStrategyUseCase;

    @Test
    @DisplayName("Should return null when strategy not found")
    void shouldReturnNullWhenStrategyNotFound() {
        // Given
        when(strategyRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

        // When
        Signal result = executeStrategyUseCase.execute("NON_EXISTENT", "005930", "ACC_001");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when strategy is not active")
    void shouldReturnNullWhenStrategyNotActive() {
        // Given
        Strategy inactiveStrategy = Strategy.builder()
                .strategyId("STR_001")
                .name("Inactive Strategy")
                .status("INACTIVE")
                .build();

        when(strategyRepository.findById("STR_001")).thenReturn(Optional.of(inactiveStrategy));

        // When
        Signal result = executeStrategyUseCase.execute("STR_001", "005930", "ACC_001");

        // Then
        assertThat(result).isNull();
        verify(loadContextUseCase, never()).execute(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should return null when strategy version not found")
    void shouldReturnNullWhenVersionNotFound() {
        // Given
        Strategy strategy = Strategy.builder()
                .strategyId("STR_001")
                .name("MA Crossover")
                .status("ACTIVE")
                .activeVersionId("VER_001")
                .build();

        when(strategyRepository.findById("STR_001")).thenReturn(Optional.of(strategy));
        when(strategyRepository.findVersionById("VER_001")).thenReturn(Optional.empty());

        // When
        Signal result = executeStrategyUseCase.execute("STR_001", "005930", "ACC_001");

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle exception gracefully")
    void shouldHandleExceptionGracefully() {
        // Given
        Strategy strategy = Strategy.builder()
                .strategyId("STR_001")
                .name("MA Crossover")
                .status("ACTIVE")
                .activeVersionId("VER_001")
                .build();

        StrategyVersion version = StrategyVersion.builder()
                .strategyVersionId("VER_001")
                .strategyId("STR_001")
                .build();

        when(strategyRepository.findById("STR_001")).thenReturn(Optional.of(strategy));
        when(strategyRepository.findVersionById("VER_001")).thenReturn(Optional.of(version));
        when(loadContextUseCase.execute(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Test exception"));

        // When
        Signal result = executeStrategyUseCase.execute("STR_001", "005930", "ACC_001");

        // Then
        assertThat(result).isNull();
    }
}
