package maru.trading.domain.backtest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.order.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Backtest trade (entry + exit).
 *
 * Represents a complete round-trip trade during backtest simulation.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTrade {

    private String tradeId;
    private String backtestId;
    private String symbol;

    // Entry
    private LocalDateTime entryTime;
    private BigDecimal entryPrice;
    private BigDecimal entryQty;
    private Side side;

    // Exit
    private LocalDateTime exitTime;
    private BigDecimal exitPrice;
    private BigDecimal exitQty;

    // P&L
    private BigDecimal grossPnl;
    private BigDecimal commissionPaid;
    private BigDecimal slippageCost;
    private BigDecimal netPnl;
    private BigDecimal returnPct;

    // Metadata
    private String signalReason;
    private String status; // OPEN, CLOSED

    /**
     * Check if trade is a winner (net profit > 0).
     */
    public boolean isWinner() {
        return netPnl != null && netPnl.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if trade is a loser (net profit < 0).
     */
    public boolean isLoser() {
        return netPnl != null && netPnl.compareTo(BigDecimal.ZERO) < 0;
    }
}
