package maru.trading.application.usecase.instrument;

import maru.trading.broker.kis.api.KrxInstrumentClient;
import maru.trading.broker.kis.api.KrxInstrumentClient.KrxInstrument;
import maru.trading.infra.persistence.jpa.entity.InstrumentEntity;
import maru.trading.infra.persistence.jpa.repository.InstrumentJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncInstrumentsUseCase Test")
class SyncInstrumentsUseCaseTest {

    @Mock
    private KrxInstrumentClient krxClient;

    @Mock
    private InstrumentJpaRepository instrumentRepository;

    @InjectMocks
    private SyncInstrumentsUseCase syncInstrumentsUseCase;

    private KrxInstrument createKrxInstrument(String symbol, String name, String market) {
        KrxInstrument instrument = new KrxInstrument();
        instrument.setSymbol(symbol);
        instrument.setNameKr(name);
        instrument.setMarket(market);
        return instrument;
    }

    @Nested
    @DisplayName("SyncAll Tests")
    class SyncAllTests {

        @Test
        @DisplayName("Should sync all instruments")
        void shouldSyncAllInstruments() {
            // Given
            List<KrxInstrument> instruments = List.of(
                    createKrxInstrument("005930", "삼성전자", "KOSPI"),
                    createKrxInstrument("000660", "SK하이닉스", "KOSPI")
            );

            when(krxClient.fetchAllInstruments()).thenReturn(instruments);
            when(instrumentRepository.findById(any())).thenReturn(Optional.empty());

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncAll();

            // Then
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.inserted()).isEqualTo(2);
            assertThat(result.updated()).isEqualTo(0);
            verify(instrumentRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Should update existing instruments")
        void shouldUpdateExistingInstruments() {
            // Given
            KrxInstrument krxInstrument = createKrxInstrument("005930", "삼성전자(변경)", "KOSPI");
            InstrumentEntity existingEntity = InstrumentEntity.builder()
                    .symbol("005930")
                    .nameKr("삼성전자")
                    .market("KOSPI")
                    .build();

            when(krxClient.fetchAllInstruments()).thenReturn(List.of(krxInstrument));
            when(instrumentRepository.findById("005930")).thenReturn(Optional.of(existingEntity));

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncAll();

            // Then
            assertThat(result.total()).isEqualTo(1);
            assertThat(result.updated()).isEqualTo(1);
            assertThat(result.inserted()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle empty instrument list")
        void shouldHandleEmptyInstrumentList() {
            // Given
            when(krxClient.fetchAllInstruments()).thenReturn(Collections.emptyList());

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncAll();

            // Then
            assertThat(result.total()).isEqualTo(0);
            assertThat(result.inserted()).isEqualTo(0);
            assertThat(result.updated()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("SyncKospi Tests")
    class SyncKospiTests {

        @Test
        @DisplayName("Should sync KOSPI instruments")
        void shouldSyncKospiInstruments() {
            // Given
            List<KrxInstrument> instruments = List.of(
                    createKrxInstrument("005930", "삼성전자", "KOSPI")
            );

            when(krxClient.fetchKospiInstruments()).thenReturn(instruments);
            when(instrumentRepository.findById(any())).thenReturn(Optional.empty());

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncKospi();

            // Then
            assertThat(result.total()).isEqualTo(1);
            verify(krxClient).fetchKospiInstruments();
        }

        @Test
        @DisplayName("Should fallback to Naver API when KRX fails")
        void shouldFallbackToNaverApiWhenKrxFails() {
            // Given
            when(krxClient.fetchKospiInstruments()).thenReturn(Collections.emptyList());
            when(krxClient.fetchInstrumentsFromNaver("KOSPI")).thenReturn(List.of(
                    createKrxInstrument("005930", "삼성전자", "KOSPI")
            ));
            when(instrumentRepository.findById(any())).thenReturn(Optional.empty());

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncKospi();

            // Then
            assertThat(result.total()).isEqualTo(1);
            verify(krxClient).fetchInstrumentsFromNaver("KOSPI");
        }
    }

    @Nested
    @DisplayName("SyncKosdaq Tests")
    class SyncKosdaqTests {

        @Test
        @DisplayName("Should sync KOSDAQ instruments")
        void shouldSyncKosdaqInstruments() {
            // Given
            List<KrxInstrument> instruments = List.of(
                    createKrxInstrument("035720", "카카오", "KOSDAQ")
            );

            when(krxClient.fetchKosdaqInstruments()).thenReturn(instruments);
            when(instrumentRepository.findById(any())).thenReturn(Optional.empty());

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncKosdaq();

            // Then
            assertThat(result.total()).isEqualTo(1);
            verify(krxClient).fetchKosdaqInstruments();
        }
    }

    @Nested
    @DisplayName("SyncByMarkets Tests")
    class SyncByMarketsTests {

        @Test
        @DisplayName("Should sync multiple markets")
        void shouldSyncMultipleMarkets() {
            // Given
            when(krxClient.fetchKospiInstruments()).thenReturn(List.of(
                    createKrxInstrument("005930", "삼성전자", "KOSPI")
            ));
            when(krxClient.fetchKosdaqInstruments()).thenReturn(List.of(
                    createKrxInstrument("035720", "카카오", "KOSDAQ")
            ));
            when(instrumentRepository.findById(any())).thenReturn(Optional.empty());

            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncByMarkets(
                    List.of("KOSPI", "KOSDAQ")
            );

            // Then
            assertThat(result.total()).isEqualTo(2);
            assertThat(result.inserted()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should skip unknown market")
        void shouldSkipUnknownMarket() {
            // Given
            // When
            SyncInstrumentsUseCase.SyncResult result = syncInstrumentsUseCase.syncByMarkets(
                    List.of("UNKNOWN")
            );

            // Then
            assertThat(result.total()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("SyncResult Tests")
    class SyncResultTests {

        @Test
        @DisplayName("Should generate correct message")
        void shouldGenerateCorrectMessage() {
            // Given
            SyncInstrumentsUseCase.SyncResult result = new SyncInstrumentsUseCase.SyncResult(10, 5, 15);

            // When
            String message = result.toMessage();

            // Then
            assertThat(message).isEqualTo("Synced 15 instruments (new: 10, updated: 5)");
        }
    }
}
