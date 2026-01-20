package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Account Permission Update Request
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountPermissionUpdateRequest {

    /**
     * 매수 허용
     */
    @NotNull(message = "tradeBuy is required")
    private Boolean tradeBuy;

    /**
     * 매도 허용
     */
    @NotNull(message = "tradeSell is required")
    private Boolean tradeSell;

    /**
     * 자동매매 허용
     */
    @NotNull(message = "autoTrade is required")
    private Boolean autoTrade;

    /**
     * 수동매매 허용
     */
    @NotNull(message = "manualTrade is required")
    private Boolean manualTrade;

    /**
     * PAPER 전용
     */
    @NotNull(message = "paperOnly is required")
    private Boolean paperOnly;
}
