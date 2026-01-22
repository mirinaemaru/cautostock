package maru.trading.application.usecase.execution;

import maru.trading.application.ports.repo.AccountRepository;
import maru.trading.application.ports.repo.PortfolioSnapshotRepository;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.broker.kis.api.KisBalanceApiClient;
import maru.trading.broker.kis.dto.KisBalanceResponse;
import maru.trading.domain.account.Account;
import maru.trading.domain.execution.PortfolioSnapshot;
import maru.trading.domain.execution.Position;
import maru.trading.infra.cache.MarketDataCache;
import maru.trading.infra.config.UlidGenerator;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculatePnlUseCase Test")
class CalculatePnlUseCaseTest {

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MarketDataCache marketDataCache;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private KisBalanceApiClient balanceApiClient;

    @InjectMocks
    private CalculatePnlUseCase calculatePnlUseCase;

    private Position testPosition;

    @BeforeEach
    void setUp() {
        testPosition = new Position(
                "POS_001",
                "ACC_001",
                "005930",
                100,
                BigDecimal.valueOf(70000),
                BigDecimal.ZERO
        );
    }

    @Nested
    @DisplayName("Execute with Cash Tests")
    class ExecuteWithCashTests {

        @Test
        @DisplayName("Should calculate PnL for account with positions")
        void shouldCalculatePnlForAccountWithPositions() {
            // Given
            String accountId = "ACC_001";
            BigDecimal cash = BigDecimal.valueOf(5_000_000);

            when(positionRepository.findAllByAccount(accountId)).thenReturn(List.of(testPosition));
            when(marketDataCache.getPrice("005930")).thenReturn(BigDecimal.valueOf(72000));
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            PortfolioSnapshot result = calculatePnlUseCase.execute(accountId, cash);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSnapshotId()).isEqualTo("SNAP_001");
            assertThat(result.getAccountId()).isEqualTo(accountId);
            verify(snapshotRepository).save(any());
        }

        @Test
        @DisplayName("Should handle empty positions")
        void shouldHandleEmptyPositions() {
            // Given
            String accountId = "ACC_001";
            BigDecimal cash = BigDecimal.valueOf(10_000_000);

            when(positionRepository.findAllByAccount(accountId)).thenReturn(Collections.emptyList());
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            PortfolioSnapshot result = calculatePnlUseCase.execute(accountId, cash);

            // Then
            assertThat(result).isNotNull();
            verify(snapshotRepository).save(any());
        }

        @Test
        @DisplayName("Should use avg price as fallback when market price unavailable")
        void shouldUseAvgPriceAsFallbackWhenMarketPriceUnavailable() {
            // Given
            String accountId = "ACC_001";
            BigDecimal cash = BigDecimal.valueOf(5_000_000);

            when(positionRepository.findAllByAccount(accountId)).thenReturn(List.of(testPosition));
            when(marketDataCache.getPrice("005930")).thenReturn(null);
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            PortfolioSnapshot result = calculatePnlUseCase.execute(accountId, cash);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should publish PnlUpdated event")
        void shouldPublishPnlUpdatedEvent() {
            // Given
            String accountId = "ACC_001";
            BigDecimal cash = BigDecimal.valueOf(5_000_000);

            when(positionRepository.findAllByAccount(accountId)).thenReturn(Collections.emptyList());
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001", "EVENT_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            calculatePnlUseCase.execute(accountId, cash);

            // Then
            ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outboxService).save(eventCaptor.capture());

            OutboxEvent event = eventCaptor.getValue();
            assertThat(event.getEventType()).isEqualTo("PnlUpdated");
            assertThat(event.getPayload()).containsEntry("accountId", accountId);
        }
    }

    @Nested
    @DisplayName("Execute without Cash Tests")
    class ExecuteWithoutCashTests {

        @Test
        @DisplayName("Should fetch cash balance from KIS API")
        void shouldFetchCashBalanceFromKisApi() {
            // Given
            String accountId = "ACC_001";
            Account account = Account.builder()
                    .accountId(accountId)
                    .cano("12345678")
                    .acntPrdtCd("01")
                    .build();

            KisBalanceResponse response = mock(KisBalanceResponse.class);
            when(response.isSuccess()).thenReturn(true);
            when(response.getCashBalance()).thenReturn(BigDecimal.valueOf(10_000_000));

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(balanceApiClient.getBalance(any(), any(), any())).thenReturn(response);
            when(positionRepository.findAllByAccount(accountId)).thenReturn(Collections.emptyList());
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            PortfolioSnapshot result = calculatePnlUseCase.execute(accountId);

            // Then
            assertThat(result).isNotNull();
            verify(balanceApiClient).getBalance(any(), any(), any());
        }

        @Test
        @DisplayName("Should return zero cash when account not found")
        void shouldReturnZeroCashWhenAccountNotFound() {
            // Given
            String accountId = "NON_EXISTENT";

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
            when(positionRepository.findAllByAccount(accountId)).thenReturn(Collections.emptyList());
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            PortfolioSnapshot result = calculatePnlUseCase.execute(accountId);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should return zero cash when KIS API fails")
        void shouldReturnZeroCashWhenKisApiFails() {
            // Given
            String accountId = "ACC_001";
            Account account = Account.builder()
                    .accountId(accountId)
                    .cano("12345678")
                    .acntPrdtCd("01")
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(balanceApiClient.getBalance(any(), any(), any()))
                    .thenThrow(new RuntimeException("API Error"));
            when(positionRepository.findAllByAccount(accountId)).thenReturn(Collections.emptyList());
            when(ulidGenerator.generateInstance()).thenReturn("SNAP_001");
            when(snapshotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            PortfolioSnapshot result = calculatePnlUseCase.execute(accountId);

            // Then
            assertThat(result).isNotNull();
        }
    }
}
