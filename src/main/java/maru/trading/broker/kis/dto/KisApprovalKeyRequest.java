package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS WebSocket Approval Key 요청 DTO.
 *
 * API: POST /oauth2/Approval
 *
 * Note: This API uses 'secretkey' instead of 'appsecret' field name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisApprovalKeyRequest {

    /**
     * Grant type (항상 "client_credentials")
     */
    @JsonProperty("grant_type")
    private String grantType;

    /**
     * Application key
     */
    @JsonProperty("appkey")
    private String appkey;

    /**
     * Application secret (KIS Approval API uses 'secretkey')
     */
    @JsonProperty("secretkey")
    private String secretkey;

    /**
     * Approval Key 발급 요청 생성
     */
    public static KisApprovalKeyRequest of(String appKey, String appSecret) {
        return KisApprovalKeyRequest.builder()
                .grantType("client_credentials")
                .appkey(appKey)
                .secretkey(appSecret)
                .build();
    }
}
