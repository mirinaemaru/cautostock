package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for cancelling an order (Phase 3.3).
 *
 * Only orderId is required - order will be loaded from database.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {

    /**
     * Order ID to cancel.
     */
    @NotBlank(message = "Order ID is required")
    private String orderId;

    /**
     * Optional cancellation reason for audit trail.
     */
    private String reason;
}
