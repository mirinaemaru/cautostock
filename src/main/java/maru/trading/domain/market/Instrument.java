package maru.trading.domain.market;

import java.math.BigDecimal;

/**
 * Domain model for tradable instrument (stock, futures, etc.).
 * Represents instrument metadata and trading rules.
 */
public class Instrument {

    private final String symbol;
    private final String market;
    private final boolean tradable;
    private final boolean halted;
    private final BigDecimal tickSize;
    private final int lotSize;

    public Instrument(
            String symbol,
            String market,
            boolean tradable,
            boolean halted,
            BigDecimal tickSize,
            int lotSize) {
        this.symbol = symbol;
        this.market = market;
        this.tradable = tradable;
        this.halted = halted;
        this.tickSize = tickSize;
        this.lotSize = lotSize;
    }

    /**
     * Check if instrument can be traded.
     * Returns false if instrument is not tradable or is halted.
     */
    public boolean isTradeAllowed() {
        return tradable && !halted;
    }

    /**
     * Validate instrument configuration.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (market == null || market.isBlank()) {
            throw new IllegalArgumentException("Market cannot be null or blank");
        }
        if (tickSize == null || tickSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tick size must be positive");
        }
        if (lotSize <= 0) {
            throw new IllegalArgumentException("Lot size must be positive");
        }
    }

    // Getters
    public String getSymbol() {
        return symbol;
    }

    public String getMarket() {
        return market;
    }

    public boolean isTradable() {
        return tradable;
    }

    public boolean isHalted() {
        return halted;
    }

    public BigDecimal getTickSize() {
        return tickSize;
    }

    public int getLotSize() {
        return lotSize;
    }

    @Override
    public String toString() {
        return "Instrument{" +
                "symbol='" + symbol + '\'' +
                ", market='" + market + '\'' +
                ", tradable=" + tradable +
                ", halted=" + halted +
                ", tickSize=" + tickSize +
                ", lotSize=" + lotSize +
                '}';
    }
}
