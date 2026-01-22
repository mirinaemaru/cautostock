package maru.trading.application.usecase.instrument;

import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateInstrumentStatusUseCase Test")
class UpdateInstrumentStatusUseCaseTest {

    @Mock
    private InstrumentJpaRepository instrumentRepository;

    @InjectMocks
    private UpdateInstrumentStatusUseCase updateInstrumentStatusUseCase;

    @Test
    @DisplayName("Should update instrument status successfully")
    void shouldUpdateInstrumentStatusSuccessfully() {
        // Given
        String symbol = "005930";
        InstrumentEntity instrument = InstrumentEntity.builder()
                .symbol(symbol)
                .nameKr("삼성전자")
                .status("LISTED")
                .tradable(true)
                .halted(false)
                .build();

        when(instrumentRepository.findById(symbol)).thenReturn(Optional.of(instrument));

        // When
        updateInstrumentStatusUseCase.execute(symbol, "SUSPENDED", false, true);

        // Then
        verify(instrumentRepository).save(instrument);
    }

    @Test
    @DisplayName("Should throw exception when instrument not found")
    void shouldThrowExceptionWhenInstrumentNotFound() {
        // Given
        String symbol = "999999";
        when(instrumentRepository.findById(symbol)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> updateInstrumentStatusUseCase.execute(symbol, "SUSPENDED", false, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Instrument not found");
    }

    @Test
    @DisplayName("Should update only tradable flag")
    void shouldUpdateOnlyTradableFlag() {
        // Given
        String symbol = "005930";
        InstrumentEntity instrument = InstrumentEntity.builder()
                .symbol(symbol)
                .nameKr("삼성전자")
                .status("LISTED")
                .tradable(true)
                .halted(false)
                .build();

        when(instrumentRepository.findById(symbol)).thenReturn(Optional.of(instrument));

        // When
        updateInstrumentStatusUseCase.execute(symbol, null, false, null);

        // Then
        verify(instrumentRepository).save(instrument);
    }

    @Test
    @DisplayName("Should update only halted flag")
    void shouldUpdateOnlyHaltedFlag() {
        // Given
        String symbol = "005930";
        InstrumentEntity instrument = InstrumentEntity.builder()
                .symbol(symbol)
                .nameKr("삼성전자")
                .status("LISTED")
                .tradable(true)
                .halted(false)
                .build();

        when(instrumentRepository.findById(symbol)).thenReturn(Optional.of(instrument));

        // When
        updateInstrumentStatusUseCase.execute(symbol, null, null, true);

        // Then
        verify(instrumentRepository).save(instrument);
    }
}
