package maru.trading.api.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard statistics response.
 */
@Getter
@Builder
public class DashboardStatsResponse {

    private final long todayOrders;
    private final long todayFills;
    private final BigDecimal todayProfitLoss;
    private final BigDecimal totalProfitLoss;
    private final BigDecimal winRate;
    private final List<RecentActivity> recentActivities;
    private final List<DailyStat> dailyStats;

    /**
     * Recent activity item.
     */
    @Getter
    @Builder
    public static class RecentActivity {
        private final String id;
        private final String type;        // ORDER, FILL, SIGNAL, ALERT
        private final String description;
        private final LocalDateTime timestamp;
        private final String status;
    }

    /**
     * Daily statistics.
     */
    @Getter
    @Builder
    public static class DailyStat {
        private final LocalDate date;
        private final long orderCount;
        private final long fillCount;
        private final BigDecimal profitLoss;
        private final BigDecimal cumulativeProfitLoss;
        private final int winCount;
        private final int lossCount;
    }
}
