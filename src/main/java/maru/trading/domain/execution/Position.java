package maru.trading.domain.execution;

import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain model for position state.
 * Represents current holdings for an account-symbol pair.
 *
 * CRITICAL: Contains core business logic for:
 * - Average price calculation
 * - Realized P&L tracking
 * - Position updates from fills
 */
public class Position {

    private final String positionId;
    private final String accountId;
    private final String symbol;
    private int qty; // Current quantity (positive = long, negative = short, 0 = closed)
    private BigDecimal avgPrice; // Volume-weighted average cost
    private BigDecimal realizedPnl; // Cumulative realized P&L for this symbol

    public Position(
            String positionId,
            String accountId,
            String symbol,
            int qty,
            BigDecimal avgPrice,
            BigDecimal realizedPnl) {
        this.positionId = positionId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.qty = qty;
        this.avgPrice = avgPrice;
        this.realizedPnl = realizedPnl;
    }

    /**
     * Create a new empty position.
     */
    public static Position createEmpty(String positionId, String accountId, String symbol) {
        return new Position(positionId, accountId, symbol, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Apply a BUY fill to the position.
     * Updates quantity and average price.
     */
    public void applyBuyFill(Fill fill) {
        if (fill.getSide() != Side.BUY) {
            throw new IllegalArgumentException("Fill must be BUY side");
        }

        int newQty = this.qty + fill.getFillQty();

        if (this.qty >= 0) {
            // Case 1: Adding to long position or opening long from flat
            // New avg price = (old_qty * old_price + fill_qty * fill_price) / new_qty
            BigDecimal oldValue = this.avgPrice.multiply(BigDecimal.valueOf(this.qty));
            BigDecimal fillValue = fill.getFillPrice().multiply(BigDecimal.valueOf(fill.getFillQty()));
            this.avgPrice = oldValue.add(fillValue).divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);
        } else {
            // Case 2: Covering short position (qty < 0, buying to reduce short)
            if (newQty < 0) {
                // Still short after this fill - no realized P&L, avg price unchanged
                // (short position avg price remains from original short entry)
            } else if (newQty == 0) {
                // Fully covered short position - realize P&L
                BigDecimal realizedPnlFromCover = this.avgPrice.subtract(fill.getFillPrice())
                        .multiply(BigDecimal.valueOf(fill.getFillQty()));
                this.realizedPnl = this.realizedPnl.add(realizedPnlFromCover);
                this.avgPrice = BigDecimal.ZERO; // Reset avg price when flat
            } else {
                // Covered short and went long (newQty > 0)
                // Realize P&L on the covered portion, then calculate new avg price for long
                int coveredQty = Math.abs(this.qty);
                int longQty = newQty;

                BigDecimal realizedPnlFromCover = this.avgPrice.subtract(fill.getFillPrice())
                        .multiply(BigDecimal.valueOf(coveredQty));
                this.realizedPnl = this.realizedPnl.add(realizedPnlFromCover);

                // New avg price is fill price for the long portion
                this.avgPrice = fill.getFillPrice();
            }
        }

        this.qty = newQty;
    }

    /**
     * Apply a SELL fill to the position.
     * Updates quantity, average price, and realized P&L.
     */
    public void applySellFill(Fill fill) {
        if (fill.getSide() != Side.SELL) {
            throw new IllegalArgumentException("Fill must be SELL side");
        }

        int newQty = this.qty - fill.getFillQty();

        if (this.qty <= 0) {
            // Case 1: Adding to short position or opening short from flat
            // New avg price = (abs(old_qty) * old_price + fill_qty * fill_price) / abs(new_qty)
            if (this.qty == 0) {
                // Opening short from flat
                this.avgPrice = fill.getFillPrice();
            } else {
                // Adding to existing short
                BigDecimal oldValue = this.avgPrice.multiply(BigDecimal.valueOf(Math.abs(this.qty)));
                BigDecimal fillValue = fill.getFillPrice().multiply(BigDecimal.valueOf(fill.getFillQty()));
                this.avgPrice = oldValue.add(fillValue).divide(BigDecimal.valueOf(Math.abs(newQty)), 4, RoundingMode.HALF_UP);
            }
        } else {
            // Case 2: Reducing long position (qty > 0, selling to reduce long)
            if (newQty > 0) {
                // Still long after this fill - realize P&L on sold portion, avg price unchanged
                BigDecimal realizedPnlFromSell = fill.getFillPrice().subtract(this.avgPrice)
                        .multiply(BigDecimal.valueOf(fill.getFillQty()));
                this.realizedPnl = this.realizedPnl.add(realizedPnlFromSell);
            } else if (newQty == 0) {
                // Fully closed long position - realize P&L
                BigDecimal realizedPnlFromSell = fill.getFillPrice().subtract(this.avgPrice)
                        .multiply(BigDecimal.valueOf(fill.getFillQty()));
                this.realizedPnl = this.realizedPnl.add(realizedPnlFromSell);
                this.avgPrice = BigDecimal.ZERO; // Reset avg price when flat
            } else {
                // Closed long and went short (newQty < 0)
                // Realize P&L on the closed portion, then calculate new avg price for short
                int closedQty = this.qty;
                int shortQty = Math.abs(newQty);

                BigDecimal realizedPnlFromClose = fill.getFillPrice().subtract(this.avgPrice)
                        .multiply(BigDecimal.valueOf(closedQty));
                this.realizedPnl = this.realizedPnl.add(realizedPnlFromClose);

                // New avg price is fill price for the short portion
                this.avgPrice = fill.getFillPrice();
            }
        }

        this.qty = newQty;
    }

    /**
     * Calculate unrealized P&L based on current market price.
     * For long: (currentPrice - avgPrice) * qty
     * For short: (avgPrice - currentPrice) * abs(qty)
     */
    public BigDecimal calculateUnrealizedPnl(BigDecimal currentPrice) {
        if (qty == 0) {
            return BigDecimal.ZERO;
        }

        if (qty > 0) {
            // Long position
            return currentPrice.subtract(avgPrice).multiply(BigDecimal.valueOf(qty));
        } else {
            // Short position
            return avgPrice.subtract(currentPrice).multiply(BigDecimal.valueOf(Math.abs(qty)));
        }
    }

    /**
     * Check if position is flat (qty == 0).
     */
    public boolean isFlat() {
        return qty == 0;
    }

    /**
     * Check if position is long (qty > 0).
     */
    public boolean isLong() {
        return qty > 0;
    }

    /**
     * Check if position is short (qty < 0).
     */
    public boolean isShort() {
        return qty < 0;
    }

    // Getters and setters
    public String getPositionId() {
        return positionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    @Override
    public String toString() {
        return "Position{" +
                "positionId='" + positionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", qty=" + qty +
                ", avgPrice=" + avgPrice +
                ", realizedPnl=" + realizedPnl +
                '}';
    }
}
