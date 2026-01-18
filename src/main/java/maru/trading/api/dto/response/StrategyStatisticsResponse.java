package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategyStatisticsResponse {

    private String strategyId;
    private String strategyName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastExecutedAt;

    // 성과 지표
    private BigDecimal totalPnl;
    private BigDecimal totalReturn;
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    private BigDecimal winRate;

    // 리스크 지표
    private BigDecimal maxDrawdown;
    private BigDecimal sharpeRatio;
    private BigDecimal volatility;
    private BigDecimal avgHoldingPeriod;

    // 최근 성과
    private BigDecimal last7DaysPnl;
    private BigDecimal last30DaysPnl;
    private BigDecimal last90DaysPnl;

    // 월별 수익률
    private List<MonthlyReturn> monthlyReturns;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyReturn {
        private String month; // YYYY-MM
        private BigDecimal pnl;
        private BigDecimal returnPct;
        private Integer trades;
    }

    // 전략 목록 응답
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StrategyStatisticsList {
        private List<StrategyStatisticsResponse> strategies;
        private int count;
        private LocalDate fromDate;
        private LocalDate toDate;
    }
}
