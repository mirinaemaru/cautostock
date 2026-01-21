package maru.trading.domain.backtest.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bar data domain object for backtest.
 *
 * Pure domain object representing OHLCV bar data.
 * Independent from persistence layer (JPA entities).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BarData {

    private String symbol;
    private String timeframe;
    private LocalDateTime timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;

    /**
     * Check if this is a bullish bar (close > open).
     */
    public boolean isBullish() {
        return close != null && open != null && close.compareTo(open) > 0;
    }

    /**
     * Check if this is a bearish bar (close < open).
     */
    public boolean isBearish() {
        return close != null && open != null && close.compareTo(open) < 0;
    }

    /**
     * Get bar range (high - low).
     */
    public BigDecimal getRange() {
        if (high == null || low == null) {
            return BigDecimal.ZERO;
        }
        return high.subtract(low);
    }

    /**
     * Get bar body (|close - open|).
     */
    public BigDecimal getBody() {
        if (close == null || open == null) {
            return BigDecimal.ZERO;
        }
        return close.subtract(open).abs();
    }

    /**
     * Create from basic values (convenience factory).
     */
    public static BarData of(String symbol, String timeframe, LocalDateTime timestamp,
                             BigDecimal open, BigDecimal high, BigDecimal low,
                             BigDecimal close, Long volume) {
        return BarData.builder()
                .symbol(symbol)
                .timeframe(timeframe)
                .timestamp(timestamp)
                .open(open)
                .high(high)
                .low(low)
                .close(close)
                .volume(volume)
                .build();
    }
}
