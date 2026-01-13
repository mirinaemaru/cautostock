package maru.trading.infra.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for persisting market bars (OHLCV data).
 */
@Entity
@Table(name = "market_bars", indexes = {
        @Index(name = "idx_bars_symbol_timeframe_timestamp",
                columnList = "symbol, timeframe, bar_timestamp", unique = true),
        @Index(name = "idx_bars_symbol_timestamp",
                columnList = "symbol, bar_timestamp")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarEntity {

    @Id
    @Column(name = "bar_id", columnDefinition = "CHAR(26)")
    private String barId;

    @Column(name = "symbol", length = 16, nullable = false)
    private String symbol;

    @Column(name = "timeframe", length = 8, nullable = false)
    private String timeframe; // "1m", "5m", "1d", etc.

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

    @Column(name = "bar_timestamp", nullable = false)
    private LocalDateTime barTimestamp;

    @Column(name = "closed", nullable = false)
    private Boolean closed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
