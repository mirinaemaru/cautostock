package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS WebSocket Approval Key 응답 DTO.
 *
 * API: POST /oauth2/Approval
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisApprovalKeyResponse {

    /**
     * Approval key for WebSocket subscription
     */
    @JsonProperty("approval_key")
    private String approvalKey;
}
