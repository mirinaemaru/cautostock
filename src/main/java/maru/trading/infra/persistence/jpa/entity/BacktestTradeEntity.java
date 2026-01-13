package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for individual trades during backtest.
 *
 * Tracks entry, exit, P&L, and metadata for each trade
 * executed during backtest simulation.
 */
@Entity
@Table(name = "backtest_trades", indexes = {
        @Index(name = "idx_backtest_id", columnList = "backtest_id"),
        @Index(name = "idx_symbol", columnList = "symbol"),
        @Index(name = "idx_entry_time", columnList = "entry_time")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTradeEntity {

    @Id
    @Column(name = "trade_id", columnDefinition = "CHAR(26)")
    private String tradeId;

    @Column(name = "backtest_id", columnDefinition = "CHAR(26)", nullable = false)
    private String backtestId;

    @Column(name = "symbol", length = 16, nullable = false)
    private String symbol;

    // ========== Entry ==========

    @Column(name = "entry_time", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime entryTime;

    @Column(name = "entry_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal entryPrice;

    @Column(name = "entry_qty", precision = 18, scale = 6, nullable = false)
    private BigDecimal entryQty;

    /**
     * Side: BUY or SELL.
     */
    @Column(name = "side", length = 8, nullable = false)
    private String side;

    // ========== Exit ==========

    @Column(name = "exit_time", columnDefinition = "DATETIME(3)")
    private LocalDateTime exitTime;

    @Column(name = "exit_price", precision = 18, scale = 4)
    private BigDecimal exitPrice;

    @Column(name = "exit_qty", precision = 18, scale = 6)
    private BigDecimal exitQty;

    // ========== P&L ==========

    @Column(name = "gross_pnl", precision = 18, scale = 4)
    private BigDecimal grossPnl;

    @Column(name = "commission_paid", precision = 18, scale = 4)
    private BigDecimal commissionPaid;

    @Column(name = "slippage_cost", precision = 18, scale = 4)
    private BigDecimal slippageCost;

    @Column(name = "net_pnl", precision = 18, scale = 4)
    private BigDecimal netPnl;

    @Column(name = "return_pct", precision = 10, scale = 6)
    private BigDecimal returnPct;

    // ========== Metadata ==========

    @Column(name = "signal_reason", length = 500)
    private String signalReason;

    /**
     * Status: OPEN, CLOSED, PARTIAL.
     */
    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "OPEN";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== Business Methods ==========

    /**
     * Close the trade with exit details.
     *
     * @param exitTime Exit timestamp
     * @param exitPrice Exit price
     * @param exitQty Exit quantity
     * @param commission Commission rate
     * @param slippage Slippage rate
     */
    public void close(LocalDateTime exitTime, BigDecimal exitPrice, BigDecimal exitQty,
                      BigDecimal commission, BigDecimal slippage) {
        this.exitTime = exitTime;
        this.exitPrice = exitPrice;
        this.exitQty = exitQty;
        this.status = "CLOSED";

        // Calculate P&L
        calculatePnl(commission, slippage);
    }

    /**
     * Calculate P&L metrics.
     *
     * @param commissionRate Commission rate (e.g., 0.001 = 0.1%)
     * @param slippageRate Slippage rate (e.g., 0.0005 = 0.05%)
     */
    private void calculatePnl(BigDecimal commissionRate, BigDecimal slippageRate) {
        if (exitPrice == null || exitQty == null) {
            return;
        }

        BigDecimal entryValue = entryPrice.multiply(entryQty);
        BigDecimal exitValue = exitPrice.multiply(exitQty);

        // Gross P&L (before costs)
        if ("BUY".equals(side)) {
            // Long: P&L = (Exit - Entry) * Qty
            grossPnl = exitValue.subtract(entryValue);
        } else {
            // Short: P&L = (Entry - Exit) * Qty
            grossPnl = entryValue.subtract(exitValue);
        }

        // Commission cost (entry + exit)
        BigDecimal entryCommission = entryValue.multiply(commissionRate);
        BigDecimal exitCommission = exitValue.multiply(commissionRate);
        commissionPaid = entryCommission.add(exitCommission);

        // Slippage cost (entry + exit)
        BigDecimal entrySlippage = entryValue.multiply(slippageRate);
        BigDecimal exitSlippage = exitValue.multiply(slippageRate);
        slippageCost = entrySlippage.add(exitSlippage);

        // Net P&L
        netPnl = grossPnl.subtract(commissionPaid).subtract(slippageCost);

        // Return percentage
        if (entryValue.compareTo(BigDecimal.ZERO) > 0) {
            returnPct = netPnl.divide(entryValue, 6, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    /**
     * Check if trade is a winner.
     */
    public boolean isWinner() {
        return netPnl != null && netPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if trade is a loser.
     */
    public boolean isLoser() {
        return netPnl != null && netPnl.compareTo(BigDecimal.ZERO) < 0;
    }
}
