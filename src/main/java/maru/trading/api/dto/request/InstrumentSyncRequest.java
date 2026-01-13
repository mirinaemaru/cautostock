package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for manual instrument sync.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstrumentSyncRequest {

    /**
     * Markets to sync.
     * Example: ["KOSPI", "KOSDAQ"]
     */
    @NotEmpty(message = "Markets cannot be empty")
    private List<String> markets;
}
