package maru.trading.application.usecase.execution;

import maru.trading.TestFixtures;
import maru.trading.application.ports.repo.FillRepository;
import maru.trading.application.ports.repo.PnlLedgerRepository;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.application.usecase.risk.UpdateRiskStateWithPnlUseCase;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.execution.PnlLedger;
import maru.trading.domain.execution.Position;
import maru.trading.domain.order.Side;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * ApplyFillUseCase 테스트
 *
 * 테스트 범위:
 * 1. 중복 체결 감지
 * 2. 신규 포지션 생성 (BUY)
 * 3. 포지션 확대 (BUY → BUY)
 * 4. 포지션 축소 (BUY → SELL, 일부)
 * 5. 포지션 청산 (BUY → SELL, 전체)
 * 6. 포지션 반전 (BUY → SELL, 초과)
 * 7. Realized PnL 계산
 * 8. P&L Ledger 생성 (Fill, Fee, Tax)
 * 9. 이벤트 발행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyFillUseCase 테스트")
class ApplyFillUseCaseTest {

    @Mock
    private FillRepository fillRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PnlLedgerRepository pnlLedgerRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @Mock
    private UpdateRiskStateWithPnlUseCase updateRiskStateWithPnlUseCase;

    @InjectMocks
    private ApplyFillUseCase applyFillUseCase;

    private Fill testBuyFill;
    private Fill testSellFill;

    @BeforeEach
    void setUp() {
        // Default BUY fill
        testBuyFill = TestFixtures.createFill(
            "FILL_001",
            "ORDER_001",
            "ACC_001",
            "005930",
            Side.BUY,
            BigDecimal.valueOf(70000),
            10,
            BigDecimal.valueOf(500),
            BigDecimal.ZERO
        );

        // Default SELL fill
        testSellFill = TestFixtures.createFill(
            "FILL_002",
            "ORDER_002",
            "ACC_001",
            "005930",
            Side.SELL,
            BigDecimal.valueOf(75000),
            5,
            BigDecimal.valueOf(300),
            BigDecimal.valueOf(1875)  // 5 * 75000 * 0.005
        );

        lenient().when(ulidGenerator.generateInstance()).thenReturn("ULID_001", "ULID_002", "ULID_003");
    }

    // ==================== 1. Duplicate Detection Tests ====================

    @Test
    @DisplayName("중복 체결 감지 - 기존 포지션 반환, 변경 없음")
    void testDuplicateFill_ReturnExisting_NoChanges() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(
            eq("ORDER_001"),
            any(LocalDateTime.class),
            eq(BigDecimal.valueOf(70000)),
            eq(10)
        )).thenReturn(true);

        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testBuyFill);

        // Then
        assertThat(result.isDuplicate()).isTrue();
        assertThat(result.getRealizedPnlDelta()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getPosition()).isEqualTo(existingPosition);

        // Verify no changes made
        verify(fillRepository, never()).save(any());
        verify(positionRepository, never()).upsert(any());
        verify(pnlLedgerRepository, never()).saveAll(any());
        verify(outboxService, never()).save(any());
    }

    // ==================== 2. New Position Tests ====================

    @Test
    @DisplayName("신규 포지션 생성 - BUY 체결")
    void testNewPosition_BuyFill_CreatePosition() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testBuyFill);
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.empty());

        Position newPosition = Position.createEmpty("POS_001", "ACC_001", "005930");
        newPosition.applyBuyFill(testBuyFill);
        when(positionRepository.upsert(any())).thenReturn(newPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testBuyFill);

        // Then
        assertThat(result.isDuplicate()).isFalse();
        assertThat(result.getPosition().getQty()).isEqualTo(10);
        assertThat(result.getPosition().getAvgPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(result.getRealizedPnlDelta()).isEqualByComparingTo(BigDecimal.ZERO); // No realized P&L on opening

        verify(fillRepository).save(testBuyFill);
        verify(positionRepository).upsert(any());
    }

    // ==================== 3. Position Increase Tests ====================

    @Test
    @DisplayName("포지션 확대 - BUY → BUY (평균가 재계산)")
    void testPositionIncrease_BuyToBuy_AveragePriceRecalculated() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testBuyFill);

        // Existing position: 10 shares @ 70,000
        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        // New fill: 10 shares @ 80,000
        Fill newBuyFill = TestFixtures.createFill(
            "FILL_003",
            "ORDER_003",
            "ACC_001",
            "005930",
            Side.BUY,
            BigDecimal.valueOf(80000),
            10,
            BigDecimal.valueOf(600),
            BigDecimal.ZERO
        );

        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applyBuyFill(newBuyFill);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(newBuyFill);

        // Then
        // New qty: 10 + 10 = 20
        // New avg price: (10*70000 + 10*80000) / 20 = 75,000
        assertThat(result.getPosition().getQty()).isEqualTo(20);
        assertThat(result.getPosition().getAvgPrice()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(result.getRealizedPnlDelta()).isEqualByComparingTo(BigDecimal.ZERO); // No realized P&L on increase
    }

    // ==================== 4. Position Decrease Tests ====================

    @Test
    @DisplayName("포지션 축소 - BUY → SELL (일부 청산, 수익)")
    void testPositionDecrease_BuyToSell_Partial_Profit() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testSellFill);

        // Existing long position: 10 shares @ 70,000
        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        // SELL fill: 5 shares @ 75,000
        // Realized P&L: 5 * (75000 - 70000) = 25,000
        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applySellFill(testSellFill);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testSellFill);

        // Then
        // Remaining qty: 10 - 5 = 5
        // Avg price: 70,000 (unchanged for partial close)
        // Realized P&L: 5 * (75000 - 70000) = 25,000
        assertThat(result.getPosition().getQty()).isEqualTo(5);
        assertThat(result.getPosition().getAvgPrice()).isEqualTo(BigDecimal.valueOf(70000));
        assertThat(result.getRealizedPnlDelta()).isEqualTo(BigDecimal.valueOf(25000));

        verify(updateRiskStateWithPnlUseCase).execute("ACC_001", BigDecimal.valueOf(25000));
    }

    @Test
    @DisplayName("포지션 축소 - BUY → SELL (일부 청산, 손실)")
    void testPositionDecrease_BuyToSell_Partial_Loss() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);

        // Existing long position: 10 shares @ 70,000
        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        // SELL fill: 5 shares @ 65,000 (loss)
        // Realized P&L: 5 * (65000 - 70000) = -25,000
        Fill sellFillLoss = TestFixtures.createFill(
            "FILL_004",
            "ORDER_004",
            "ACC_001",
            "005930",
            Side.SELL,
            BigDecimal.valueOf(65000),
            5,
            BigDecimal.valueOf(300),
            BigDecimal.valueOf(1625)
        );

        when(fillRepository.save(any())).thenReturn(sellFillLoss);

        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applySellFill(sellFillLoss);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(sellFillLoss);

        // Then
        assertThat(result.getPosition().getQty()).isEqualTo(5);
        assertThat(result.getRealizedPnlDelta()).isEqualTo(BigDecimal.valueOf(-25000));

        verify(updateRiskStateWithPnlUseCase).execute("ACC_001", BigDecimal.valueOf(-25000));
    }

    // ==================== 5. Position Closure Tests ====================

    @Test
    @DisplayName("포지션 청산 - BUY → SELL (전체 청산)")
    void testPositionClosure_BuyToSell_Full() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);

        // Existing long position: 10 shares @ 70,000
        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        // SELL fill: 10 shares @ 75,000 (full close)
        // Realized P&L: 10 * (75000 - 70000) = 50,000
        Fill sellFillFull = TestFixtures.createFill(
            "FILL_005",
            "ORDER_005",
            "ACC_001",
            "005930",
            Side.SELL,
            BigDecimal.valueOf(75000),
            10,
            BigDecimal.valueOf(600),
            BigDecimal.valueOf(3750)
        );

        when(fillRepository.save(any())).thenReturn(sellFillFull);

        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applySellFill(sellFillFull);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(sellFillFull);

        // Then
        assertThat(result.getPosition().getQty()).isEqualTo(0); // Flat position
        assertThat(result.getRealizedPnlDelta()).isEqualTo(BigDecimal.valueOf(50000));

        verify(updateRiskStateWithPnlUseCase).execute("ACC_001", BigDecimal.valueOf(50000));
    }

    // ==================== 6. P&L Ledger Tests ====================

    @Test
    @DisplayName("P&L Ledger 생성 - Realized PnL + Fee + Tax")
    void testPnlLedger_AllEntries() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testSellFill);

        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applySellFill(testSellFill);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        when(ulidGenerator.generateInstance())
            .thenReturn("LEDGER_001", "LEDGER_002", "LEDGER_003", "EVENT_001", "EVENT_002", "EVENT_003");

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testSellFill);

        // Then
        ArgumentCaptor<List<PnlLedger>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(pnlLedgerRepository).saveAll(ledgerCaptor.capture());

        List<PnlLedger> savedLedgers = ledgerCaptor.getValue();
        assertThat(savedLedgers).hasSize(3); // Fill PnL + Fee + Tax

        // Verify ledger types
        assertThat(savedLedgers.stream().filter(l -> l.getEventType().equals("FILL")).count()).isEqualTo(1);
        assertThat(savedLedgers.stream().filter(l -> l.getEventType().equals("FEE")).count()).isEqualTo(1);
        assertThat(savedLedgers.stream().filter(l -> l.getEventType().equals("TAX")).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("P&L Ledger 생성 - Fee만 있는 경우 (BUY)")
    void testPnlLedger_OnlyFee_BuyFill() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testBuyFill);
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.empty());

        Position newPosition = Position.createEmpty("POS_001", "ACC_001", "005930");
        newPosition.applyBuyFill(testBuyFill);
        when(positionRepository.upsert(any())).thenReturn(newPosition);

        when(ulidGenerator.generateInstance())
            .thenReturn("POS_001", "LEDGER_001", "EVENT_001", "EVENT_002");

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testBuyFill);

        // Then
        ArgumentCaptor<List<PnlLedger>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(pnlLedgerRepository).saveAll(ledgerCaptor.capture());

        List<PnlLedger> savedLedgers = ledgerCaptor.getValue();
        assertThat(savedLedgers).hasSize(1); // Only Fee (no realized P&L on BUY open)
        assertThat(savedLedgers.get(0).getEventType()).isEqualTo("FEE");
        assertThat(savedLedgers.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(-500)); // Fee is negative (cost)
    }

    // ==================== 7. Event Publishing Tests ====================

    @Test
    @DisplayName("이벤트 발행 - FillReceived, PositionUpdated, PnlUpdated")
    void testEventPublishing_AllEvents() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testSellFill);

        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applySellFill(testSellFill);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testSellFill);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService, times(3)).save(eventCaptor.capture());

        List<OutboxEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(3);

        // Verify event types
        assertThat(events.get(0).getEventType()).isEqualTo("FillReceived");
        assertThat(events.get(1).getEventType()).isEqualTo("PositionUpdated");
        assertThat(events.get(2).getEventType()).isEqualTo("PnlUpdated");
    }

    @Test
    @DisplayName("이벤트 발행 - PnL 변화 없을 때 PnlUpdated 이벤트 스킵")
    void testEventPublishing_SkipPnlUpdated_NoPnlChange() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testBuyFill);
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.empty());

        Position newPosition = Position.createEmpty("POS_001", "ACC_001", "005930");
        newPosition.applyBuyFill(testBuyFill);
        when(positionRepository.upsert(any())).thenReturn(newPosition);

        when(ulidGenerator.generateInstance())
            .thenReturn("POS_001", "LEDGER_001", "EVENT_001", "EVENT_002");

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testBuyFill);

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService, times(2)).save(eventCaptor.capture());

        List<OutboxEvent> events = eventCaptor.getAllValues();
        assertThat(events).hasSize(2); // FillReceived + PositionUpdated (no PnlUpdated)
        assertThat(events.get(0).getEventType()).isEqualTo("FillReceived");
        assertThat(events.get(1).getEventType()).isEqualTo("PositionUpdated");
    }

    // ==================== 8. Risk State Update Tests ====================

    @Test
    @DisplayName("Risk State 업데이트 - Realized PnL 변화 시 호출")
    void testRiskStateUpdate_Called_OnPnlChange() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testSellFill);

        Position existingPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.of(existingPosition));

        Position updatedPosition = TestFixtures.createLongPosition(
            "POS_001", "ACC_001", "005930", 10, BigDecimal.valueOf(70000)
        );
        updatedPosition.applySellFill(testSellFill);
        when(positionRepository.upsert(any())).thenReturn(updatedPosition);

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testSellFill);

        // Then
        verify(updateRiskStateWithPnlUseCase).execute(
            eq("ACC_001"),
            eq(BigDecimal.valueOf(25000))
        );
    }

    @Test
    @DisplayName("Risk State 업데이트 - PnL 변화 없을 때 호출 안 함")
    void testRiskStateUpdate_NotCalled_NoPnlChange() {
        // Given
        when(fillRepository.existsByOrderIdAndDetails(any(), any(), any(), anyInt()))
            .thenReturn(false);
        when(fillRepository.save(any())).thenReturn(testBuyFill);
        when(positionRepository.findByAccountAndSymbol("ACC_001", "005930"))
            .thenReturn(Optional.empty());

        Position newPosition = Position.createEmpty("POS_001", "ACC_001", "005930");
        newPosition.applyBuyFill(testBuyFill);
        when(positionRepository.upsert(any())).thenReturn(newPosition);

        when(ulidGenerator.generateInstance())
            .thenReturn("POS_001", "LEDGER_001", "EVENT_001", "EVENT_002");

        // When
        ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(testBuyFill);

        // Then
        verify(updateRiskStateWithPnlUseCase, never()).execute(any(), any());
    }
}
