package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerformanceResponse {

    private String accountId;
    private String strategyId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String period; // daily, weekly, monthly

    // 총 성과 지표
    private BigDecimal totalPnl;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal totalReturn; // 수익률 %
    private BigDecimal totalVolume;
    private BigDecimal totalFees;

    // 거래 통계
    private Integer totalTrades;
    private Integer winningTrades;
    private Integer losingTrades;
    private BigDecimal winRate;
    private BigDecimal avgWin;
    private BigDecimal avgLoss;
    private BigDecimal profitFactor;

    // 리스크 지표
    private BigDecimal maxDrawdown;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal volatility;

    // 일별 성과 데이터
    private List<DailyPerformance> dailyPerformances;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyPerformance {
        private LocalDate date;
        private BigDecimal pnl;
        private BigDecimal cumulativePnl;
        private BigDecimal returnPct;
        private Integer trades;
        private Integer wins;
        private Integer losses;
    }
}
