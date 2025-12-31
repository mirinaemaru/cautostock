package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity for broker_tokens table.
 * Stores OAuth2 access tokens for broker authentication.
 */
@Entity
@Table(name = "broker_tokens",
        indexes = {
                @Index(name = "idx_broker_tokens_expiry", columnList = "broker, environment, expires_at")
        })
public class BrokerTokenEntity {

    @Id
    @Column(name = "token_id", length = 26, nullable = false)
    private String tokenId;

    @Column(name = "broker", length = 20, nullable = false)
    private String broker; // e.g., "KIS"

    @Column(name = "environment", length = 20, nullable = false)
    private String environment; // PAPER, LIVE

    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(name = "issued_at", columnDefinition = "DATETIME(3)", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", columnDefinition = "DATETIME(3)", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", columnDefinition = "DATETIME(3)", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
