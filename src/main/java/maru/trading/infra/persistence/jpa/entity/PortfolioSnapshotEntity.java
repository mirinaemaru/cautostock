package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for portfolio_snapshots table.
 * Point-in-time snapshot of account portfolio state.
 */
@Entity
@Table(name = "portfolio_snapshots",
        indexes = {
                @Index(name = "idx_portfolio_snapshot_account", columnList = "account_id, snapshot_ts")
        })
public class PortfolioSnapshotEntity {

    @Id
    @Column(name = "snapshot_id", length = 26, nullable = false)
    private String snapshotId;

    @Column(name = "account_id", length = 26, nullable = false)
    private String accountId;

    @Column(name = "total_value", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalValue;

    @Column(name = "cash", precision = 20, scale = 4, nullable = false)
    private BigDecimal cash;

    @Column(name = "realized_pnl", precision = 20, scale = 4, nullable = false)
    private BigDecimal realizedPnl;

    @Column(name = "unrealized_pnl", precision = 20, scale = 4, nullable = false)
    private BigDecimal unrealizedPnl;

    @Column(name = "snapshot_ts", columnDefinition = "DATETIME(3)", nullable = false)
    private LocalDateTime snapshotTs;

    @Column(name = "created_at", columnDefinition = "DATETIME(3)", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public LocalDateTime getSnapshotTs() {
        return snapshotTs;
    }

    public void setSnapshotTs(LocalDateTime snapshotTs) {
        this.snapshotTs = snapshotTs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
