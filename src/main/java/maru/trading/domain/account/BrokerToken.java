package maru.trading.domain.account;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Domain model for broker authentication token.
 * Represents OAuth2 access token with expiration tracking.
 */
public class BrokerToken {

    private final String tokenId;
    private final String broker;
    private final String environment;
    private final String accessToken;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;

    public BrokerToken(
            String tokenId,
            String broker,
            String environment,
            String accessToken,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt) {
        this.tokenId = tokenId;
        this.broker = broker;
        this.environment = environment;
        this.accessToken = accessToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Check if token is expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token needs refresh based on threshold.
     * Returns true if token expires within the given threshold duration.
     */
    public boolean needsRefresh(Duration threshold) {
        LocalDateTime refreshThreshold = LocalDateTime.now().plus(threshold);
        return refreshThreshold.isAfter(expiresAt);
    }

    // Getters
    public String getTokenId() {
        return tokenId;
    }

    public String getBroker() {
        return broker;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "BrokerToken{" +
                "tokenId='" + tokenId + '\'' +
                ", broker='" + broker + '\'' +
                ", environment='" + environment + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
