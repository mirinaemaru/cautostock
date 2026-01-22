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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListInstrumentsUseCase Test")
class ListInstrumentsUseCaseTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private ListInstrumentsUseCase listInstrumentsUseCase;

    @Test
    @DisplayName("Should return all instruments")
    void shouldReturnAllInstruments() {
        // Given
        List<Instrument> instruments = List.of(
                new Instrument("005930", "KOSPI", true, false, BigDecimal.ONE, 1),
                new Instrument("000660", "KOSPI", true, false, BigDecimal.ONE, 1)
        );
        when(instrumentRepository.findAll()).thenReturn(instruments);

        // When
        List<Instrument> result = listInstrumentsUseCase.execute();

        // Then
        assertThat(result).hasSize(2);
        verify(instrumentRepository).findAll();
    }

    @Test
    @DisplayName("Should return instruments by market")
    void shouldReturnInstrumentsByMarket() {
        // Given
        String market = "KOSPI";
        List<Instrument> instruments = List.of(
                new Instrument("005930", market, true, false, BigDecimal.ONE, 1)
        );
        when(instrumentRepository.findByMarket(market)).thenReturn(instruments);

        // When
        List<Instrument> result = listInstrumentsUseCase.executeByMarket(market);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMarket()).isEqualTo(market);
        verify(instrumentRepository).findByMarket(market);
    }

    @Test
    @DisplayName("Should return tradable instruments only")
    void shouldReturnTradableInstrumentsOnly() {
        // Given
        List<Instrument> instruments = List.of(
                new Instrument("005930", "KOSPI", true, false, BigDecimal.ONE, 1)
        );
        when(instrumentRepository.findTradable()).thenReturn(instruments);

        // When
        List<Instrument> result = listInstrumentsUseCase.executeTradableOnly();

        // Then
        assertThat(result).hasSize(1);
        verify(instrumentRepository).findTradable();
    }

    @Test
    @DisplayName("Should return instruments by status")
    void shouldReturnInstrumentsByStatus() {
        // Given
        String status = "SUSPENDED";
        List<Instrument> instruments = List.of(
                new Instrument("999999", "KOSPI", false, true, BigDecimal.ONE, 1)
        );
        when(instrumentRepository.findByStatus(status)).thenReturn(instruments);

        // When
        List<Instrument> result = listInstrumentsUseCase.executeByStatus(status);

        // Then
        assertThat(result).hasSize(1);
        verify(instrumentRepository).findByStatus(status);
    }

    @Test
    @DisplayName("Should search instruments by keyword")
    void shouldSearchInstrumentsByKeyword() {
        // Given
        String keyword = "삼성";
        List<Instrument> instruments = List.of(
                new Instrument("005930", "KOSPI", true, false, BigDecimal.ONE, 1),
                new Instrument("006400", "KOSPI", true, false, BigDecimal.ONE, 1)
        );
        when(instrumentRepository.searchByName(keyword)).thenReturn(instruments);

        // When
        List<Instrument> result = listInstrumentsUseCase.executeSearch(keyword);

        // Then
        assertThat(result).hasSize(2);
        verify(instrumentRepository).searchByName(keyword);
    }
}
