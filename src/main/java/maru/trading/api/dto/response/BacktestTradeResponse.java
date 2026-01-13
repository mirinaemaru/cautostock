package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.infra.persistence.jpa.entity.BacktestTradeEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for backtest trade.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestTradeResponse {

    private String tradeId;
    private String backtestId;
    private String symbol;
    private String side;

    // Entry
    private LocalDateTime entryTime;
    private BigDecimal entryPrice;
    private BigDecimal entryQty;

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
    private String status;

    /**
     * Create response from BacktestTradeEntity.
     */
    public static BacktestTradeResponse fromEntity(BacktestTradeEntity entity) {
        return BacktestTradeResponse.builder()
                .tradeId(entity.getTradeId())
                .backtestId(entity.getBacktestId())
                .symbol(entity.getSymbol())
                .side(entity.getSide())
                .entryTime(entity.getEntryTime())
                .entryPrice(entity.getEntryPrice())
                .entryQty(entity.getEntryQty())
                .exitTime(entity.getExitTime())
                .exitPrice(entity.getExitPrice())
                .exitQty(entity.getExitQty())
                .grossPnl(entity.getGrossPnl())
                .commissionPaid(entity.getCommissionPaid())
                .slippageCost(entity.getSlippageCost())
                .netPnl(entity.getNetPnl())
                .returnPct(entity.getReturnPct())
                .signalReason(entity.getSignalReason())
                .status(entity.getStatus())
                .build();
    }
}
