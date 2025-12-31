package maru.trading.infra.messaging.listener;

import maru.trading.application.usecase.execution.ApplyFillUseCase;
import maru.trading.domain.execution.Fill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event listener for fill events from WebSocket.
 *
 * Listens for Fill events and triggers ApplyFillUseCase.
 *
 * In current implementation, fills are processed synchronously.
 * In production, could use:
 * - @Async for async processing
 * - @TransactionalEventListener for transactional guarantees
 * - Spring Integration or Kafka for event streaming
 */
@Component
public class FillEventListener {

    private static final Logger log = LoggerFactory.getLogger(FillEventListener.class);

    private final ApplyFillUseCase applyFillUseCase;

    public FillEventListener(ApplyFillUseCase applyFillUseCase) {
        this.applyFillUseCase = applyFillUseCase;
    }

    /**
     * Handle fill received from WebSocket.
     * Triggers ApplyFillUseCase to update positions and P&L.
     *
     * @param fill Fill event
     */
    public void onFillReceived(Fill fill) {
        log.info("Fill event received: fillId={}, orderId={}, symbol={}, qty={}",
                fill.getFillId(), fill.getOrderId(), fill.getSymbol(), fill.getFillQty());

        try {
            // Execute use case to apply fill
            ApplyFillUseCase.ApplyFillResult result = applyFillUseCase.execute(fill);

            if (result.isDuplicate()) {
                log.info("Duplicate fill handled: fillId={}", fill.getFillId());
            } else {
                log.info("Fill applied successfully: fillId={}, positionId={}, realizedPnlDelta={}",
                        fill.getFillId(),
                        result.getPosition().getPositionId(),
                        result.getRealizedPnlDelta());
            }

        } catch (Exception e) {
            log.error("Error applying fill: fillId={}", fill.getFillId(), e);
            // In production, could publish to DLQ (dead letter queue)
            // or trigger alert for manual intervention
        }
    }
}
