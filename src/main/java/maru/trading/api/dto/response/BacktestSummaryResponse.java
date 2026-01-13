package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.infra.persistence.jpa.entity.BacktestRunEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary response DTO for backtest list.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestSummaryResponse {

    private String backtestId;
    private String strategyId;
    private List<String> symbols;
    private String startDate;
    private String endDate;
    private String timeframe;
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturn;
    private Integer totalTrades;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    /**
     * Create summary from BacktestRunEntity.
     */
    public static BacktestSummaryResponse fromEntity(BacktestRunEntity entity) {
        return BacktestSummaryResponse.builder()
                .backtestId(entity.getBacktestId())
                .strategyId(entity.getStrategyId())
                .symbols(entity.getSymbols() != null ? List.of(entity.getSymbols().split(",")) : List.of())
                .startDate(entity.getStartDate() != null ? entity.getStartDate().toString() : null)
                .endDate(entity.getEndDate() != null ? entity.getEndDate().toString() : null)
                .timeframe(entity.getTimeframe())
                .initialCapital(entity.getInitialCapital())
                .finalCapital(entity.getFinalCapital())
                .totalReturn(entity.getTotalReturn())
                .totalTrades(entity.getTotalTrades())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }
}
