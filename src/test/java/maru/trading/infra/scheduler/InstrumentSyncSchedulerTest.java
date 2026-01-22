package maru.trading.infra.scheduler;

import maru.trading.application.usecase.instrument.SyncInstrumentsUseCase;
import maru.trading.application.usecase.instrument.SyncInstrumentsUseCase.SyncResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InstrumentSyncScheduler Test")
class InstrumentSyncSchedulerTest {

    @Mock
    private SyncInstrumentsUseCase syncInstrumentsUseCase;

    private InstrumentSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new InstrumentSyncScheduler(syncInstrumentsUseCase, "KOSPI,KOSDAQ");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should parse markets from comma-separated string")
        void shouldParseMarketsFromCommaSeparatedString() {
            // When
            InstrumentSyncScheduler scheduler = new InstrumentSyncScheduler(
                    syncInstrumentsUseCase, "KOSPI,KOSDAQ,KONEX");

            // Then - no exception should be thrown
            assertThat(scheduler).isNotNull();
        }

        @Test
        @DisplayName("Should handle single market")
        void shouldHandleSingleMarket() {
            // When
            InstrumentSyncScheduler scheduler = new InstrumentSyncScheduler(
                    syncInstrumentsUseCase, "KOSPI");

            // Then - no exception should be thrown
            assertThat(scheduler).isNotNull();
        }
    }

    @Nested
    @DisplayName("syncInstruments() Tests")
    class SyncInstrumentsTests {

        @Test
        @DisplayName("Should call usecase with configured markets")
        void shouldCallUsecaseWithConfiguredMarkets() {
            // Given
            SyncResult result = new SyncResult(100, 5, 2);
            when(syncInstrumentsUseCase.syncByMarkets(anyList())).thenReturn(result);

            // When
            scheduler.syncInstruments();

            // Then
            verify(syncInstrumentsUseCase).syncByMarkets(List.of("KOSPI", "KOSDAQ"));
        }

        @Test
        @DisplayName("Should handle sync failure gracefully")
        void shouldHandleSyncFailureGracefully() {
            // Given
            when(syncInstrumentsUseCase.syncByMarkets(anyList()))
                    .thenThrow(new RuntimeException("Network error"));

            // When - should not throw exception
            scheduler.syncInstruments();

            // Then
            verify(syncInstrumentsUseCase).syncByMarkets(anyList());
        }

        @Test
        @DisplayName("Should complete sync successfully")
        void shouldCompleteSyncSuccessfully() {
            // Given
            SyncResult result = new SyncResult(500, 10, 0);
            when(syncInstrumentsUseCase.syncByMarkets(anyList())).thenReturn(result);

            // When
            scheduler.syncInstruments();

            // Then
            verify(syncInstrumentsUseCase, times(1)).syncByMarkets(anyList());
        }
    }

    @Nested
    @DisplayName("triggerSync() Tests")
    class TriggerSyncTests {

        @Test
        @DisplayName("Should call sync and return result")
        void shouldCallSyncAndReturnResult() {
            // Given
            SyncResult expectedResult = new SyncResult(100, 5, 2);
            when(syncInstrumentsUseCase.syncByMarkets(anyList())).thenReturn(expectedResult);

            // When
            SyncResult result = scheduler.triggerSync();

            // Then
            assertThat(result).isEqualTo(expectedResult);
            // Called twice: once in syncInstruments, once for return value
            verify(syncInstrumentsUseCase, times(2)).syncByMarkets(List.of("KOSPI", "KOSDAQ"));
        }
    }
}
