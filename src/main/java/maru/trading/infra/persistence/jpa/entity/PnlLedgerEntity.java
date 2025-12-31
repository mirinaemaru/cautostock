package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for pnl_ledger table.
 * Append-only record of P&L events.
 */
@Entity
@Table(name = "pnl_ledger",
        indexes = {
                @Index(name = "idx_pnl_ledger_account", columnList = "account_id, event_ts"),
                @Index(name = "idx_pnl_ledger_symbol", columnList = "account_id, symbol, event_ts")
        })
public class PnlLedgerEntity {

    @Id
    @Column(name = "ledger_id", length = 26, nullable = false)
    private String ledgerId;

    @Column(name = "account_id", length = 26, nullable = false)
    private String accountId;

    @Column(name = "symbol", length = 20)
    private String symbol;

    @Column(name = "event_type", length = 20, nullable = false)
    private String eventType; // FILL, FEE, TAX, ADJUST

    @Column(name = "amount", precision = 20, scale = 4, nullable = false)
    private BigDecimal amount; // Signed amount (positive = profit, negative = loss)

    @Column(name = "ref_id", length = 26)
    private String refId; // Reference ID (fill_id, order_id, etc.)

    @Column(name = "event_ts", columnDefinition = "DATETIME(3)", nullable = false)
    private LocalDateTime eventTs;

    @Column(name = "created_at", columnDefinition = "DATETIME(3)", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public String getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(String ledgerId) {
        this.ledgerId = ledgerId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public LocalDateTime getEventTs() {
        return eventTs;
    }

    public void setEventTs(LocalDateTime eventTs) {
        this.eventTs = eventTs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
