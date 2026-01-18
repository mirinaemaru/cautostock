package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyPerformanceEntity {

    @Id
    @Column(name = "performance_id", columnDefinition = "CHAR(26)")
    private String performanceId;

    @Column(name = "account_id", columnDefinition = "CHAR(26)", nullable = false)
    private String accountId;

    @Column(name = "strategy_id", columnDefinition = "CHAR(26)")
    private String strategyId;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "total_trades", nullable = false)
    @Builder.Default
    private Integer totalTrades = 0;

    @Column(name = "winning_trades", nullable = false)
    @Builder.Default
    private Integer winningTrades = 0;

    @Column(name = "losing_trades", nullable = false)
    @Builder.Default
    private Integer losingTrades = 0;

    @Column(name = "total_pnl", precision = 20, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalPnl = BigDecimal.ZERO;

    @Column(name = "realized_pnl", precision = 20, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl", precision = 20, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @Column(name = "total_volume", precision = 20, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalVolume = BigDecimal.ZERO;

    @Column(name = "total_fees", precision = 20, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Column(name = "max_drawdown", precision = 10, scale = 4)
    private BigDecimal maxDrawdown;

    @Column(name = "sharpe_ratio", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public double getWinRate() {
        if (totalTrades == 0) return 0.0;
        return (double) winningTrades / totalTrades * 100;
    }
}
