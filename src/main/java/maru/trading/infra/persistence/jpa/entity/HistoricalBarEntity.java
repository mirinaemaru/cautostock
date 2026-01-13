package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for historical market bars (for backtesting).
 *
 * Stores OHLCV data for historical analysis and strategy backtesting.
 * Different from BarEntity which stores real-time aggregated bars.
 */
@Entity
@Table(name = "historical_bars", indexes = {
        @Index(name = "idx_symbol_timestamp", columnList = "symbol, bar_timestamp")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_symbol_timeframe_timestamp",
                columnNames = {"symbol", "timeframe", "bar_timestamp"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalBarEntity {

    @Id
    @Column(name = "bar_id", columnDefinition = "CHAR(26)")
    private String barId;

    @Column(name = "symbol", length = 16, nullable = false)
    private String symbol;

    /**
     * Timeframe: 1m, 5m, 15m, 1h, 1d, etc.
     */
    @Column(name = "timeframe", length = 8, nullable = false)
    private String timeframe;

    /**
     * Bar timestamp (bar opening time).
     */
    @Column(name = "bar_timestamp", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime barTimestamp;

    /**
     * OHLCV data.
     */
    @Column(name = "open_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal lowPrice;

    @Column(name = "close_price", precision = 18, scale = 4, nullable = false)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false)
    private Long volume;

    /**
     * Record creation timestamp.
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
