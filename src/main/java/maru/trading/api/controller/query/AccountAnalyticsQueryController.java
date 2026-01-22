package maru.trading.api.controller.query;

import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.ExecutionHistoryJpaRepository;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Account Analytics Query API.
 *
 * Provides comprehensive account-level metrics and analytics.
 *
 * Endpoints:
 * - GET /api/v1/query/accounts/{id}/analytics - Account-level metrics
 * - GET /api/v1/query/accounts/{id}/performance - Performance summary
 * - GET /api/v1/query/accounts/{id}/holdings - Holdings breakdown
 */
@RestController
@RequestMapping("/api/v1/query/accounts")
public class AccountAnalyticsQueryController {

    private static final Logger log = LoggerFactory.getLogger(AccountAnalyticsQueryController.class);

    private final PositionJpaRepository positionRepository;
    private final DailyPerformanceJpaRepository dailyPerformanceRepository;
    private final ExecutionHistoryJpaRepository executionHistoryRepository;

    public AccountAnalyticsQueryController(
            PositionJpaRepository positionRepository,
            DailyPerformanceJpaRepository dailyPerformanceRepository,
            ExecutionHistoryJpaRepository executionHistoryRepository) {
        this.positionRepository = positionRepository;
        this.dailyPerformanceRepository = dailyPerformanceRepository;
        this.executionHistoryRepository = executionHistoryRepository;
    }

    /**
     * Get comprehensive account analytics.
     *
     * @param accountId Account ID
     * @param from Start date (default: 30 days ago)
     * @param to End date (default: today)
     */
    @GetMapping("/{accountId}/analytics")
    public ResponseEntity<Map<String, Object>> getAccountAnalytics(
            @PathVariable String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        log.info("Getting analytics for account: {}", accountId);

        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDate startDate = from != null ? from : endDate.minusDays(30);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("fromDate", startDate);
        response.put("toDate", endDate);
        response.put("generatedAt", LocalDateTime.now());

        // Portfolio metrics
        List<PositionEntity> positions = positionRepository.findByAccountId(accountId);
        Map<String, Object> portfolioMetrics = calculatePortfolioMetrics(positions);
        response.put("portfolio", portfolioMetrics);

        // Performance metrics
        List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, startDate, endDate);
        Map<String, Object> performanceMetrics = calculatePerformanceMetrics(performanceData);
        response.put("performance", performanceMetrics);

        // Execution metrics
        long successCount = executionHistoryRepository.countByAccountIdAndStatusAndCreatedAtBetween(
                accountId, "SUCCESS", startDateTime, endDateTime);
        long failedCount = executionHistoryRepository.countByAccountIdAndStatusAndCreatedAtBetween(
                accountId, "FAILED", startDateTime, endDateTime);
        Map<String, Object> executionMetrics = new HashMap<>();
        executionMetrics.put("totalExecutions", successCount + failedCount);
        executionMetrics.put("successCount", successCount);
        executionMetrics.put("failedCount", failedCount);
        executionMetrics.put("successRate", successCount + failedCount > 0
                ? BigDecimal.valueOf(successCount * 100.0 / (successCount + failedCount))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        response.put("execution", executionMetrics);

        return ResponseEntity.ok(response);
    }

    /**
     * Get account performance summary.
     */
    @GetMapping("/{accountId}/performance")
    public ResponseEntity<Map<String, Object>> getAccountPerformance(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "30") Integer period) {

        log.info("Getting performance for account: {}, period: {} days", accountId, period);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(period);

        List<DailyPerformanceEntity> performanceData = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("period", period);
        response.put("fromDate", startDate);
        response.put("toDate", endDate);
        response.put("dataPoints", performanceData.size());

        Map<String, Object> metrics = calculatePerformanceMetrics(performanceData);
        response.putAll(metrics);

        // Daily returns for charting (calculate cumulative equity from P&L)
        BigDecimal baseEquity = new BigDecimal("10000000");
        final BigDecimal[] runningPnl = {BigDecimal.ZERO};
        List<Map<String, Object>> dailyData = performanceData.stream()
                .map(p -> {
                    runningPnl[0] = runningPnl[0].add(p.getTotalPnl() != null ? p.getTotalPnl() : BigDecimal.ZERO);
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", p.getTradeDate());
                    day.put("pnl", p.getTotalPnl());
                    day.put("equity", baseEquity.add(runningPnl[0]));
                    return day;
                })
                .collect(Collectors.toList());
        response.put("dailyData", dailyData);

        return ResponseEntity.ok(response);
    }

    /**
     * Get account holdings breakdown.
     */
    @GetMapping("/{accountId}/holdings")
    public ResponseEntity<Map<String, Object>> getAccountHoldings(
            @PathVariable String accountId) {

        log.info("Getting holdings for account: {}", accountId);

        List<PositionEntity> positions = positionRepository.findByAccountId(accountId);

        BigDecimal totalValue = positions.stream()
                .map(p -> p.getAvgPrice().multiply(p.getQty()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> holdings = positions.stream()
                .map(p -> {
                    BigDecimal posValue = p.getAvgPrice().multiply(p.getQty().abs());
                    String side = p.getQty().compareTo(BigDecimal.ZERO) >= 0 ? "LONG" : "SHORT";
                    Map<String, Object> holding = new HashMap<>();
                    holding.put("symbol", p.getSymbol());
                    holding.put("side", side);
                    holding.put("quantity", p.getQty());
                    holding.put("avgPrice", p.getAvgPrice());
                    holding.put("currentValue", posValue);
                    holding.put("weight", totalValue.compareTo(BigDecimal.ZERO) > 0
                            ? posValue.divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                            : BigDecimal.ZERO);
                    holding.put("realizedPnl", p.getRealizedPnl());
                    return holding;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("currentValue")).compareTo((BigDecimal) a.get("currentValue")))
                .collect(Collectors.toList());

        // Sector/Industry breakdown would require additional data
        Map<String, Object> response = new HashMap<>();
        response.put("accountId", accountId);
        response.put("totalValue", totalValue);
        response.put("positionCount", positions.size());
        response.put("holdings", holdings);

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> calculatePortfolioMetrics(List<PositionEntity> positions) {
        Map<String, Object> metrics = new HashMap<>();

        BigDecimal totalValue = positions.stream()
                .map(p -> p.getAvgPrice().multiply(p.getQty().abs()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealizedPnl = positions.stream()
                .map(p -> p.getRealizedPnl() != null ? p.getRealizedPnl() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Determine side based on quantity sign (positive = LONG, negative = SHORT)
        long longCount = positions.stream().filter(p -> p.getQty().compareTo(BigDecimal.ZERO) > 0).count();
        long shortCount = positions.stream().filter(p -> p.getQty().compareTo(BigDecimal.ZERO) < 0).count();

        metrics.put("totalValue", totalValue.setScale(0, RoundingMode.HALF_UP));
        metrics.put("positionCount", positions.size());
        metrics.put("longPositions", longCount);
        metrics.put("shortPositions", shortCount);
        metrics.put("realizedPnl", totalRealizedPnl.setScale(0, RoundingMode.HALF_UP));
        metrics.put("realizedPnlPct", totalValue.compareTo(BigDecimal.ZERO) > 0
                ? totalRealizedPnl.divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        return metrics;
    }

    private Map<String, Object> calculatePerformanceMetrics(List<DailyPerformanceEntity> data) {
        Map<String, Object> metrics = new HashMap<>();

        if (data.isEmpty()) {
            metrics.put("totalPnl", BigDecimal.ZERO);
            metrics.put("totalReturn", BigDecimal.ZERO);
            metrics.put("winningDays", 0);
            metrics.put("losingDays", 0);
            metrics.put("winRate", BigDecimal.ZERO);
            return metrics;
        }

        BigDecimal totalPnl = data.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long winningDays = data.stream()
                .filter(d -> d.getTotalPnl() != null && d.getTotalPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();
        long losingDays = data.stream()
                .filter(d -> d.getTotalPnl() != null && d.getTotalPnl().compareTo(BigDecimal.ZERO) < 0)
                .count();

        BigDecimal avgDailyPnl = totalPnl.divide(BigDecimal.valueOf(data.size()), 2, RoundingMode.HALF_UP);

        BigDecimal bestDay = data.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal worstDay = data.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        metrics.put("totalPnl", totalPnl.setScale(0, RoundingMode.HALF_UP));
        metrics.put("avgDailyPnl", avgDailyPnl.setScale(0, RoundingMode.HALF_UP));
        metrics.put("winningDays", winningDays);
        metrics.put("losingDays", losingDays);
        metrics.put("winRate", winningDays + losingDays > 0
                ? BigDecimal.valueOf(winningDays * 100.0 / (winningDays + losingDays))
                        .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        metrics.put("bestDay", bestDay.setScale(0, RoundingMode.HALF_UP));
        metrics.put("worstDay", worstDay.setScale(0, RoundingMode.HALF_UP));

        return metrics;
    }
}
