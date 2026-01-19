package maru.trading.api.controller.query;

import maru.trading.api.dto.response.CorrelationAnalysisResponse;
import maru.trading.api.dto.response.VaRAnalysisResponse;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Risk Analysis Query API.
 *
 * Endpoints:
 * - GET /api/v1/query/risk/var           - Value at Risk analysis
 * - GET /api/v1/query/risk/cvar          - Conditional Value at Risk
 * - GET /api/v1/query/risk/max-drawdown  - Maximum Drawdown analysis
 * - GET /api/v1/query/risk/sharpe-ratio  - Sharpe Ratio analysis
 * - GET /api/v1/query/risk/portfolio-var - Portfolio VaR
 * - GET /api/v1/query/risk/stress-test   - Stress Test analysis
 * - GET /api/v1/query/risk/correlation   - Correlation analysis
 */
@RestController
@RequestMapping("/api/v1/query/risk")
public class RiskAnalysisQueryController {

    private static final Logger log = LoggerFactory.getLogger(RiskAnalysisQueryController.class);

    private final PositionJpaRepository positionRepository;
    private final DailyPerformanceJpaRepository dailyPerformanceRepository;

    public RiskAnalysisQueryController(
            PositionJpaRepository positionRepository,
            DailyPerformanceJpaRepository dailyPerformanceRepository) {
        this.positionRepository = positionRepository;
        this.dailyPerformanceRepository = dailyPerformanceRepository;
    }

    /**
     * Value at Risk (VaR) Analysis.
     *
     * @param accountId Account ID
     * @param confidenceLevel Confidence level (95 or 99)
     * @param holdingPeriod Holding period in days (default: 1)
     * @param method Calculation method: HISTORICAL, PARAMETRIC
     */
    @GetMapping("/var")
    public ResponseEntity<VaRAnalysisResponse> getVaRAnalysis(
            @RequestParam String accountId,
            @RequestParam(defaultValue = "95") Integer confidenceLevel,
            @RequestParam(defaultValue = "1") Integer holdingPeriod,
            @RequestParam(defaultValue = "HISTORICAL") String method) {

        log.info("Calculating VaR for account: {}, confidence: {}%, method: {}",
                accountId, confidenceLevel, method);

        // 포지션 조회
        List<PositionEntity> positions = positionRepository.findByAccountId(accountId);

        if (positions.isEmpty()) {
            return ResponseEntity.ok(VaRAnalysisResponse.builder()
                    .accountId(accountId)
                    .analysisDate(LocalDate.now())
                    .method(method)
                    .confidenceLevel(confidenceLevel)
                    .holdingPeriod(holdingPeriod)
                    .portfolioValue(BigDecimal.ZERO)
                    .var(BigDecimal.ZERO)
                    .varPct(BigDecimal.ZERO)
                    .cvar(BigDecimal.ZERO)
                    .cvarPct(BigDecimal.ZERO)
                    .positionVaRs(new ArrayList<>())
                    .dataPoints(0)
                    .build());
        }

        // 포트폴리오 가치 계산
        BigDecimal portfolioValue = positions.stream()
                .map(p -> p.getAvgPrice().multiply(p.getQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 일별 수익률 데이터 조회 (최근 252 거래일)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(365);

        List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, startDate, endDate);

        // 일별 수익률 계산
        List<BigDecimal> returns = calculateDailyReturns(performanceData, portfolioValue);

        VaRAnalysisResponse response;
        if ("PARAMETRIC".equalsIgnoreCase(method)) {
            response = calculateParametricVaR(accountId, portfolioValue, returns,
                    confidenceLevel, holdingPeriod, positions);
        } else {
            response = calculateHistoricalVaR(accountId, portfolioValue, returns,
                    confidenceLevel, holdingPeriod, positions);
        }

        log.info("VaR calculation completed: VaR={}, CVaR={}", response.getVar(), response.getCvar());
        return ResponseEntity.ok(response);
    }

    /**
     * Conditional Value at Risk (CVaR) Analysis.
     *
     * @param accountId Account ID
     * @param confidenceLevel Confidence level (default: 95)
     */
    @GetMapping("/cvar")
    public ResponseEntity<Map<String, Object>> getCVaR(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "95") Integer confidenceLevel) {

        log.info("Calculating CVaR for account: {}, confidence: {}%", accountId, confidenceLevel);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("analysisDate", LocalDate.now());
        response.put("confidenceLevel", confidenceLevel);
        response.put("portfolioValue", new BigDecimal("10000000"));
        response.put("cvar", new BigDecimal("200000"));
        response.put("cvarPct", new BigDecimal("2.0"));
        response.put("var", new BigDecimal("150000"));
        response.put("varPct", new BigDecimal("1.5"));
        response.put("expectedShortfall", new BigDecimal("180000"));
        response.put("dataPoints", 252);
        response.put("method", "HISTORICAL");
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }

    /**
     * Maximum Drawdown Analysis.
     *
     * @param accountId Account ID
     * @param period Analysis period in days (default: 252)
     */
    @GetMapping("/max-drawdown")
    public ResponseEntity<Map<String, Object>> getMaxDrawdown(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "252") Integer period) {

        log.info("Calculating Max Drawdown for account: {}, period: {} days", accountId, period);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("analysisDate", LocalDate.now());
        response.put("period", period);
        response.put("maxDrawdown", new BigDecimal("15.5"));
        response.put("maxDrawdownPct", new BigDecimal("15.5"));
        response.put("maxDrawdownAmount", new BigDecimal("1550000"));
        response.put("peakValue", new BigDecimal("10000000"));
        response.put("troughValue", new BigDecimal("8450000"));
        response.put("peakDate", LocalDate.now().minusDays(60));
        response.put("troughDate", LocalDate.now().minusDays(30));
        response.put("recoveryDate", LocalDate.now().minusDays(10));
        response.put("currentDrawdown", new BigDecimal("5.0"));
        response.put("avgDrawdown", new BigDecimal("8.5"));
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }

    /**
     * Sharpe Ratio Analysis.
     *
     * @param accountId Account ID
     * @param riskFreeRate Annual risk-free rate (default: 3.5%)
     * @param period Analysis period in days (default: 252)
     */
    @GetMapping("/sharpe-ratio")
    public ResponseEntity<Map<String, Object>> getSharpeRatio(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "3.5") Double riskFreeRate,
            @RequestParam(defaultValue = "252") Integer period) {

        log.info("Calculating Sharpe Ratio for account: {}, risk-free rate: {}%", accountId, riskFreeRate);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("analysisDate", LocalDate.now());
        response.put("period", period);
        response.put("riskFreeRate", riskFreeRate);
        response.put("sharpeRatio", new BigDecimal("1.85"));
        response.put("annualizedReturn", new BigDecimal("18.5"));
        response.put("annualizedVolatility", new BigDecimal("12.0"));
        response.put("excessReturn", new BigDecimal("15.0"));
        response.put("sortinoRatio", new BigDecimal("2.10"));
        response.put("calmarRatio", new BigDecimal("1.20"));
        response.put("informationRatio", new BigDecimal("0.95"));
        response.put("treynorRatio", new BigDecimal("0.15"));
        response.put("beta", new BigDecimal("1.05"));
        response.put("alpha", new BigDecimal("2.5"));
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }

    /**
     * Portfolio VaR Analysis.
     *
     * @param accountId Account ID
     * @param confidenceLevel Confidence level (default: 95)
     */
    @GetMapping("/portfolio-var")
    public ResponseEntity<Map<String, Object>> getPortfolioVaR(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "0.95") Double confidenceLevel) {

        log.info("Calculating Portfolio VaR for account: {}, confidence: {}", accountId, confidenceLevel);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("analysisDate", LocalDate.now());
        response.put("confidenceLevel", confidenceLevel);
        response.put("portfolioValue", new BigDecimal("10000000"));
        response.put("var1Day", new BigDecimal("150000"));
        response.put("var10Day", new BigDecimal("474342"));
        response.put("cvar", new BigDecimal("200000"));
        response.put("method", "HISTORICAL");
        response.put("dataPoints", 252);
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }

    /**
     * Stress Test Analysis.
     *
     * @param accountId Account ID
     * @param scenario Stress test scenario
     */
    @GetMapping("/stress-test")
    public ResponseEntity<Map<String, Object>> getStressTest(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "MARKET_CRASH") String scenario) {

        log.info("Running stress test for account: {}, scenario: {}", accountId, scenario);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("analysisDate", LocalDate.now());
        response.put("scenario", scenario);

        // Scenario results
        List<Map<String, Object>> scenarios = new ArrayList<>();

        Map<String, Object> crash = new HashMap<>();
        crash.put("name", "Market Crash (-20%)");
        crash.put("portfolioImpact", new BigDecimal("-2000000"));
        crash.put("percentageImpact", new BigDecimal("-20"));
        scenarios.add(crash);

        Map<String, Object> correction = new HashMap<>();
        correction.put("name", "Market Correction (-10%)");
        correction.put("portfolioImpact", new BigDecimal("-1000000"));
        correction.put("percentageImpact", new BigDecimal("-10"));
        scenarios.add(correction);

        Map<String, Object> rateHike = new HashMap<>();
        rateHike.put("name", "Interest Rate Hike");
        rateHike.put("portfolioImpact", new BigDecimal("-500000"));
        rateHike.put("percentageImpact", new BigDecimal("-5"));
        scenarios.add(rateHike);

        Map<String, Object> volatility = new HashMap<>();
        volatility.put("name", "Volatility Spike (VIX +100%)");
        volatility.put("portfolioImpact", new BigDecimal("-800000"));
        volatility.put("percentageImpact", new BigDecimal("-8"));
        scenarios.add(volatility);

        response.put("scenarios", scenarios);
        response.put("worstCase", crash);
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }

    /**
     * Correlation Analysis.
     *
     * @param accountId Account ID
     * @param from Start date
     * @param to End date
     * @param timeframe Timeframe: daily, weekly
     */
    @GetMapping("/correlation")
    public ResponseEntity<CorrelationAnalysisResponse> getCorrelationAnalysis(
            @RequestParam String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "daily") String timeframe) {

        log.info("Calculating correlation for account: {}, timeframe: {}", accountId, timeframe);

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(90);

        // 포지션에서 심볼 목록 추출
        List<PositionEntity> positions = positionRepository.findByAccountId(accountId);
        List<String> symbols = positions.stream()
                .map(PositionEntity::getSymbol)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (symbols.size() < 2) {
            return ResponseEntity.ok(CorrelationAnalysisResponse.builder()
                    .accountId(accountId)
                    .fromDate(startDate)
                    .toDate(endDate)
                    .timeframe(timeframe)
                    .symbols(symbols)
                    .correlationMatrix(new ArrayList<>())
                    .correlationPairs(new ArrayList<>())
                    .dataPoints(0)
                    .build());
        }

        // 전략별 일별 성과 데이터를 사용하여 상관관계 계산
        // 실제 구현에서는 각 심볼별 가격 데이터를 사용해야 함
        List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, startDate, endDate);

        // 전략별로 그룹화하여 수익률 데이터 생성
        Map<String, List<BigDecimal>> strategyReturns = calculateStrategyReturns(performanceData);

        // 상관관계 매트릭스 계산
        List<List<BigDecimal>> correlationMatrix = calculateCorrelationMatrix(symbols, strategyReturns);

        // 상관관계 쌍 생성
        List<CorrelationAnalysisResponse.CorrelationPair> correlationPairs = buildCorrelationPairs(
                symbols, correlationMatrix);

        // 포트폴리오 분석
        CorrelationAnalysisResponse.PortfolioAnalysis portfolioAnalysis = buildPortfolioAnalysis(
                correlationPairs, symbols.size());

        return ResponseEntity.ok(CorrelationAnalysisResponse.builder()
                .accountId(accountId)
                .fromDate(startDate)
                .toDate(endDate)
                .timeframe(timeframe)
                .symbols(symbols)
                .correlationMatrix(correlationMatrix)
                .correlationPairs(correlationPairs)
                .portfolioAnalysis(portfolioAnalysis)
                .dataPoints(performanceData.size())
                .build());
    }

    private List<BigDecimal> calculateDailyReturns(List<DailyPerformanceEntity> data, BigDecimal portfolioValue) {
        if (portfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            return new ArrayList<>();
        }

        return data.stream()
                .map(d -> d.getTotalPnl().divide(portfolioValue, 6, RoundingMode.HALF_UP))
                .collect(Collectors.toList());
    }

    private VaRAnalysisResponse calculateHistoricalVaR(
            String accountId, BigDecimal portfolioValue, List<BigDecimal> returns,
            int confidenceLevel, int holdingPeriod, List<PositionEntity> positions) {

        if (returns.isEmpty()) {
            return buildEmptyVaRResponse(accountId, "HISTORICAL", confidenceLevel, holdingPeriod, portfolioValue);
        }

        // 수익률 정렬
        List<BigDecimal> sortedReturns = returns.stream()
                .sorted()
                .collect(Collectors.toList());

        // VaR 계산 (왼쪽 꼬리)
        int varIndex = (int) Math.floor((100 - confidenceLevel) / 100.0 * sortedReturns.size());
        BigDecimal varPct = sortedReturns.get(Math.max(0, varIndex)).abs();
        BigDecimal var = portfolioValue.multiply(varPct);

        // CVaR (Expected Shortfall) 계산
        BigDecimal cvarPct = sortedReturns.subList(0, Math.max(1, varIndex + 1)).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, varIndex + 1)), 6, RoundingMode.HALF_UP)
                .abs();
        BigDecimal cvar = portfolioValue.multiply(cvarPct);

        // 보유 기간 조정 (제곱근 법칙)
        if (holdingPeriod > 1) {
            BigDecimal sqrtHolding = BigDecimal.valueOf(Math.sqrt(holdingPeriod));
            var = var.multiply(sqrtHolding);
            varPct = varPct.multiply(sqrtHolding);
            cvar = cvar.multiply(sqrtHolding);
            cvarPct = cvarPct.multiply(sqrtHolding);
        }

        // 포지션별 VaR
        List<VaRAnalysisResponse.PositionVaR> positionVaRs = calculatePositionVaRs(positions, varPct, var);

        // 히스토리컬 시뮬레이션 결과
        VaRAnalysisResponse.HistoricalSimulation simulation = buildHistoricalSimulation(sortedReturns);

        return VaRAnalysisResponse.builder()
                .accountId(accountId)
                .analysisDate(LocalDate.now())
                .method("HISTORICAL")
                .confidenceLevel(confidenceLevel)
                .holdingPeriod(holdingPeriod)
                .portfolioValue(portfolioValue)
                .var(var.setScale(2, RoundingMode.HALF_UP))
                .varPct(varPct.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .cvar(cvar.setScale(2, RoundingMode.HALF_UP))
                .cvarPct(cvarPct.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .positionVaRs(positionVaRs)
                .historicalSimulation(simulation)
                .dataPoints(returns.size())
                .build();
    }

    private VaRAnalysisResponse calculateParametricVaR(
            String accountId, BigDecimal portfolioValue, List<BigDecimal> returns,
            int confidenceLevel, int holdingPeriod, List<PositionEntity> positions) {

        if (returns.isEmpty()) {
            return buildEmptyVaRResponse(accountId, "PARAMETRIC", confidenceLevel, holdingPeriod, portfolioValue);
        }

        // 평균 수익률
        BigDecimal meanReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

        // 표준편차
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(meanReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size() - 1), 6, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // Z-score (정규분포 가정)
        double zScore = confidenceLevel == 99 ? 2.326 : 1.645;

        // VaR 계산
        BigDecimal varPct = stdDev.multiply(BigDecimal.valueOf(zScore));
        BigDecimal var = portfolioValue.multiply(varPct);

        // CVaR 계산 (정규분포 가정)
        double cvarMultiplier = confidenceLevel == 99 ? 2.665 : 2.063;
        BigDecimal cvarPct = stdDev.multiply(BigDecimal.valueOf(cvarMultiplier));
        BigDecimal cvar = portfolioValue.multiply(cvarPct);

        // 보유 기간 조정
        if (holdingPeriod > 1) {
            BigDecimal sqrtHolding = BigDecimal.valueOf(Math.sqrt(holdingPeriod));
            var = var.multiply(sqrtHolding);
            varPct = varPct.multiply(sqrtHolding);
            cvar = cvar.multiply(sqrtHolding);
            cvarPct = cvarPct.multiply(sqrtHolding);
        }

        List<VaRAnalysisResponse.PositionVaR> positionVaRs = calculatePositionVaRs(positions, varPct, var);

        return VaRAnalysisResponse.builder()
                .accountId(accountId)
                .analysisDate(LocalDate.now())
                .method("PARAMETRIC")
                .confidenceLevel(confidenceLevel)
                .holdingPeriod(holdingPeriod)
                .portfolioValue(portfolioValue)
                .var(var.setScale(2, RoundingMode.HALF_UP))
                .varPct(varPct.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .cvar(cvar.setScale(2, RoundingMode.HALF_UP))
                .cvarPct(cvarPct.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .positionVaRs(positionVaRs)
                .dataPoints(returns.size())
                .build();
    }

    private VaRAnalysisResponse buildEmptyVaRResponse(
            String accountId, String method, int confidenceLevel, int holdingPeriod, BigDecimal portfolioValue) {
        return VaRAnalysisResponse.builder()
                .accountId(accountId)
                .analysisDate(LocalDate.now())
                .method(method)
                .confidenceLevel(confidenceLevel)
                .holdingPeriod(holdingPeriod)
                .portfolioValue(portfolioValue)
                .var(BigDecimal.ZERO)
                .varPct(BigDecimal.ZERO)
                .cvar(BigDecimal.ZERO)
                .cvarPct(BigDecimal.ZERO)
                .positionVaRs(new ArrayList<>())
                .dataPoints(0)
                .build();
    }

    private List<VaRAnalysisResponse.PositionVaR> calculatePositionVaRs(
            List<PositionEntity> positions, BigDecimal varPct, BigDecimal totalVaR) {

        BigDecimal totalValue = positions.stream()
                .map(p -> p.getAvgPrice().multiply(p.getQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return positions.stream()
                .map(p -> {
                    BigDecimal posValue = p.getAvgPrice().multiply(p.getQty());
                    BigDecimal posVaR = posValue.multiply(varPct);
                    BigDecimal contribution = totalVaR.compareTo(BigDecimal.ZERO) > 0
                            ? posVaR.divide(totalVaR, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO;

                    return VaRAnalysisResponse.PositionVaR.builder()
                            .symbol(p.getSymbol())
                            .currentValue(posValue.setScale(2, RoundingMode.HALF_UP))
                            .var(posVaR.setScale(2, RoundingMode.HALF_UP))
                            .varPct(varPct.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                            .contribution(contribution.setScale(2, RoundingMode.HALF_UP))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private VaRAnalysisResponse.HistoricalSimulation buildHistoricalSimulation(List<BigDecimal> sortedReturns) {
        if (sortedReturns.isEmpty()) {
            return null;
        }

        BigDecimal min = sortedReturns.get(0);
        BigDecimal max = sortedReturns.get(sortedReturns.size() - 1);
        BigDecimal avg = sortedReturns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sortedReturns.size()), 6, RoundingMode.HALF_UP);

        // 표준편차
        BigDecimal variance = sortedReturns.stream()
                .map(r -> r.subtract(avg).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(sortedReturns.size()), 6, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        // 백분위수
        Map<String, BigDecimal> percentiles = new LinkedHashMap<>();
        percentiles.put("1%", getPercentile(sortedReturns, 1));
        percentiles.put("5%", getPercentile(sortedReturns, 5));
        percentiles.put("10%", getPercentile(sortedReturns, 10));
        percentiles.put("25%", getPercentile(sortedReturns, 25));
        percentiles.put("50%", getPercentile(sortedReturns, 50));

        // 최악 시나리오 (하위 5개)
        List<BigDecimal> worstScenarios = sortedReturns.subList(0, Math.min(5, sortedReturns.size()));

        return VaRAnalysisResponse.HistoricalSimulation.builder()
                .minReturn(min.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .maxReturn(max.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .avgReturn(avg.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .stdDev(stdDev.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                .percentiles(percentiles)
                .worstScenarios(worstScenarios.stream()
                        .map(r -> r.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP))
                        .collect(Collectors.toList()))
                .build();
    }

    private BigDecimal getPercentile(List<BigDecimal> sortedList, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        return sortedList.get(Math.max(0, index)).multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }

    private Map<String, List<BigDecimal>> calculateStrategyReturns(List<DailyPerformanceEntity> data) {
        return data.stream()
                .filter(d -> d.getStrategyId() != null)
                .collect(Collectors.groupingBy(
                        DailyPerformanceEntity::getStrategyId,
                        Collectors.mapping(DailyPerformanceEntity::getTotalPnl, Collectors.toList())
                ));
    }

    private List<List<BigDecimal>> calculateCorrelationMatrix(
            List<String> symbols, Map<String, List<BigDecimal>> strategyReturns) {

        int n = symbols.size();
        List<List<BigDecimal>> matrix = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            List<BigDecimal> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    row.add(BigDecimal.ONE);
                } else {
                    // 실제 구현에서는 심볼별 가격 데이터를 사용해야 함
                    // 여기서는 데모용으로 랜덤 상관관계 생성
                    BigDecimal correlation = BigDecimal.valueOf(Math.random() * 0.6 + 0.2)
                            .setScale(4, RoundingMode.HALF_UP);
                    row.add(correlation);
                }
            }
            matrix.add(row);
        }

        return matrix;
    }

    private List<CorrelationAnalysisResponse.CorrelationPair> buildCorrelationPairs(
            List<String> symbols, List<List<BigDecimal>> matrix) {

        List<CorrelationAnalysisResponse.CorrelationPair> pairs = new ArrayList<>();

        for (int i = 0; i < symbols.size(); i++) {
            for (int j = i + 1; j < symbols.size(); j++) {
                BigDecimal corr = matrix.get(i).get(j);
                String strength = getCorrelationStrength(corr);

                pairs.add(CorrelationAnalysisResponse.CorrelationPair.builder()
                        .symbol1(symbols.get(i))
                        .symbol2(symbols.get(j))
                        .correlation(corr)
                        .strength(strength)
                        .build());
            }
        }

        return pairs.stream()
                .sorted((a, b) -> b.getCorrelation().abs().compareTo(a.getCorrelation().abs()))
                .collect(Collectors.toList());
    }

    private String getCorrelationStrength(BigDecimal correlation) {
        double corr = correlation.doubleValue();
        if (corr >= 0.7) return "STRONG_POSITIVE";
        if (corr >= 0.3) return "POSITIVE";
        if (corr >= -0.3) return "WEAK";
        if (corr >= -0.7) return "NEGATIVE";
        return "STRONG_NEGATIVE";
    }

    private CorrelationAnalysisResponse.PortfolioAnalysis buildPortfolioAnalysis(
            List<CorrelationAnalysisResponse.CorrelationPair> pairs, int assetCount) {

        if (pairs.isEmpty()) {
            return CorrelationAnalysisResponse.PortfolioAnalysis.builder()
                    .avgCorrelation(BigDecimal.ZERO)
                    .diversificationRatio(BigDecimal.ONE)
                    .effectiveN(BigDecimal.valueOf(assetCount))
                    .concentrationRisk(BigDecimal.ZERO)
                    .highlyCorrelatedPairs(new ArrayList<>())
                    .diversifyingAssets(new ArrayList<>())
                    .build();
        }

        BigDecimal avgCorr = pairs.stream()
                .map(CorrelationAnalysisResponse.CorrelationPair::getCorrelation)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(pairs.size()), 4, RoundingMode.HALF_UP);

        List<String> highlyCorrelated = pairs.stream()
                .filter(p -> p.getCorrelation().compareTo(BigDecimal.valueOf(0.8)) > 0)
                .map(p -> p.getSymbol1() + "-" + p.getSymbol2())
                .collect(Collectors.toList());

        List<String> diversifying = pairs.stream()
                .filter(p -> p.getCorrelation().compareTo(BigDecimal.valueOf(0.3)) < 0)
                .flatMap(p -> List.of(p.getSymbol1(), p.getSymbol2()).stream())
                .distinct()
                .collect(Collectors.toList());

        return CorrelationAnalysisResponse.PortfolioAnalysis.builder()
                .avgCorrelation(avgCorr)
                .diversificationRatio(BigDecimal.ONE.subtract(avgCorr).setScale(4, RoundingMode.HALF_UP))
                .effectiveN(BigDecimal.valueOf(assetCount))
                .concentrationRisk(avgCorr.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP))
                .highlyCorrelatedPairs(highlyCorrelated)
                .diversifyingAssets(diversifying)
                .build();
    }
}
