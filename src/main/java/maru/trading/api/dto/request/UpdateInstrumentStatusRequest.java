package maru.trading.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating instrument status.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInstrumentStatusRequest {

    /**
     * New status: LISTED, DELISTED, SUSPENDED, UNDER_SUPERVISION
     */
    @NotBlank(message = "Status cannot be blank")
    private String status;

    /**
     * Whether instrument is tradable.
     */
    @NotNull(message = "Tradable flag required")
    private Boolean tradable;

    /**
     * Whether instrument is halted.
     */
    @NotNull(message = "Halted flag required")
    private Boolean halted;
}
