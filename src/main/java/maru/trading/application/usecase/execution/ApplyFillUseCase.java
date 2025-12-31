package maru.trading.application.usecase.execution;

import maru.trading.application.ports.repo.FillRepository;
import maru.trading.application.ports.repo.PnlLedgerRepository;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.execution.PnlLedger;
import maru.trading.domain.execution.Position;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CRITICAL USE CASE: Apply fill to position and update P&L.
 *
 * This use case orchestrates the entire fill processing workflow:
 * 1. Duplicate check
 * 2. Save fill to database
 * 3. Load or create position
 * 4. Apply fill to position (domain logic)
 * 5. Save updated position
 * 6. Create P&L ledger entries (fill, fee, tax)
 * 7. Publish events to outbox
 *
 * All steps execute in a single ACID transaction.
 */
@Service
public class ApplyFillUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyFillUseCase.class);

    private final FillRepository fillRepository;
    private final PositionRepository positionRepository;
    private final PnlLedgerRepository pnlLedgerRepository;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;

    public ApplyFillUseCase(
            FillRepository fillRepository,
            PositionRepository positionRepository,
            PnlLedgerRepository pnlLedgerRepository,
            OutboxService outboxService,
            UlidGenerator ulidGenerator) {
        this.fillRepository = fillRepository;
        this.positionRepository = positionRepository;
        this.pnlLedgerRepository = pnlLedgerRepository;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Execute the use case to apply a fill.
     * Returns the updated position and realized P&L delta.
     */
    @Transactional
    public ApplyFillResult execute(Fill fill) {
        log.info("Applying fill: {}", fill.getFillId());

        // Step 1: Duplicate check
        boolean isDuplicate = fillRepository.existsByOrderIdAndDetails(
                fill.getOrderId(),
                fill.getFillTimestamp(),
                fill.getFillPrice(),
                fill.getFillQty()
        );

        if (isDuplicate) {
            log.warn("Duplicate fill detected, skipping: fillId={}, orderId={}",
                    fill.getFillId(), fill.getOrderId());
            // Return existing position without changes
            Optional<Position> existingPosition = positionRepository.findByAccountAndSymbol(
                    fill.getAccountId(), fill.getSymbol());
            return new ApplyFillResult(
                    existingPosition.orElse(null),
                    BigDecimal.ZERO,
                    true // isDuplicate
            );
        }

        // Step 2: Save fill
        Fill savedFill = fillRepository.save(fill);
        log.debug("Fill saved: {}", savedFill.getFillId());

        // Step 3: Load or create position
        Position position = positionRepository.findByAccountAndSymbol(
                fill.getAccountId(),
                fill.getSymbol()
        ).orElseGet(() -> {
            String positionId = ulidGenerator.generateInstance();
            log.info("Creating new position: positionId={}, account={}, symbol={}",
                    positionId, fill.getAccountId(), fill.getSymbol());
            return Position.createEmpty(positionId, fill.getAccountId(), fill.getSymbol());
        });

        // Store previous realized P&L to calculate delta
        BigDecimal previousRealizedPnl = position.getRealizedPnl();

        // Step 4: Apply fill to position (domain logic)
        if (fill.getSide() == Side.BUY) {
            position.applyBuyFill(fill);
            log.debug("Applied BUY fill to position: qty={}, avgPrice={}",
                    position.getQty(), position.getAvgPrice());
        } else {
            position.applySellFill(fill);
            log.debug("Applied SELL fill to position: qty={}, avgPrice={}",
                    position.getQty(), position.getAvgPrice());
        }

        // Calculate realized P&L delta from this fill
        BigDecimal realizedPnlDelta = position.getRealizedPnl().subtract(previousRealizedPnl);

        // Step 5: Save updated position (upsert)
        Position updatedPosition = positionRepository.upsert(position);
        log.info("Position updated: {}, qty={}, avgPrice={}, realizedPnl={}",
                updatedPosition.getPositionId(),
                updatedPosition.getQty(),
                updatedPosition.getAvgPrice(),
                updatedPosition.getRealizedPnl());

        // Step 6: Create P&L ledger entries
        List<PnlLedger> ledgerEntries = new ArrayList<>();

        // Ledger entry for realized P&L (if any)
        if (realizedPnlDelta.compareTo(BigDecimal.ZERO) != 0) {
            PnlLedger fillLedger = PnlLedger.forFill(
                    ulidGenerator.generateInstance(),
                    fill.getAccountId(),
                    fill.getSymbol(),
                    realizedPnlDelta,
                    fill.getFillId(),
                    fill.getFillTimestamp()
            );
            ledgerEntries.add(fillLedger);
        }

        // Ledger entry for fee
        if (fill.getFee().compareTo(BigDecimal.ZERO) > 0) {
            PnlLedger feeLedger = PnlLedger.forFee(
                    ulidGenerator.generateInstance(),
                    fill.getAccountId(),
                    fill.getSymbol(),
                    fill.getFee(),
                    fill.getFillId(),
                    fill.getFillTimestamp()
            );
            ledgerEntries.add(feeLedger);
        }

        // Ledger entry for tax
        if (fill.getTax().compareTo(BigDecimal.ZERO) > 0) {
            PnlLedger taxLedger = PnlLedger.forTax(
                    ulidGenerator.generateInstance(),
                    fill.getAccountId(),
                    fill.getSymbol(),
                    fill.getTax(),
                    fill.getFillId(),
                    fill.getFillTimestamp()
            );
            ledgerEntries.add(taxLedger);
        }

        // Save ledger entries
        if (!ledgerEntries.isEmpty()) {
            pnlLedgerRepository.saveAll(ledgerEntries);
            log.debug("Saved {} P&L ledger entries", ledgerEntries.size());
        }

        // Step 7: Publish events to outbox
        publishFillReceivedEvent(savedFill);
        publishPositionUpdatedEvent(updatedPosition);
        publishPnlUpdatedEvent(fill.getAccountId(), realizedPnlDelta);

        log.info("Fill applied successfully: fillId={}, positionId={}, realizedPnlDelta={}",
                fill.getFillId(), updatedPosition.getPositionId(), realizedPnlDelta);

        return new ApplyFillResult(updatedPosition, realizedPnlDelta, false);
    }

    private void publishFillReceivedEvent(Fill fill) {
        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("FillReceived")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "fillId", fill.getFillId(),
                        "orderId", fill.getOrderId(),
                        "accountId", fill.getAccountId(),
                        "symbol", fill.getSymbol(),
                        "side", fill.getSide().name(),
                        "fillQty", fill.getFillQty(),
                        "fillPrice", fill.getFillPrice()
                ))
                .build();
        outboxService.save(event);
        log.debug("Published FillReceived event: {}", eventId);
    }

    private void publishPositionUpdatedEvent(Position position) {
        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("PositionUpdated")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "positionId", position.getPositionId(),
                        "accountId", position.getAccountId(),
                        "symbol", position.getSymbol(),
                        "qty", position.getQty(),
                        "avgPrice", position.getAvgPrice(),
                        "realizedPnl", position.getRealizedPnl()
                ))
                .build();
        outboxService.save(event);
        log.debug("Published PositionUpdated event: {}", eventId);
    }

    private void publishPnlUpdatedEvent(String accountId, BigDecimal realizedPnlDelta) {
        if (realizedPnlDelta.compareTo(BigDecimal.ZERO) == 0) {
            return; // No P&L change, skip event
        }

        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("PnlUpdated")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "accountId", accountId,
                        "realizedPnlDelta", realizedPnlDelta
                ))
                .build();
        outboxService.save(event);
        log.debug("Published PnlUpdated event: {}", eventId);
    }

    /**
     * Result object for ApplyFillUseCase.
     */
    public static class ApplyFillResult {
        private final Position position;
        private final BigDecimal realizedPnlDelta;
        private final boolean isDuplicate;

        public ApplyFillResult(Position position, BigDecimal realizedPnlDelta, boolean isDuplicate) {
            this.position = position;
            this.realizedPnlDelta = realizedPnlDelta;
            this.isDuplicate = isDuplicate;
        }

        public Position getPosition() {
            return position;
        }

        public BigDecimal getRealizedPnlDelta() {
            return realizedPnlDelta;
        }

        public boolean isDuplicate() {
            return isDuplicate;
        }
    }
}
