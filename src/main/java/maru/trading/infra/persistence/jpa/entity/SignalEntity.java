package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for trading signals.
 */
@Entity
@Table(name = "signals", indexes = {
        @Index(name = "idx_signals_strategy_symbol", columnList = "strategy_id, symbol"),
        @Index(name = "idx_signals_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalEntity {

    @Id
    @Column(name = "signal_id", columnDefinition = "CHAR(26)")
    private String signalId;

    @Column(name = "strategy_id", columnDefinition = "CHAR(26)", nullable = false)
    private String strategyId;

    @Column(name = "strategy_version_id", columnDefinition = "CHAR(26)")
    private String strategyVersionId;

    @Column(name = "account_id", columnDefinition = "CHAR(26)", nullable = false)
    private String accountId;

    @Column(name = "symbol", length = 16, nullable = false)
    private String symbol;

    @Column(name = "signal_type", length = 8, nullable = false)
    private String signalType; // BUY, SELL, HOLD

    @Column(name = "target_type", length = 16)
    private String targetType; // QTY, WEIGHT

    @Column(name = "target_value", precision = 18, scale = 8)
    private BigDecimal targetValue;

    @Column(name = "ttl_seconds")
    private Integer ttlSeconds;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "expired", nullable = false)
    private Boolean expired;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expired == null) {
            expired = false;
        }
    }
}
