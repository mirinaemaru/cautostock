package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding symbols to market data subscription.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddSymbolsRequest {

    /**
     * Symbols to add to subscription.
     * Example: ["005490", "000270"]
     */
    @NotEmpty(message = "Symbols cannot be empty")
    private List<String> symbols;
}
