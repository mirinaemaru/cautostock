package maru.trading.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VaRAnalysisResponse {

    private String accountId;
    private LocalDate analysisDate;
    private String method; // HISTORICAL, PARAMETRIC, MONTE_CARLO
    private Integer confidenceLevel; // 95, 99
    private Integer holdingPeriod; // days

    // VaR 결과
    private BigDecimal portfolioValue;
    private BigDecimal var; // Value at Risk (절대값)
    private BigDecimal varPct; // Value at Risk (%)
    private BigDecimal cvar; // Conditional VaR (Expected Shortfall)
    private BigDecimal cvarPct;

    // 포지션별 VaR
    private List<PositionVaR> positionVaRs;

    // 히스토리컬 시뮬레이션 결과
    private HistoricalSimulation historicalSimulation;

    // 분석 메타데이터
    private Integer dataPoints;
    private LocalDate dataStartDate;
    private LocalDate dataEndDate;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PositionVaR {
        private String symbol;
        private BigDecimal currentValue;
        private BigDecimal var;
        private BigDecimal varPct;
        private BigDecimal contribution; // 전체 VaR에 대한 기여도 %
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoricalSimulation {
        private List<BigDecimal> worstScenarios; // 최악 시나리오 수익률 리스트
        private BigDecimal minReturn;
        private BigDecimal maxReturn;
        private BigDecimal avgReturn;
        private BigDecimal stdDev;
        private Map<String, BigDecimal> percentiles; // 1%, 5%, 10%, 25%, 50%
    }
}
