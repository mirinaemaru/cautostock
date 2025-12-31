package maru.trading.domain.execution;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain model for portfolio snapshot.
 * Captures account-level portfolio state at a point in time.
 */
public class PortfolioSnapshot {

    private final String snapshotId;
    private final String accountId;
    private final BigDecimal totalValue; // Cash + position values
    private final BigDecimal cash;
    private final BigDecimal realizedPnl; // Sum of realized P&L from all positions
    private final BigDecimal unrealizedPnl; // Sum of unrealized P&L from all positions
    private final LocalDateTime snapshotTimestamp;

    public PortfolioSnapshot(
            String snapshotId,
            String accountId,
            BigDecimal totalValue,
            BigDecimal cash,
            BigDecimal realizedPnl,
            BigDecimal unrealizedPnl,
            LocalDateTime snapshotTimestamp) {
        this.snapshotId = snapshotId;
        this.accountId = accountId;
        this.totalValue = totalValue;
        this.cash = cash;
        this.realizedPnl = realizedPnl;
        this.unrealizedPnl = unrealizedPnl;
        this.snapshotTimestamp = snapshotTimestamp;
    }

    /**
     * Calculate portfolio snapshot from positions and current prices.
     */
    public static PortfolioSnapshot calculate(
            String snapshotId,
            String accountId,
            List<Position> positions,
            Map<String, BigDecimal> currentPrices,
            BigDecimal cash) {

        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        BigDecimal totalUnrealizedPnl = BigDecimal.ZERO;
        BigDecimal totalPositionValue = BigDecimal.ZERO;

        for (Position position : positions) {
            // Sum realized P&L
            totalRealizedPnl = totalRealizedPnl.add(position.getRealizedPnl());

            // Calculate unrealized P&L if position is not flat
            if (!position.isFlat()) {
                BigDecimal currentPrice = currentPrices.get(position.getSymbol());
                if (currentPrice != null) {
                    BigDecimal unrealizedPnl = position.calculateUnrealizedPnl(currentPrice);
                    totalUnrealizedPnl = totalUnrealizedPnl.add(unrealizedPnl);

                    // Position value = qty * currentPrice (absolute value for longs and shorts)
                    BigDecimal positionValue = currentPrice.multiply(BigDecimal.valueOf(Math.abs(position.getQty())));
                    totalPositionValue = totalPositionValue.add(positionValue);
                }
            }
        }

        BigDecimal totalValue = cash.add(totalPositionValue).add(totalUnrealizedPnl);

        return new PortfolioSnapshot(
                snapshotId,
                accountId,
                totalValue,
                cash,
                totalRealizedPnl,
                totalUnrealizedPnl,
                LocalDateTime.now()
        );
    }

    /**
     * Calculate total P&L (realized + unrealized).
     */
    public BigDecimal getTotalPnl() {
        return realizedPnl.add(unrealizedPnl);
    }

    // Getters
    public String getSnapshotId() {
        return snapshotId;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public LocalDateTime getSnapshotTimestamp() {
        return snapshotTimestamp;
    }

    @Override
    public String toString() {
        return "PortfolioSnapshot{" +
                "snapshotId='" + snapshotId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", totalValue=" + totalValue +
                ", cash=" + cash +
                ", realizedPnl=" + realizedPnl +
                ", unrealizedPnl=" + unrealizedPnl +
                ", snapshotTimestamp=" + snapshotTimestamp +
                '}';
    }
}
