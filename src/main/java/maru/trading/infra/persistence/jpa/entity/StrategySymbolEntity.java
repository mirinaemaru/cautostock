package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Strategy-Symbol mapping entity.
 * Defines which symbols a strategy should trade for which account.
 */
@Entity
@Table(name = "strategy_symbols")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategySymbolEntity {

    @Id
    @Column(name = "strategy_symbol_id", columnDefinition = "CHAR(26)")
    private String strategySymbolId;

    @Column(name = "strategy_id", columnDefinition = "CHAR(26)", nullable = false)
    private String strategyId;

    @Column(name = "symbol", length = 16, nullable = false)
    private String symbol;

    @Column(name = "account_id", columnDefinition = "CHAR(26)", nullable = false)
    private String accountId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
