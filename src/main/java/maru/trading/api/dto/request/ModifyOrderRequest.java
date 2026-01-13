package maru.trading.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for modifying an order (Phase 3.3).
 *
 * At least one of newQty or newPrice must be provided.
 * Null values mean "keep current value".
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyOrderRequest {

    /**
     * Order ID to modify.
     */
    @NotBlank(message = "Order ID is required")
    private String orderId;

    /**
     * New quantity (null to keep current).
     */
    @DecimalMin(value = "0.000001", message = "Quantity must be positive")
    private BigDecimal newQty;

    /**
     * New price (null to keep current).
     */
    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal newPrice;

    /**
     * Optional modification reason for audit trail.
     */
    private String reason;
}
