package maru.trading.api.dto.response;

import lombok.Builder;
import lombok.Getter;
import maru.trading.domain.backtest.montecarlo.MonteCarloResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response DTO for Monte Carlo simulation.
 */
@Getter
@Builder
public class MonteCarloResponse {

    private String simulationId;
    private int numSimulations;
    private String method;

    // Return statistics
    private BigDecimal meanReturn;
    private BigDecimal medianReturn;
    private BigDecimal stdDevReturn;
    private BigDecimal minReturn;
    private BigDecimal maxReturn;

    // Risk metrics
    private BigDecimal valueAtRisk;
    private BigDecimal conditionalVaR;
    private DrawdownStats maxDrawdownStats;

    // Probabilities
    private BigDecimal probabilityOfProfit;
    private BigDecimal probabilityOfTargetReturn;
    private BigDecimal targetReturn;
    private BigDecimal probabilityOfRuin;
    private BigDecimal ruinThreshold;

    // Percentiles
    private Map<Integer, BigDecimal> returnPercentiles;
    private Map<Integer, BigDecimal> drawdownPercentiles;

    // Distribution
    private List<DistributionBin> returnDistribution;

    // Confidence intervals
    private BigDecimal[] returnConfidenceInterval95;
    private BigDecimal[] returnConfidenceInterval99;

    // Best/Worst/Median cases
    private SimulationPathSummary bestCase;
    private SimulationPathSummary worstCase;
    private SimulationPathSummary medianCase;

    // Execution info
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;

    // Error handling
    private String error;

    @Getter
    @Builder
    public static class DrawdownStats {
        private BigDecimal meanMaxDrawdown;
        private BigDecimal medianMaxDrawdown;
        private BigDecimal stdDevMaxDrawdown;
        private BigDecimal worstMaxDrawdown;
        private BigDecimal bestMaxDrawdown;
    }

    @Getter
    @Builder
    public static class DistributionBin {
        private BigDecimal binStart;
        private BigDecimal binEnd;
        private BigDecimal binCenter;
        private int count;
        private BigDecimal frequency;
    }

    @Getter
    @Builder
    public static class SimulationPathSummary {
        private int simulationNumber;
        private BigDecimal totalReturn;
        private BigDecimal maxDrawdown;
        private BigDecimal finalEquity;
    }

    /**
     * Create response from domain object.
     */
    public static MonteCarloResponse fromDomain(MonteCarloResult result) {
        MonteCarloResponseBuilder builder = MonteCarloResponse.builder()
                .simulationId(result.getSimulationId())
                .numSimulations(result.getNumSimulations())
                .method(result.getConfig().getMethod().name())
                // Return statistics
                .meanReturn(result.getMeanReturn())
                .medianReturn(result.getMedianReturn())
                .stdDevReturn(result.getStdDevReturn())
                .minReturn(result.getMinReturn())
                .maxReturn(result.getMaxReturn())
                // Risk metrics
                .valueAtRisk(result.getValueAtRisk())
                .conditionalVaR(result.getConditionalVaR())
                // Probabilities
                .probabilityOfProfit(result.getProbabilityOfProfit())
                .probabilityOfTargetReturn(result.getProbabilityOfTargetReturn())
                .targetReturn(result.getTargetReturn())
                .probabilityOfRuin(result.getProbabilityOfRuin())
                .ruinThreshold(result.getRuinThreshold())
                // Percentiles
                .returnPercentiles(result.getReturnPercentiles())
                .drawdownPercentiles(result.getDrawdownPercentiles())
                // Confidence intervals
                .returnConfidenceInterval95(result.getReturnConfidenceInterval95())
                .returnConfidenceInterval99(result.getReturnConfidenceInterval99())
                // Execution info
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .durationMs(result.getDurationMs());

        // Drawdown stats
        if (result.getMaxDrawdownStats() != null) {
            builder.maxDrawdownStats(DrawdownStats.builder()
                    .meanMaxDrawdown(result.getMaxDrawdownStats().getMeanMaxDrawdown())
                    .medianMaxDrawdown(result.getMaxDrawdownStats().getMedianMaxDrawdown())
                    .stdDevMaxDrawdown(result.getMaxDrawdownStats().getStdDevMaxDrawdown())
                    .worstMaxDrawdown(result.getMaxDrawdownStats().getWorstMaxDrawdown())
                    .bestMaxDrawdown(result.getMaxDrawdownStats().getBestMaxDrawdown())
                    .build());
        }

        // Distribution
        if (result.getReturnDistribution() != null) {
            builder.returnDistribution(result.getReturnDistribution().stream()
                    .map(bin -> DistributionBin.builder()
                            .binStart(bin.getBinStart())
                            .binEnd(bin.getBinEnd())
                            .binCenter(bin.getBinCenter())
                            .count(bin.getCount())
                            .frequency(bin.getFrequency())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Best/Worst/Median
        if (result.getBestCase() != null) {
            builder.bestCase(SimulationPathSummary.builder()
                    .simulationNumber(result.getBestCase().getSimulationNumber())
                    .totalReturn(result.getBestCase().getTotalReturn())
                    .maxDrawdown(result.getBestCase().getMaxDrawdown())
                    .finalEquity(result.getBestCase().getFinalEquity())
                    .build());
        }

        if (result.getWorstCase() != null) {
            builder.worstCase(SimulationPathSummary.builder()
                    .simulationNumber(result.getWorstCase().getSimulationNumber())
                    .totalReturn(result.getWorstCase().getTotalReturn())
                    .maxDrawdown(result.getWorstCase().getMaxDrawdown())
                    .finalEquity(result.getWorstCase().getFinalEquity())
                    .build());
        }

        if (result.getMedianCase() != null) {
            builder.medianCase(SimulationPathSummary.builder()
                    .simulationNumber(result.getMedianCase().getSimulationNumber())
                    .totalReturn(result.getMedianCase().getTotalReturn())
                    .maxDrawdown(result.getMedianCase().getMaxDrawdown())
                    .finalEquity(result.getMedianCase().getFinalEquity())
                    .build());
        }

        return builder.build();
    }

    /**
     * Create error response.
     */
    public static MonteCarloResponse error(String message) {
        return MonteCarloResponse.builder()
                .error(message)
                .build();
    }
}
