package maru.trading.domain.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for OHLCV bar data (1-minute, daily, etc.).
 * Represents aggregated market data over a time period.
 */
public class MarketBar {

    private final String symbol;
    private final String timeframe; // "1m", "5m", "1d", etc.
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private long volume;
    private final LocalDateTime barTimestamp;
    private boolean closed;

    public MarketBar(
            String symbol,
            String timeframe,
            LocalDateTime barTimestamp) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.barTimestamp = barTimestamp;
        this.closed = false;
        this.volume = 0;
    }

    /**
     * Add a tick to the bar, updating OHLCV values.
     */
    public void addTick(MarketTick tick) {
        if (closed) {
            throw new IllegalStateException("Cannot add tick to closed bar");
        }

        if (open == null) {
            open = tick.getPrice();
            high = tick.getPrice();
            low = tick.getPrice();
        } else {
            if (tick.getPrice().compareTo(high) > 0) {
                high = tick.getPrice();
            }
            if (tick.getPrice().compareTo(low) < 0) {
                low = tick.getPrice();
            }
        }

        close = tick.getPrice();
        volume += tick.getVolume();
    }

    /**
     * Mark bar as closed (no more ticks can be added).
     */
    public void close() {
        this.closed = true;
    }

    /**
     * Validate bar data.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("Timeframe cannot be null or blank");
        }
        if (barTimestamp == null) {
            throw new IllegalArgumentException("Bar timestamp cannot be null");
        }
        if (open == null) {
            throw new IllegalArgumentException("Bar must have at least one tick (open is null)");
        }
    }

    // Getters
    public String getSymbol() {
        return symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public long getVolume() {
        return volume;
    }

    public LocalDateTime getBarTimestamp() {
        return barTimestamp;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public String toString() {
        return "MarketBar{" +
                "symbol='" + symbol + '\'' +
                ", timeframe='" + timeframe + '\'' +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", barTimestamp=" + barTimestamp +
                ", closed=" + closed +
                '}';
    }
}
