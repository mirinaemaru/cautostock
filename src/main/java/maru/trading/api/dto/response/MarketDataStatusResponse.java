package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for market data service status.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketDataStatusResponse {

    /**
     * Whether market data service is actively subscribed.
     */
    private boolean subscribed;

    /**
     * Active subscription ID.
     */
    private String subscriptionId;

    /**
     * Number of currently subscribed symbols.
     */
    private int symbolCount;

    /**
     * Whether the broker stream is connected.
     */
    private boolean connected;

    /**
     * Status message.
     */
    private String message;
}
