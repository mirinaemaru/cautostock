package maru.trading.domain.execution;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for P&L ledger entry.
 * Append-only record of P&L events (fills, fees, taxes, adjustments).
 */
public class PnlLedger {

    private final String ledgerId;
    private final String accountId;
    private final String symbol;
    private final String eventType; // "FILL", "FEE", "TAX", "ADJUST"
    private final BigDecimal amount; // Signed amount (positive = profit, negative = loss)
    private final String refId; // Reference ID (fill_id, order_id, etc.)
    private final LocalDateTime eventTimestamp;

    public PnlLedger(
            String ledgerId,
            String accountId,
            String symbol,
            String eventType,
            BigDecimal amount,
            String refId,
            LocalDateTime eventTimestamp) {
        this.ledgerId = ledgerId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.eventType = eventType;
        this.amount = amount;
        this.refId = refId;
        this.eventTimestamp = eventTimestamp;
    }

    /**
     * Create ledger entry for fill P&L.
     */
    public static PnlLedger forFill(
            String ledgerId,
            String accountId,
            String symbol,
            BigDecimal realizedPnl,
            String fillId,
            LocalDateTime fillTimestamp) {
        return new PnlLedger(
                ledgerId,
                accountId,
                symbol,
                "FILL",
                realizedPnl,
                fillId,
                fillTimestamp
        );
    }

    /**
     * Create ledger entry for fee.
     */
    public static PnlLedger forFee(
            String ledgerId,
            String accountId,
            String symbol,
            BigDecimal fee,
            String fillId,
            LocalDateTime fillTimestamp) {
        return new PnlLedger(
                ledgerId,
                accountId,
                symbol,
                "FEE",
                fee.negate(), // Fees are costs (negative P&L)
                fillId,
                fillTimestamp
        );
    }

    /**
     * Create ledger entry for tax.
     */
    public static PnlLedger forTax(
            String ledgerId,
            String accountId,
            String symbol,
            BigDecimal tax,
            String fillId,
            LocalDateTime fillTimestamp) {
        return new PnlLedger(
                ledgerId,
                accountId,
                symbol,
                "TAX",
                tax.negate(), // Taxes are costs (negative P&L)
                fillId,
                fillTimestamp
        );
    }

    // Getters
    public String getLedgerId() {
        return ledgerId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getEventType() {
        return eventType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRefId() {
        return refId;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    @Override
    public String toString() {
        return "PnlLedger{" +
                "ledgerId='" + ledgerId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", eventType='" + eventType + '\'' +
                ", amount=" + amount +
                ", refId='" + refId + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
