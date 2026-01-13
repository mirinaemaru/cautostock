package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS OAuth2 토큰 요청 DTO.
 *
 * API: POST /oauth2/tokenP (PAPER) 또는 /oauth2/Approval (WebSocket)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KisTokenRequest {

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
     * Application secret
     */
    @JsonProperty("appsecret")
    private String appsecret;

    /**
     * OAuth2 토큰 발급 요청 생성
     */
    public static KisTokenRequest of(String appKey, String appSecret) {
        return KisTokenRequest.builder()
                .grantType("client_credentials")
                .appkey(appKey)
                .appsecret(appSecret)
                .build();
    }
}
