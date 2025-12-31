package maru.trading.domain.market;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Domain model for real-time market tick data.
 * Represents a single tick event from WebSocket stream.
 */
public class MarketTick implements Comparable<MarketTick> {

    private final String symbol;
    private final BigDecimal price;
    private final long volume;
    private final LocalDateTime timestamp;
    private final String tradingStatus; // "NORMAL", "HALTED", "VI", etc.

    public MarketTick(
            String symbol,
            BigDecimal price,
            long volume,
            LocalDateTime timestamp,
            String tradingStatus) {
        this.symbol = symbol;
        this.price = price;
        this.volume = volume;
        this.timestamp = timestamp;
        this.tradingStatus = tradingStatus;
    }

    /**
     * Validate tick data.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (volume < 0) {
            throw new IllegalArgumentException("Volume cannot be negative");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
    }

    /**
     * Check if tick data is delayed beyond the given threshold.
     */
    public boolean isDelayed(Duration threshold) {
        Duration age = Duration.between(timestamp, LocalDateTime.now());
        return age.compareTo(threshold) > 0;
    }

    @Override
    public int compareTo(MarketTick other) {
        return this.timestamp.compareTo(other.timestamp);
    }

    // Getters
    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getVolume() {
        return volume;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTradingStatus() {
        return tradingStatus;
    }

    @Override
    public String toString() {
        return "MarketTick{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", volume=" + volume +
                ", timestamp=" + timestamp +
                ", tradingStatus='" + tradingStatus + '\'' +
                '}';
    }
}
