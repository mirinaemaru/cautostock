package maru.trading.broker.kis.fill;

import maru.trading.application.usecase.execution.ApplyFillUseCase;
import maru.trading.domain.execution.Fill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Fill Stream Handler.
 *
 * Handles real-time fill notifications from WebSocket.
 */
@Component
public class FillStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(FillStreamHandler.class);

    private final ApplyFillUseCase applyFillUseCase;
    private final DuplicateFillFilter duplicateFilter;
    private final FillDataValidator validator;

    private final AtomicLong fillsReceived = new AtomicLong(0);
    private final AtomicLong fillsProcessed = new AtomicLong(0);
    private final AtomicLong fillsDuplicate = new AtomicLong(0);
    private final AtomicLong fillsInvalid = new AtomicLong(0);

    public FillStreamHandler(
            ApplyFillUseCase applyFillUseCase,
            DuplicateFillFilter duplicateFilter,
            FillDataValidator validator) {
        this.applyFillUseCase = applyFillUseCase;
        this.duplicateFilter = duplicateFilter;
        this.validator = validator;
    }

    /**
     * Handle fill notification from WebSocket.
     *
     * @param fill Fill data from WebSocket
     */
    public void onFill(Fill fill) {
        fillsReceived.incrementAndGet();

        log.info("Received fill notification: orderId={}, fillId={}, qty={}, price={}",
                fill.getOrderId(), fill.getFillId(), fill.getFillQty(), fill.getFillPrice());

        try {
            // 1. Validate fill data
            FillDataValidator.ValidationResult validation = validator.validate(fill);
            if (!validation.isValid()) {
                fillsInvalid.incrementAndGet();
                log.warn("Invalid fill data: {}", validation.getErrorMessage());
                return;
            }

            // 2. Check for duplicates
            if (duplicateFilter.isDuplicate(fill)) {
                fillsDuplicate.incrementAndGet();
                log.warn("Duplicate fill detected: fillId={}", fill.getFillId());
                return;
            }

            // 3. Apply fill (update position, PnL)
            applyFillUseCase.execute(fill);

            fillsProcessed.incrementAndGet();

            log.info("Fill processed successfully: fillId={}, orderId={}",
                    fill.getFillId(), fill.getOrderId());

        } catch (Exception e) {
            log.error("Error processing fill: fillId={}, error={}",
                    fill.getFillId(), e.getMessage(), e);
        }
    }

    /**
     * Get total fills received.
     */
    public long getFillsReceived() {
        return fillsReceived.get();
    }

    /**
     * Get fills processed.
     */
    public long getFillsProcessed() {
        return fillsProcessed.get();
    }

    /**
     * Get duplicate fills count.
     */
    public long getFillsDuplicate() {
        return fillsDuplicate.get();
    }

    /**
     * Get invalid fills count.
     */
    public long getFillsInvalid() {
        return fillsInvalid.get();
    }

    /**
     * Reset statistics.
     */
    public void resetStats() {
        fillsReceived.set(0);
        fillsProcessed.set(0);
        fillsDuplicate.set(0);
        fillsInvalid.set(0);
        log.info("Fill stream statistics reset");
    }
}
