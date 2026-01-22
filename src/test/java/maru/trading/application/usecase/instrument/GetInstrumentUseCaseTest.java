package maru.trading.application.usecase.instrument;

import maru.trading.application.ports.repo.InstrumentRepository;
import maru.trading.domain.market.Instrument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetInstrumentUseCase Test")
class GetInstrumentUseCaseTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private GetInstrumentUseCase getInstrumentUseCase;

    @Test
    @DisplayName("Should return instrument when found")
    void shouldReturnInstrumentWhenFound() {
        // Given
        String symbol = "005930";
        Instrument instrument = new Instrument(
                symbol,
                "KOSPI",
                true,
                false,
                BigDecimal.ONE,
                1
        );
        when(instrumentRepository.findBySymbol(symbol)).thenReturn(Optional.of(instrument));

        // When
        Optional<Instrument> result = getInstrumentUseCase.execute(symbol);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSymbol()).isEqualTo(symbol);
        assertThat(result.get().getMarket()).isEqualTo("KOSPI");
        verify(instrumentRepository).findBySymbol(symbol);
    }

    @Test
    @DisplayName("Should return empty when not found")
    void shouldReturnEmptyWhenNotFound() {
        // Given
        String symbol = "999999";
        when(instrumentRepository.findBySymbol(symbol)).thenReturn(Optional.empty());

        // When
        Optional<Instrument> result = getInstrumentUseCase.execute(symbol);

        // Then
        assertThat(result).isEmpty();
        verify(instrumentRepository).findBySymbol(symbol);
    }
}
