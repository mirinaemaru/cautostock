package maru.trading.domain.execution;

import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for fill (execution) event.
 * Represents a partial or full execution of an order.
 */
public class Fill {

    private final String fillId;
    private final String orderId;
    private final String accountId;
    private final String symbol;
    private final Side side;
    private final BigDecimal fillPrice;
    private final int fillQty;
    private final BigDecimal fee;
    private final BigDecimal tax;
    private final LocalDateTime fillTimestamp;
    private final String brokerOrderNo; // For reconciliation with broker

    public Fill(
            String fillId,
            String orderId,
            String accountId,
            String symbol,
            Side side,
            BigDecimal fillPrice,
            int fillQty,
            BigDecimal fee,
            BigDecimal tax,
            LocalDateTime fillTimestamp,
            String brokerOrderNo) {
        this.fillId = fillId;
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.side = side;
        this.fillPrice = fillPrice;
        this.fillQty = fillQty;
        this.fee = fee;
        this.tax = tax;
        this.fillTimestamp = fillTimestamp;
        this.brokerOrderNo = brokerOrderNo;
    }

    /**
     * Calculate gross amount (price * quantity).
     * Positive for buys and sells.
     */
    public BigDecimal calculateGrossAmount() {
        return fillPrice.multiply(BigDecimal.valueOf(fillQty));
    }

    /**
     * Calculate net amount after fees and taxes.
     * For BUY: -(price * qty + fee + tax) (cash outflow)
     * For SELL: +(price * qty - fee - tax) (cash inflow)
     */
    public BigDecimal calculateNetAmount() {
        BigDecimal gross = calculateGrossAmount();
        BigDecimal costs = fee.add(tax);

        if (side == Side.BUY) {
            return gross.add(costs).negate(); // Cash outflow
        } else {
            return gross.subtract(costs); // Cash inflow
        }
    }

    /**
     * Validate fill data.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (fillId == null || fillId.isBlank()) {
            throw new IllegalArgumentException("Fill ID cannot be null or blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (side == null) {
            throw new IllegalArgumentException("Side cannot be null");
        }
        if (fillPrice == null || fillPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fill price must be positive");
        }
        if (fillQty <= 0) {
            throw new IllegalArgumentException("Fill quantity must be positive");
        }
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee cannot be negative");
        }
        if (tax == null || tax.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tax cannot be negative");
        }
        if (fillTimestamp == null) {
            throw new IllegalArgumentException("Fill timestamp cannot be null");
        }
    }

    // Getters
    public String getFillId() {
        return fillId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Side getSide() {
        return side;
    }

    public BigDecimal getFillPrice() {
        return fillPrice;
    }

    public int getFillQty() {
        return fillQty;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public LocalDateTime getFillTimestamp() {
        return fillTimestamp;
    }

    public String getBrokerOrderNo() {
        return brokerOrderNo;
    }

    @Override
    public String toString() {
        return "Fill{" +
                "fillId='" + fillId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side=" + side +
                ", fillPrice=" + fillPrice +
                ", fillQty=" + fillQty +
                ", fee=" + fee +
                ", tax=" + tax +
                ", fillTimestamp=" + fillTimestamp +
                ", brokerOrderNo='" + brokerOrderNo + '\'' +
                '}';
    }
}
