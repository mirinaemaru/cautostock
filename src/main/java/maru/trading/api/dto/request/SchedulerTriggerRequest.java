package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for manually triggering a strategy execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerTriggerRequest {

    /**
     * Strategy ID to execute.
     */
    @NotBlank(message = "strategyId is required")
    private String strategyId;

    /**
     * Symbol to evaluate (e.g., "005930").
     */
    @NotBlank(message = "symbol is required")
    private String symbol;

    /**
     * Account ID for order placement.
     */
    @NotBlank(message = "accountId is required")
    private String accountId;
}
