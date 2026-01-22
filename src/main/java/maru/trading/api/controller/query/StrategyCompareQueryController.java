package maru.trading.api.controller.query;

import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
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
 * Strategy Comparison Query API.
 *
 * Provides endpoints for comparing multiple strategies' performance.
 *
 * Endpoints:
 * - GET /api/v1/query/strategies/compare - Compare multiple strategies
 * - GET /api/v1/query/strategies/ranking - Strategy performance ranking
 */
@RestController
@RequestMapping("/api/v1/query/strategies")
public class StrategyCompareQueryController {

    private static final Logger log = LoggerFactory.getLogger(StrategyCompareQueryController.class);

    private final StrategyJpaRepository strategyRepository;
    private final DailyPerformanceJpaRepository dailyPerformanceRepository;

    public StrategyCompareQueryController(
            StrategyJpaRepository strategyRepository,
            DailyPerformanceJpaRepository dailyPerformanceRepository) {
        this.strategyRepository = strategyRepository;
        this.dailyPerformanceRepository = dailyPerformanceRepository;
    }

    /**
     * Compare multiple strategies.
     *
     * @param strategyIds Comma-separated strategy IDs to compare
     * @param from Start date
     * @param to End date
     */
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> compareStrategies(
            @RequestParam String strategyIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Comparing strategies: {}", strategyIds);

        List<String> ids = Arrays.stream(strategyIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(90);

        Map<String, Object> response = new HashMap<>();
        response.put("fromDate", startDate);
        response.put("toDate", endDate);
        response.put("comparedAt", LocalDate.now());

        List<Map<String, Object>> strategyComparisons = new ArrayList<>();

        for (String strategyId : ids) {
            Optional<StrategyEntity> strategyOpt = strategyRepository.findById(strategyId);
            if (strategyOpt.isEmpty()) {
                continue;
            }

            StrategyEntity strategy = strategyOpt.get();
            List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                    .findByStrategyIdAndTradeDateBetweenOrderByTradeDateAsc(strategyId, startDate, endDate);

            Map<String, Object> comparison = buildStrategyComparison(strategy, performanceData);
            strategyComparisons.add(comparison);
        }

        // Sort by total return descending
        strategyComparisons.sort((a, b) -> {
            BigDecimal returnA = (BigDecimal) a.get("totalReturn");
            BigDecimal returnB = (BigDecimal) b.get("totalReturn");
            return returnB.compareTo(returnA);
        });

        // Add rankings
        for (int i = 0; i < strategyComparisons.size(); i++) {
            strategyComparisons.get(i).put("rank", i + 1);
        }

        response.put("strategies", strategyComparisons);
        response.put("totalStrategies", strategyComparisons.size());

        // Summary comparison
        if (!strategyComparisons.isEmpty()) {
            Map<String, Object> summary = buildComparisonSummary(strategyComparisons);
            response.put("summary", summary);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get strategy performance ranking.
     *
     * @param period Period in days (default: 30)
     * @param sortBy Sort metric: totalReturn, winRate, sharpeRatio (default: totalReturn)
     * @param limit Max strategies to return (default: 10)
     */
    @GetMapping("/ranking")
    public ResponseEntity<Map<String, Object>> getStrategyRanking(
            @RequestParam(defaultValue = "30") Integer period,
            @RequestParam(defaultValue = "totalReturn") String sortBy,
            @RequestParam(defaultValue = "10") Integer limit) {

        log.info("Getting strategy ranking: period={}, sortBy={}", period, sortBy);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period);

        // Get all active strategies
        List<StrategyEntity> strategies = strategyRepository.findByStatusAndDelyn("ACTIVE", "N");

        List<Map<String, Object>> rankings = new ArrayList<>();

        for (StrategyEntity strategy : strategies) {
            List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                    .findByStrategyIdAndTradeDateBetweenOrderByTradeDateAsc(
                            strategy.getStrategyId(), startDate, endDate);

            if (performanceData.isEmpty()) {
                continue;
            }

            Map<String, Object> ranking = buildStrategyComparison(strategy, performanceData);
            rankings.add(ranking);
        }

        // Sort by specified metric
        Comparator<Map<String, Object>> comparator = switch (sortBy.toLowerCase()) {
            case "winrate" -> (a, b) -> ((BigDecimal) b.get("winRate")).compareTo((BigDecimal) a.get("winRate"));
            case "sharperatio" -> (a, b) -> ((BigDecimal) b.get("sharpeRatio")).compareTo((BigDecimal) a.get("sharpeRatio"));
            default -> (a, b) -> ((BigDecimal) b.get("totalReturn")).compareTo((BigDecimal) a.get("totalReturn"));
        };
        rankings.sort(comparator);

        // Add ranks and limit
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).put("rank", i + 1);
        }

        List<Map<String, Object>> topRankings = rankings.stream()
                .limit(limit)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("period", period);
        response.put("fromDate", startDate);
        response.put("toDate", endDate);
        response.put("sortBy", sortBy);
        response.put("totalStrategies", strategies.size());
        response.put("rankings", topRankings);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildStrategyComparison(
            StrategyEntity strategy, List<DailyPerformanceEntity> data) {

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("strategyId", strategy.getStrategyId());
        comparison.put("strategyName", strategy.getName());
        comparison.put("status", strategy.getStatus());
        comparison.put("mode", strategy.getMode());

        if (data.isEmpty()) {
            comparison.put("totalPnl", BigDecimal.ZERO);
            comparison.put("totalReturn", BigDecimal.ZERO);
            comparison.put("winRate", BigDecimal.ZERO);
            comparison.put("sharpeRatio", BigDecimal.ZERO);
            comparison.put("maxDrawdown", BigDecimal.ZERO);
            comparison.put("tradingDays", 0);
            return comparison;
        }

        // Calculate metrics
        BigDecimal totalPnl = data.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long winDays = data.stream()
                .filter(d -> d.getTotalPnl() != null && d.getTotalPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
        long lossDays = data.stream()
                .filter(d -> d.getTotalPnl() != null && d.getTotalPnl().compareTo(BigDecimal.ZERO) < 0)
                .count();

        BigDecimal winRate = winDays + lossDays > 0
                ? BigDecimal.valueOf(winDays * 100.0 / (winDays + lossDays))
                : BigDecimal.ZERO;

        // Calculate total return using cumulative P&L
        BigDecimal baseEquity = new BigDecimal("10000000"); // Base equity for return calculation
        BigDecimal totalReturn = totalPnl.compareTo(BigDecimal.ZERO) != 0
                ? totalPnl.divide(baseEquity, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate daily returns for Sharpe Ratio using cumulative P&L
        List<BigDecimal> dailyReturns = new ArrayList<>();
        BigDecimal runningPnl = BigDecimal.ZERO;
        for (int i = 0; i < data.size(); i++) {
            BigDecimal dayPnl = data.get(i).getTotalPnl() != null ? data.get(i).getTotalPnl() : BigDecimal.ZERO;
            if (i > 0) {
                BigDecimal prevEquity = baseEquity.add(runningPnl);
                runningPnl = runningPnl.add(dayPnl);
                BigDecimal currEquity = baseEquity.add(runningPnl);
                if (prevEquity.compareTo(BigDecimal.ZERO) > 0) {
                    dailyReturns.add(currEquity.subtract(prevEquity).divide(prevEquity, 8, RoundingMode.HALF_UP));
                }
            } else {
                runningPnl = runningPnl.add(dayPnl);
            }
        }

        BigDecimal sharpeRatio = calculateSharpeRatio(dailyReturns);
        BigDecimal maxDrawdown = calculateMaxDrawdown(data);

        comparison.put("totalPnl", totalPnl.setScale(0, RoundingMode.HALF_UP));
        comparison.put("totalReturn", totalReturn.setScale(2, RoundingMode.HALF_UP));
        comparison.put("winRate", winRate.setScale(2, RoundingMode.HALF_UP));
        comparison.put("sharpeRatio", sharpeRatio.setScale(2, RoundingMode.HALF_UP));
        comparison.put("maxDrawdown", maxDrawdown.setScale(2, RoundingMode.HALF_UP));
        comparison.put("tradingDays", data.size());
        comparison.put("winDays", winDays);
        comparison.put("lossDays", lossDays);

        return comparison;
    }

    private BigDecimal calculateSharpeRatio(List<BigDecimal> returns) {
        if (returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, RoundingMode.HALF_UP);

        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size() - 1), 8, RoundingMode.HALF_UP);

        double stdDev = Math.sqrt(variance.doubleValue());
        if (stdDev == 0) {
            return BigDecimal.ZERO;
        }

        // Annualized Sharpe (assuming 3.5% risk-free rate)
        double annualizedReturn = mean.doubleValue() * 252;
        double annualizedStdDev = stdDev * Math.sqrt(252);
        double riskFreeRate = 0.035;

        return BigDecimal.valueOf((annualizedReturn - riskFreeRate) / annualizedStdDev);
    }

    private BigDecimal calculateMaxDrawdown(List<DailyPerformanceEntity> data) {
        BigDecimal baseEquity = new BigDecimal("10000000");
        BigDecimal runningPnl = BigDecimal.ZERO;
        BigDecimal peak = baseEquity;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (DailyPerformanceEntity perf : data) {
            BigDecimal dayPnl = perf.getTotalPnl() != null ? perf.getTotalPnl() : BigDecimal.ZERO;
            runningPnl = runningPnl.add(dayPnl);
            BigDecimal equity = baseEquity.add(runningPnl);

            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }

            if (peak.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = peak.subtract(equity)
                        .divide(peak, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }

        return maxDrawdown;
    }

    private Map<String, Object> buildComparisonSummary(List<Map<String, Object>> comparisons) {
        Map<String, Object> summary = new HashMap<>();

        // Best and worst performers
        summary.put("bestPerformer", comparisons.get(0).get("strategyName"));
        summary.put("bestReturn", comparisons.get(0).get("totalReturn"));
        summary.put("worstPerformer", comparisons.get(comparisons.size() - 1).get("strategyName"));
        summary.put("worstReturn", comparisons.get(comparisons.size() - 1).get("totalReturn"));

        // Average metrics
        BigDecimal avgReturn = comparisons.stream()
                .map(c -> (BigDecimal) c.get("totalReturn"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(comparisons.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgWinRate = comparisons.stream()
                .map(c -> (BigDecimal) c.get("winRate"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(comparisons.size()), 2, RoundingMode.HALF_UP);

        summary.put("averageReturn", avgReturn);
        summary.put("averageWinRate", avgWinRate);

        return summary;
    }
}
