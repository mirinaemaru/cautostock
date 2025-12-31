package maru.trading.broker.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for KIS OAuth2 token response.
 * Maps to response from KIS /oauth2/tokenP endpoint.
 */
public class KisTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType; // e.g., "Bearer"

    @JsonProperty("expires_in")
    private int expiresIn; // Expiration time in seconds (e.g., 3600 for 1 hour)

    public KisTokenResponse() {
    }

    public KisTokenResponse(String accessToken, String tokenType, int expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    @Override
    public String toString() {
        return "KisTokenResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                '}';
    }
}
