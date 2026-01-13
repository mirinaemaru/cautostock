package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for backtest run metadata and results.
 *
 * Stores configuration, execution status, and performance metrics
 * for each backtest execution.
 */
@Entity
@Table(name = "backtest_runs", indexes = {
        @Index(name = "idx_strategy_id", columnList = "strategy_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_started_at", columnList = "started_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestRunEntity {

    @Id
    @Column(name = "backtest_id", columnDefinition = "CHAR(26)")
    private String backtestId;

    @Column(name = "strategy_id", columnDefinition = "CHAR(26)", nullable = false)
    private String strategyId;

    @Column(name = "strategy_version_id", columnDefinition = "CHAR(26)")
    private String strategyVersionId;

    // ========== Configuration ==========

    @Column(name = "start_date", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime endDate;

    /**
     * Comma-separated list of symbols tested.
     */
    @Column(name = "symbols", columnDefinition = "TEXT", nullable = false)
    private String symbols;

    @Column(name = "timeframe", length = 8, nullable = false)
    private String timeframe;

    @Column(name = "initial_capital", precision = 18, scale = 2, nullable = false)
    private BigDecimal initialCapital;

    @Column(name = "commission", precision = 8, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal commission = BigDecimal.valueOf(0.001);

    @Column(name = "slippage", precision = 8, scale = 6, nullable = false)
    @Builder.Default
    private BigDecimal slippage = BigDecimal.valueOf(0.0005);

    /**
     * Strategy parameters as JSON string.
     */
    @Column(name = "strategy_params", columnDefinition = "JSON")
    private String strategyParams;

    // ========== Execution Metadata ==========

    /**
     * Status: RUNNING, COMPLETED, FAILED.
     */
    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime startedAt;

    @Column(name = "completed_at", columnDefinition = "DATETIME(3)")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ========== Results Summary ==========

    @Column(name = "final_capital", precision = 18, scale = 2)
    private BigDecimal finalCapital;

    @Column(name = "total_return", precision = 10, scale = 6)
    private BigDecimal totalReturn;

    @Column(name = "total_trades")
    @Builder.Default
    private Integer totalTrades = 0;

    @Column(name = "winning_trades")
    @Builder.Default
    private Integer winningTrades = 0;

    @Column(name = "losing_trades")
    @Builder.Default
    private Integer losingTrades = 0;

    // ========== Detailed Metrics (JSON) ==========

    /**
     * Performance metrics as JSON: Sharpe ratio, max drawdown, win rate, etc.
     */
    @Column(name = "performance_metrics", columnDefinition = "JSON")
    private String performanceMetrics;

    /**
     * Risk metrics as JSON: VaR, CVaR, Beta, Alpha, etc.
     */
    @Column(name = "risk_metrics", columnDefinition = "JSON")
    private String riskMetrics;

    /**
     * Equity curve data points as JSON.
     */
    @Column(name = "equity_curve", columnDefinition = "JSON")
    private String equityCurve;

    // ========== Timestamps ==========

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
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "RUNNING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== Business Methods ==========

    public void complete(BigDecimal finalCapital, BigDecimal totalReturn) {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
        this.finalCapital = finalCapital;
        this.totalReturn = totalReturn;
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public void updateTradeStats(int totalTrades, int winningTrades, int losingTrades) {
        this.totalTrades = totalTrades;
        this.winningTrades = winningTrades;
        this.losingTrades = losingTrades;
    }
}
