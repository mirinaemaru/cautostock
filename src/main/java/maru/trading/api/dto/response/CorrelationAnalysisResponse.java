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
public class CorrelationAnalysisResponse {

    private String accountId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer dataPoints;
    private String timeframe; // daily, weekly

    // 상관관계 매트릭스
    private List<String> symbols;
    private List<List<BigDecimal>> correlationMatrix;

    // 개별 상관관계 쌍
    private List<CorrelationPair> correlationPairs;

    // 포트폴리오 분석
    private PortfolioAnalysis portfolioAnalysis;

    // 전략 간 상관관계 (옵션)
    private List<StrategyCorrelation> strategyCorrelations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CorrelationPair {
        private String symbol1;
        private String symbol2;
        private BigDecimal correlation;
        private String strength; // STRONG_POSITIVE, POSITIVE, WEAK, NEGATIVE, STRONG_NEGATIVE
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PortfolioAnalysis {
        private BigDecimal avgCorrelation;
        private BigDecimal diversificationRatio;
        private BigDecimal effectiveN; // 유효 자산 수
        private BigDecimal concentrationRisk;
        private List<String> highlyCorrelatedPairs; // 상관계수 > 0.8인 쌍
        private List<String> diversifyingAssets; // 낮은 상관관계 자산
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StrategyCorrelation {
        private String strategy1Id;
        private String strategy1Name;
        private String strategy2Id;
        private String strategy2Name;
        private BigDecimal correlation;
    }
}
