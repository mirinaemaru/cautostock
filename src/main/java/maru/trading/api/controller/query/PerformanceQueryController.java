package maru.trading.api.controller.query;

import maru.trading.api.dto.response.PerformanceResponse;
import maru.trading.api.dto.response.StrategyStatisticsResponse;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.FillEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.FillJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Performance Analysis Query API.
 *
 * Endpoints:
 * - GET /api/v1/query/performance                    - Get account performance
 * - GET /api/v1/query/performance/strategies         - Get all strategies statistics
 * - GET /api/v1/query/performance/strategies/{id}    - Get specific strategy statistics
 */
@RestController
@RequestMapping("/api/v1/query/performance")
public class PerformanceQueryController {

    private static final Logger log = LoggerFactory.getLogger(PerformanceQueryController.class);

    private final DailyPerformanceJpaRepository dailyPerformanceRepository;
    private final FillJpaRepository fillRepository;
    private final StrategyJpaRepository strategyRepository;

    public PerformanceQueryController(
            DailyPerformanceJpaRepository dailyPerformanceRepository,
            FillJpaRepository fillRepository,
            StrategyJpaRepository strategyRepository) {
        this.dailyPerformanceRepository = dailyPerformanceRepository;
        this.fillRepository = fillRepository;
        this.strategyRepository = strategyRepository;
    }

    /**
     * Get account performance analysis.
     *
     * @param accountId Account ID (optional)
     * @param from Start date (default: 30 days ago) - alias: startDate
     * @param to End date (default: today) - alias: endDate
     * @param period Aggregation period: daily, weekly, monthly
     */
    @GetMapping
    public ResponseEntity<PerformanceResponse> getPerformance(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) String strategyId) {

        // Support both from/to and startDate/endDate parameters
        LocalDate effectiveStart = from != null ? from : (startDate != null ? startDate : LocalDate.now().minusDays(30));
        LocalDate effectiveEnd = to != null ? to : (endDate != null ? endDate : LocalDate.now());

        log.info("Fetching performance for account: {}, period: {}-{}", accountId, effectiveStart, effectiveEnd);

        LocalDate finalEndDate = effectiveEnd;
        LocalDate finalStartDate = effectiveStart;

        // Handle case when accountId is null
        if (accountId == null || accountId.isEmpty()) {
            // Return summary response for all accounts
            return ResponseEntity.ok(PerformanceResponse.builder()
                    .accountId("all")
                    .fromDate(finalStartDate)
                    .toDate(finalEndDate)
                    .period(period)
                    .totalPnl(BigDecimal.ZERO)
                    .totalTrades(0)
                    .winningTrades(0)
                    .losingTrades(0)
                    .winRate(BigDecimal.ZERO)
                    .dailyPerformances(new ArrayList<>())
                    .build());
        }

        // 일별 성과 데이터 조회
        List<DailyPerformanceEntity> dailyData = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, finalStartDate, finalEndDate);

        // 체결 데이터로 실시간 성과 계산 (일별 데이터가 없는 경우)
        if (dailyData.isEmpty()) {
            return ResponseEntity.ok(calculatePerformanceFromFills(accountId, finalStartDate, finalEndDate, period));
        }

        // 성과 지표 계산
        PerformanceResponse response = buildPerformanceResponse(accountId, finalStartDate, finalEndDate, period, dailyData);

        return ResponseEntity.ok(response);
    }

    /**
     * Get statistics for all strategies.
     */
    @GetMapping("/strategies")
    public ResponseEntity<StrategyStatisticsResponse.StrategyStatisticsList> getAllStrategiesStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Fetching all strategies statistics");

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(90);

        List<StrategyEntity> strategies = strategyRepository.findAll();
        List<StrategyStatisticsResponse> statisticsList = strategies.stream()
                .map(strategy -> buildStrategyStatistics(strategy, startDate, endDate))
                .collect(Collectors.toList());

        return ResponseEntity.ok(StrategyStatisticsResponse.StrategyStatisticsList.builder()
                .strategies(statisticsList)
                .count(statisticsList.size())
                .fromDate(startDate)
                .toDate(endDate)
                .build());
    }

    /**
     * Get statistics for a specific strategy.
     */
    @GetMapping("/strategies/{strategyId}")
    public ResponseEntity<StrategyStatisticsResponse> getStrategyStatistics(
            @PathVariable String strategyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Fetching statistics for strategy: {}", strategyId);

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(90);

        return strategyRepository.findById(strategyId)
                .map(strategy -> ResponseEntity.ok(buildStrategyStatistics(strategy, startDate, endDate)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get performance summary (simplified overview).
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getPerformanceSummary(
            @RequestParam(required = false) String accountId) {

        log.info("Fetching performance summary for account: {}", accountId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("accountId", accountId);
        summary.put("fromDate", startDate);
        summary.put("toDate", endDate);
        summary.put("totalPnl", BigDecimal.ZERO);
        summary.put("totalTrades", 0);
        summary.put("winRate", BigDecimal.ZERO);
        summary.put("status", "OK");

        if (accountId != null) {
            List<DailyPerformanceEntity> dailyData = dailyPerformanceRepository
                    .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, startDate, endDate);

            if (!dailyData.isEmpty()) {
                BigDecimal totalPnl = dailyData.stream()
                        .map(DailyPerformanceEntity::getTotalPnl)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int totalTrades = dailyData.stream()
                        .mapToInt(DailyPerformanceEntity::getTotalTrades)
                        .sum();
                int winningTrades = dailyData.stream()
                        .mapToInt(DailyPerformanceEntity::getWinningTrades)
                        .sum();

                BigDecimal winRate = totalTrades > 0
                        ? BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                summary.put("totalPnl", totalPnl);
                summary.put("totalTrades", totalTrades);
                summary.put("winRate", winRate);
            }
        }

        return ResponseEntity.ok(summary);
    }

    /**
     * Get performance analysis (detailed analysis report).
     */
    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> getPerformanceAnalysis(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String strategyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching performance analysis for account: {}, strategy: {}", accountId, strategyId);

        // Support both from/to and startDate/endDate parameters
        LocalDate effectiveStart = from != null ? from : (startDate != null ? startDate : LocalDate.now().minusDays(30));
        LocalDate effectiveEnd = to != null ? to : (endDate != null ? endDate : LocalDate.now());

        Map<String, Object> analysis = new java.util.HashMap<>();
        analysis.put("accountId", accountId);
        analysis.put("strategyId", strategyId);
        analysis.put("fromDate", effectiveStart);
        analysis.put("toDate", effectiveEnd);
        analysis.put("totalPnl", BigDecimal.ZERO);
        analysis.put("totalTrades", 0);
        analysis.put("winRate", 0);
        analysis.put("maxDrawdown", BigDecimal.ZERO);
        analysis.put("sharpeRatio", BigDecimal.ZERO);
        analysis.put("sortinoRatio", BigDecimal.ZERO);
        analysis.put("profitFactor", BigDecimal.ZERO);
        analysis.put("avgWin", BigDecimal.ZERO);
        analysis.put("avgLoss", BigDecimal.ZERO);
        analysis.put("maxConsecutiveWins", 0);
        analysis.put("maxConsecutiveLosses", 0);
        analysis.put("status", "OK");

        // Get daily performance data if accountId provided
        if (accountId != null && !accountId.isEmpty()) {
            List<DailyPerformanceEntity> dailyData = dailyPerformanceRepository
                    .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, effectiveStart, effectiveEnd);

            if (!dailyData.isEmpty()) {
                BigDecimal totalPnl = dailyData.stream()
                        .map(DailyPerformanceEntity::getTotalPnl)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                int totalTrades = dailyData.stream()
                        .mapToInt(DailyPerformanceEntity::getTotalTrades)
                        .sum();
                int winningTrades = dailyData.stream()
                        .mapToInt(DailyPerformanceEntity::getWinningTrades)
                        .sum();

                BigDecimal winRate = totalTrades > 0
                        ? BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                        : BigDecimal.ZERO;

                analysis.put("totalPnl", totalPnl);
                analysis.put("totalTrades", totalTrades);
                analysis.put("winRate", winRate);
                analysis.put("maxDrawdown", calculateMaxDrawdown(dailyData));
            }
        }

        return ResponseEntity.ok(analysis);
    }

    /**
     * Get performance comparison with benchmark.
     */
    @GetMapping("/comparison")
    public ResponseEntity<Map<String, Object>> getPerformanceComparison(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "KOSPI") String benchmark,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Fetching performance comparison for account: {}, benchmark: {}", accountId, benchmark);

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(30);

        Map<String, Object> comparison = new java.util.HashMap<>();
        comparison.put("accountId", accountId);
        comparison.put("benchmark", benchmark);
        comparison.put("fromDate", startDate);
        comparison.put("toDate", endDate);
        comparison.put("portfolioReturn", BigDecimal.ZERO);
        comparison.put("benchmarkReturn", BigDecimal.ZERO);
        comparison.put("alpha", BigDecimal.ZERO);
        comparison.put("beta", BigDecimal.ONE);
        comparison.put("sharpeRatio", BigDecimal.ZERO);
        comparison.put("status", "OK");

        return ResponseEntity.ok(comparison);
    }

    /**
     * Get daily performance trend.
     */
    @GetMapping("/daily-trend")
    public ResponseEntity<Map<String, Object>> getDailyTrend(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Fetching daily trend for account: {}", accountId);

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(30);

        List<Map<String, Object>> dailyData = new ArrayList<>();

        if (accountId != null) {
            List<DailyPerformanceEntity> performances = dailyPerformanceRepository
                    .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, startDate, endDate);

            BigDecimal cumPnl = BigDecimal.ZERO;
            for (DailyPerformanceEntity perf : performances) {
                cumPnl = cumPnl.add(perf.getTotalPnl());
                Map<String, Object> day = new java.util.HashMap<>();
                day.put("date", perf.getTradeDate());
                day.put("pnl", perf.getTotalPnl());
                day.put("cumulativePnl", cumPnl);
                day.put("trades", perf.getTotalTrades());
                dailyData.add(day);
            }
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("accountId", accountId);
        response.put("fromDate", startDate);
        response.put("toDate", endDate);
        response.put("dailyData", dailyData);
        response.put("status", "OK");

        return ResponseEntity.ok(response);
    }

    private PerformanceResponse buildPerformanceResponse(
            String accountId, LocalDate startDate, LocalDate endDate,
            String period, List<DailyPerformanceEntity> dailyData) {

        // 집계 계산
        BigDecimal totalPnl = dailyData.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal realizedPnl = dailyData.stream()
                .map(DailyPerformanceEntity::getRealizedPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTrades = dailyData.stream()
                .mapToInt(DailyPerformanceEntity::getTotalTrades)
                .sum();

        int winningTrades = dailyData.stream()
                .mapToInt(DailyPerformanceEntity::getWinningTrades)
                .sum();

        int losingTrades = dailyData.stream()
                .mapToInt(DailyPerformanceEntity::getLosingTrades)
                .sum();

        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal totalVolume = dailyData.stream()
                .map(DailyPerformanceEntity::getTotalVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = dailyData.stream()
                .map(DailyPerformanceEntity::getTotalFees)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 최대 낙폭 계산
        BigDecimal maxDrawdown = calculateMaxDrawdown(dailyData);

        // 일별 성과 리스트
        List<PerformanceResponse.DailyPerformance> dailyPerformances = new ArrayList<>();
        BigDecimal cumPnl = BigDecimal.ZERO;
        for (DailyPerformanceEntity d : dailyData) {
            cumPnl = cumPnl.add(d.getTotalPnl());
            dailyPerformances.add(PerformanceResponse.DailyPerformance.builder()
                    .date(d.getTradeDate())
                    .pnl(d.getTotalPnl())
                    .cumulativePnl(cumPnl)
                    .trades(d.getTotalTrades())
                    .wins(d.getWinningTrades())
                    .losses(d.getLosingTrades())
                    .build());
        }

        return PerformanceResponse.builder()
                .accountId(accountId)
                .fromDate(startDate)
                .toDate(endDate)
                .period(period)
                .totalPnl(totalPnl)
                .realizedPnl(realizedPnl)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .winRate(winRate)
                .totalVolume(totalVolume)
                .totalFees(totalFees)
                .maxDrawdown(maxDrawdown)
                .dailyPerformances(dailyPerformances)
                .build();
    }

    private PerformanceResponse calculatePerformanceFromFills(
            String accountId, LocalDate startDate, LocalDate endDate, String period) {

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        List<FillEntity> fills = fillRepository.findByAccountIdAndFillTsBetween(accountId, from, to);

        if (fills.isEmpty()) {
            return PerformanceResponse.builder()
                    .accountId(accountId)
                    .fromDate(startDate)
                    .toDate(endDate)
                    .period(period)
                    .totalPnl(BigDecimal.ZERO)
                    .totalTrades(0)
                    .winningTrades(0)
                    .losingTrades(0)
                    .winRate(BigDecimal.ZERO)
                    .dailyPerformances(new ArrayList<>())
                    .build();
        }

        // 체결 데이터로 기본 통계 계산
        BigDecimal totalVolume = fills.stream()
                .map(f -> f.getFillPrice().multiply(f.getFillQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = fills.stream()
                .map(f -> f.getFee().add(f.getTax()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PerformanceResponse.builder()
                .accountId(accountId)
                .fromDate(startDate)
                .toDate(endDate)
                .period(period)
                .totalPnl(BigDecimal.ZERO) // 실제 PnL은 포지션 청산 시 계산
                .totalTrades(fills.size())
                .totalVolume(totalVolume)
                .totalFees(totalFees)
                .dailyPerformances(new ArrayList<>())
                .build();
    }

    private StrategyStatisticsResponse buildStrategyStatistics(
            StrategyEntity strategy, LocalDate startDate, LocalDate endDate) {

        List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                .findByStrategyIdAndTradeDateBetweenOrderByTradeDateAsc(strategy.getStrategyId(), startDate, endDate);

        BigDecimal totalPnl = performanceData.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTrades = performanceData.stream()
                .mapToInt(DailyPerformanceEntity::getTotalTrades)
                .sum();

        int winningTrades = performanceData.stream()
                .mapToInt(DailyPerformanceEntity::getWinningTrades)
                .sum();

        int losingTrades = performanceData.stream()
                .mapToInt(DailyPerformanceEntity::getLosingTrades)
                .sum();

        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(winningTrades).divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // 최근 성과 계산
        LocalDate now = LocalDate.now();
        BigDecimal last7DaysPnl = calculatePeriodPnl(performanceData, now.minusDays(7), now);
        BigDecimal last30DaysPnl = calculatePeriodPnl(performanceData, now.minusDays(30), now);
        BigDecimal last90DaysPnl = calculatePeriodPnl(performanceData, now.minusDays(90), now);

        return StrategyStatisticsResponse.builder()
                .strategyId(strategy.getStrategyId())
                .strategyName(strategy.getName())
                .status(strategy.getStatus())
                .createdAt(strategy.getCreatedAt())
                .totalPnl(totalPnl)
                .totalTrades(totalTrades)
                .winningTrades(winningTrades)
                .losingTrades(losingTrades)
                .winRate(winRate)
                .maxDrawdown(calculateMaxDrawdown(performanceData))
                .last7DaysPnl(last7DaysPnl)
                .last30DaysPnl(last30DaysPnl)
                .last90DaysPnl(last90DaysPnl)
                .build();
    }

    private BigDecimal calculatePeriodPnl(List<DailyPerformanceEntity> data, LocalDate from, LocalDate to) {
        return data.stream()
                .filter(d -> !d.getTradeDate().isBefore(from) && !d.getTradeDate().isAfter(to))
                .map(DailyPerformanceEntity::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateMaxDrawdown(List<DailyPerformanceEntity> dailyData) {
        if (dailyData.isEmpty()) return BigDecimal.ZERO;

        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal cumPnl = BigDecimal.ZERO;

        for (DailyPerformanceEntity d : dailyData) {
            cumPnl = cumPnl.add(d.getTotalPnl());
            if (cumPnl.compareTo(peak) > 0) {
                peak = cumPnl;
            }
            BigDecimal drawdown = peak.subtract(cumPnl);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown;
    }
}
