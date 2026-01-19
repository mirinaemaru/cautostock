package maru.trading.api.controller.query;

import maru.trading.api.dto.response.DashboardStatsResponse;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.FillEntity;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.FillJpaRepository;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dashboard Statistics Query API.
 *
 * Endpoints:
 * - GET /api/v1/query/dashboard/stats - Get dashboard statistics
 */
@RestController
@RequestMapping("/api/v1/query/dashboard")
public class DashboardQueryController {

    private static final Logger log = LoggerFactory.getLogger(DashboardQueryController.class);

    private final OrderJpaRepository orderRepository;
    private final FillJpaRepository fillRepository;
    private final DailyPerformanceJpaRepository dailyPerformanceRepository;

    public DashboardQueryController(
            OrderJpaRepository orderRepository,
            FillJpaRepository fillRepository,
            DailyPerformanceJpaRepository dailyPerformanceRepository) {
        this.orderRepository = orderRepository;
        this.fillRepository = fillRepository;
        this.dailyPerformanceRepository = dailyPerformanceRepository;
    }

    /**
     * Get dashboard statistics.
     *
     * @param accountId Account ID (optional - if not provided, returns all accounts stats)
     * @param days Number of days for daily stats (default: 30)
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "30") int days) {

        log.info("Fetching dashboard stats for accountId: {}, days: {}", accountId, days);

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDate fromDate = today.minusDays(days);

        // Today's order count
        long todayOrders = accountId != null
                ? orderRepository.countByAccountIdAndCreatedAtBetween(accountId, todayStart, todayEnd)
                : orderRepository.countByCreatedAtBetween(todayStart, todayEnd);

        // Today's fill count
        long todayFills = accountId != null
                ? fillRepository.countByAccountIdAndFillTsBetween(accountId, todayStart, todayEnd)
                : fillRepository.countByFillTsBetween(todayStart, todayEnd);

        // Today's profit/loss
        BigDecimal todayProfitLoss = calculateTodayProfitLoss(accountId, today);

        // Total profit/loss
        BigDecimal totalProfitLoss = calculateTotalProfitLoss(accountId);

        // Win rate
        BigDecimal winRate = calculateWinRate(accountId, fromDate, today);

        // Recent activities
        List<DashboardStatsResponse.RecentActivity> recentActivities = getRecentActivities(accountId, 20);

        // Daily stats
        List<DashboardStatsResponse.DailyStat> dailyStats = getDailyStats(accountId, fromDate, today);

        DashboardStatsResponse response = DashboardStatsResponse.builder()
                .todayOrders(todayOrders)
                .todayFills(todayFills)
                .todayProfitLoss(todayProfitLoss)
                .totalProfitLoss(totalProfitLoss)
                .winRate(winRate)
                .recentActivities(recentActivities)
                .dailyStats(dailyStats)
                .build();

        return ResponseEntity.ok(response);
    }

    private BigDecimal calculateTodayProfitLoss(String accountId, LocalDate today) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }

        List<DailyPerformanceEntity> todayPerf = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, today, today);

        return todayPerf.stream()
                .map(DailyPerformanceEntity::getTotalPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalProfitLoss(String accountId) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }

        LocalDate fromDate = LocalDate.of(2020, 1, 1);
        LocalDate toDate = LocalDate.now();

        BigDecimal totalPnl = dailyPerformanceRepository.sumTotalPnlByAccountIdAndDateRange(accountId, fromDate, toDate);
        return totalPnl != null ? totalPnl : BigDecimal.ZERO;
    }

    private BigDecimal calculateWinRate(String accountId, LocalDate fromDate, LocalDate toDate) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }

        Integer totalTrades = dailyPerformanceRepository.sumTotalTradesByAccountIdAndDateRange(accountId, fromDate, toDate);
        Integer winningTrades = dailyPerformanceRepository.sumWinningTradesByAccountIdAndDateRange(accountId, fromDate, toDate);

        if (totalTrades == null || totalTrades == 0) {
            return BigDecimal.ZERO;
        }

        int wins = winningTrades != null ? winningTrades : 0;
        return BigDecimal.valueOf(wins)
                .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<DashboardStatsResponse.RecentActivity> getRecentActivities(String accountId, int limit) {
        List<DashboardStatsResponse.RecentActivity> activities = new ArrayList<>();

        // Get recent orders
        List<OrderEntity> recentOrders = accountId != null
                ? orderRepository.findRecentByAccountId(accountId, PageRequest.of(0, limit))
                : orderRepository.findTop20ByOrderByCreatedAtDesc();

        for (OrderEntity order : recentOrders) {
            activities.add(DashboardStatsResponse.RecentActivity.builder()
                    .id(order.getOrderId())
                    .type("ORDER")
                    .description(String.format("%s %s %s @ %s",
                            order.getSide(),
                            order.getSymbol(),
                            order.getQty(),
                            order.getPrice()))
                    .timestamp(order.getCreatedAt())
                    .status(order.getStatus().name())
                    .build());
        }

        // Get recent fills
        List<FillEntity> recentFills = accountId != null
                ? fillRepository.findRecentByAccountId(accountId, PageRequest.of(0, limit))
                : fillRepository.findTop20ByOrderByFillTsDesc();

        for (FillEntity fill : recentFills) {
            activities.add(DashboardStatsResponse.RecentActivity.builder()
                    .id(fill.getFillId())
                    .type("FILL")
                    .description(String.format("%s %s @ %s",
                            fill.getSymbol(),
                            fill.getFillQty(),
                            fill.getFillPrice()))
                    .timestamp(fill.getFillTs())
                    .status("FILLED")
                    .build());
        }

        // Sort by timestamp descending and limit
        return activities.stream()
                .sorted(Comparator.comparing(DashboardStatsResponse.RecentActivity::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<DashboardStatsResponse.DailyStat> getDailyStats(String accountId, LocalDate fromDate, LocalDate toDate) {
        List<DashboardStatsResponse.DailyStat> dailyStats = new ArrayList<>();

        if (accountId == null) {
            // Return empty list or aggregate all accounts
            return dailyStats;
        }

        List<DailyPerformanceEntity> performances = dailyPerformanceRepository
                .findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(accountId, fromDate, toDate);

        BigDecimal cumulativePnl = BigDecimal.ZERO;
        for (DailyPerformanceEntity perf : performances) {
            cumulativePnl = cumulativePnl.add(perf.getTotalPnl());

            dailyStats.add(DashboardStatsResponse.DailyStat.builder()
                    .date(perf.getTradeDate())
                    .orderCount(perf.getTotalTrades())
                    .fillCount(perf.getTotalTrades()) // Assuming 1:1 for simplicity
                    .profitLoss(perf.getTotalPnl())
                    .cumulativeProfitLoss(cumulativePnl)
                    .winCount(perf.getWinningTrades())
                    .lossCount(perf.getLosingTrades())
                    .build());
        }

        return dailyStats;
    }
}
