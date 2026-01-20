package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for removing symbols from market data subscription.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemoveSymbolsRequest {

    /**
     * Symbols to remove from subscription.
     * Example: ["005380", "051910"]
     */
    @NotEmpty(message = "Symbols cannot be empty")
    private List<String> symbols;
}
