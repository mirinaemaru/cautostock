package maru.trading.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating risk rules.
 *
 * All fields are optional - only provided fields will be updated.
 * null values mean "use default" or "no limit".
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRiskRuleRequest {

    /**
     * Maximum position value per symbol (KRW).
     * Example: 5000000 = 5M KRW max exposure per symbol.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Max position value must be positive")
    private BigDecimal maxPositionValuePerSymbol;

    /**
     * Maximum number of open (unfilled) orders allowed.
     * Example: 10 = allow up to 10 pending orders.
     */
    @Min(value = 1, message = "Max open orders must be at least 1")
    private Integer maxOpenOrders;

    /**
     * Maximum orders per minute (frequency limit).
     * Example: 5 = allow up to 5 orders per minute.
     */
    @Min(value = 1, message = "Max orders per minute must be at least 1")
    private Integer maxOrdersPerMinute;

    /**
     * Daily loss limit (KRW, positive value).
     * Example: 100000 = stop trading if daily loss exceeds 100K.
     * Kill switch triggers when dailyPnl < -dailyLossLimit.
     */
    @DecimalMin(value = "0.0", inclusive = false, message = "Daily loss limit must be positive")
    private BigDecimal dailyLossLimit;

    /**
     * Consecutive order failures before kill switch.
     * Example: 5 = trigger kill switch after 5 consecutive order rejections.
     */
    @Min(value = 1, message = "Consecutive failures limit must be at least 1")
    private Integer consecutiveOrderFailuresLimit;
}
