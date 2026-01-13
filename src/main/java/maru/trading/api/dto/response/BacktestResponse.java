package maru.trading.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.BacktestResult;
import maru.trading.domain.backtest.PerformanceMetrics;
import maru.trading.domain.backtest.RiskMetrics;
import maru.trading.infra.persistence.jpa.entity.BacktestRunEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for backtest result.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BacktestResponse {

    private String backtestId;
    private String strategyId;
    private List<String> symbols;
    private String startDate;
    private String endDate;
    private String timeframe;

    // Capital
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturn;    // Percentage

    // Execution info
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;             // COMPLETED, FAILED, RUNNING
    private String errorMessage;

    // Performance Metrics
    private PerformanceMetricsDTO performance;

    // Risk Metrics
    private RiskMetricsDTO risk;

    // Summary
    private Integer totalTrades;

    /**
     * Create response from domain BacktestResult.
     */
    public static BacktestResponse fromDomain(BacktestResult result) {
        PerformanceMetrics perf = result.getPerformanceMetrics();
        RiskMetrics risk = result.getRiskMetrics();
        BacktestConfig config = result.getConfig();

        return BacktestResponse.builder()
                .backtestId(result.getBacktestId())
                .strategyId(config != null ? config.getStrategyId() : null)
                .symbols(config != null ? config.getSymbols() : List.of())
                .startDate(config != null && config.getStartDate() != null ? config.getStartDate().toString() : null)
                .endDate(config != null && config.getEndDate() != null ? config.getEndDate().toString() : null)
                .timeframe(config != null ? config.getTimeframe() : null)
                .initialCapital(config != null ? config.getInitialCapital() : null)
                .finalCapital(result.getFinalCapital())
                .totalReturn(result.getTotalReturn())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .status("COMPLETED")
                .performance(perf != null ? PerformanceMetricsDTO.fromDomain(perf) : null)
                .risk(risk != null ? RiskMetricsDTO.fromDomain(risk) : null)
                .totalTrades(result.getTrades() != null ? result.getTrades().size() : 0)
                .build();
    }

    /**
     * Create response from BacktestRunEntity.
     */
    public static BacktestResponse fromEntity(BacktestRunEntity entity) {
        return BacktestResponse.builder()
                .backtestId(entity.getBacktestId())
                .strategyId(entity.getStrategyId())
                .symbols(entity.getSymbols() != null ? List.of(entity.getSymbols().split(",")) : List.of())
                .startDate(entity.getStartDate() != null ? entity.getStartDate().toLocalDate().toString() : null)
                .endDate(entity.getEndDate() != null ? entity.getEndDate().toLocalDate().toString() : null)
                .timeframe(entity.getTimeframe())
                .initialCapital(entity.getInitialCapital())
                .finalCapital(entity.getFinalCapital())
                .totalReturn(entity.getTotalReturn())
                .startTime(entity.getCreatedAt())
                .endTime(entity.getCompletedAt())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .totalTrades(entity.getTotalTrades())
                // Performance and Risk metrics would need separate queries
                .build();
    }

    /**
     * Create error response.
     */
    public static BacktestResponse error(String errorMessage) {
        return BacktestResponse.builder()
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Performance Metrics DTO.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerformanceMetricsDTO {
        private BigDecimal totalReturn;
        private BigDecimal annualReturn;
        private BigDecimal sharpeRatio;
        private BigDecimal sortinoRatio;
        private BigDecimal maxDrawdown;
        private Integer maxDrawdownDuration;
        private Integer totalTrades;
        private Integer winningTrades;
        private Integer losingTrades;
        private BigDecimal winRate;
        private BigDecimal profitFactor;
        private BigDecimal avgWin;
        private BigDecimal avgLoss;
        private BigDecimal avgTrade;
        private BigDecimal largestWin;
        private BigDecimal largestLoss;
        private Integer maxConsecutiveWins;
        private Integer maxConsecutiveLosses;

        public static PerformanceMetricsDTO fromDomain(PerformanceMetrics metrics) {
            return PerformanceMetricsDTO.builder()
                    .totalReturn(metrics.getTotalReturn())
                    .annualReturn(metrics.getAnnualReturn())
                    .sharpeRatio(metrics.getSharpeRatio())
                    .sortinoRatio(metrics.getSortinoRatio())
                    .maxDrawdown(metrics.getMaxDrawdown())
                    .maxDrawdownDuration(metrics.getMaxDrawdownDuration())
                    .totalTrades(metrics.getTotalTrades())
                    .winningTrades(metrics.getWinningTrades())
                    .losingTrades(metrics.getLosingTrades())
                    .winRate(metrics.getWinRate())
                    .profitFactor(metrics.getProfitFactor())
                    .avgWin(metrics.getAvgWin())
                    .avgLoss(metrics.getAvgLoss())
                    .avgTrade(metrics.getAvgTrade())
                    .largestWin(metrics.getLargestWin())
                    .largestLoss(metrics.getLargestLoss())
                    .maxConsecutiveWins(metrics.getMaxConsecutiveWins())
                    .maxConsecutiveLosses(metrics.getMaxConsecutiveLosses())
                    .build();
        }
    }

    /**
     * Risk Metrics DTO.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskMetricsDTO {
        private BigDecimal volatility;
        private BigDecimal downsideDeviation;
        private BigDecimal var95;
        private BigDecimal cvar95;
        private BigDecimal calmarRatio;
        private BigDecimal recoveryFactor;

        public static RiskMetricsDTO fromDomain(RiskMetrics metrics) {
            return RiskMetricsDTO.builder()
                    .volatility(metrics.getVolatility())
                    .downsideDeviation(metrics.getDownsideDeviation())
                    .var95(metrics.getVar95())
                    .cvar95(metrics.getCvar95())
                    .calmarRatio(metrics.getCalmarRatio())
                    .recoveryFactor(metrics.getRecoveryFactor())
                    .build();
        }
    }
}
