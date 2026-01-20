package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for subscribed symbols.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscribedSymbolsResponse {

    /**
     * Currently subscribed symbols.
     */
    private List<String> symbols;

    /**
     * Total number of subscribed symbols.
     */
    private int total;

    /**
     * Active subscription ID.
     */
    private String subscriptionId;

    /**
     * Whether the subscription is active.
     */
    private boolean active;
}
