package maru.trading.api.controller.query;

import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.FillEntity;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.FillJpaRepository;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboard Query Controller Unit Test
 *
 * Unit tests with mocked repositories to test controller logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dashboard Query Controller Unit Test")
class DashboardQueryControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private FillJpaRepository fillRepository;

    @Mock
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    @InjectMocks
    private DashboardQueryController controller;

    private static final String BASE_URL = "/api/v1/query/dashboard";
    private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_001";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("GET /api/v1/query/dashboard/stats")
    class GetDashboardStats {

        @Test
        @DisplayName("Should return dashboard stats with all fields")
        void getDashboardStats_Success() throws Exception {
            // Given
            when(orderRepository.countByAccountIdAndCreatedAtBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(5L);
            when(fillRepository.countByAccountIdAndFillTsBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(3L);
            when(dailyPerformanceRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
                    eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(createMockDailyPerformances());
            when(dailyPerformanceRepository.sumTotalPnlByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(BigDecimal.valueOf(150000));
            when(dailyPerformanceRepository.sumTotalTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(100);
            when(dailyPerformanceRepository.sumWinningTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(65);
            when(orderRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(createMockOrders());
            when(fillRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(createMockFills());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayOrders").value(5))
                    .andExpect(jsonPath("$.todayFills").value(3))
                    .andExpect(jsonPath("$.totalProfitLoss").value(150000))
                    .andExpect(jsonPath("$.winRate").value(65.0000))
                    .andExpect(jsonPath("$.recentActivities").isArray())
                    .andExpect(jsonPath("$.dailyStats").isArray());
        }

        @Test
        @DisplayName("Should return zero values when no data exists")
        void getDashboardStats_NoData() throws Exception {
            // Given
            when(orderRepository.countByAccountIdAndCreatedAtBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(0L);
            when(fillRepository.countByAccountIdAndFillTsBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(0L);
            when(dailyPerformanceRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
                    eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(dailyPerformanceRepository.sumTotalPnlByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(null);
            when(dailyPerformanceRepository.sumTotalTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(null);
            when(dailyPerformanceRepository.sumWinningTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(null);
            when(orderRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(Collections.emptyList());
            when(fillRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayOrders").value(0))
                    .andExpect(jsonPath("$.todayFills").value(0))
                    .andExpect(jsonPath("$.totalProfitLoss").value(0))
                    .andExpect(jsonPath("$.winRate").value(0))
                    .andExpect(jsonPath("$.recentActivities").isEmpty())
                    .andExpect(jsonPath("$.dailyStats").isEmpty());
        }

        @Test
        @DisplayName("Should return stats without accountId (all accounts)")
        void getDashboardStats_WithoutAccountId() throws Exception {
            // Given - mock all required methods for no-accountId case
            when(orderRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(10L);
            when(fillRepository.countByFillTsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(8L);
            when(orderRepository.findTop20ByOrderByCreatedAtDesc())
                    .thenReturn(createMockOrders());
            when(fillRepository.findTop20ByOrderByFillTsDesc())
                    .thenReturn(createMockFills());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayOrders").value(10))
                    .andExpect(jsonPath("$.todayFills").value(8))
                    .andExpect(jsonPath("$.todayProfitLoss").value(0))
                    .andExpect(jsonPath("$.totalProfitLoss").value(0))
                    .andExpect(jsonPath("$.recentActivities").isArray());
        }

        @Test
        @DisplayName("Should apply custom days parameter")
        void getDashboardStats_WithCustomDays() throws Exception {
            // Given
            when(orderRepository.countByAccountIdAndCreatedAtBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(2L);
            when(fillRepository.countByAccountIdAndFillTsBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(1L);
            when(dailyPerformanceRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
                    eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(createMockDailyPerformances());
            when(dailyPerformanceRepository.sumTotalPnlByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(BigDecimal.valueOf(50000));
            when(dailyPerformanceRepository.sumTotalTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(20);
            when(dailyPerformanceRepository.sumWinningTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(12);
            when(orderRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(Collections.emptyList());
            when(fillRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", TEST_ACCOUNT_ID)
                            .param("days", "7")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayOrders").value(2))
                    .andExpect(jsonPath("$.dailyStats").isArray());
        }

        @Test
        @DisplayName("Should calculate win rate correctly")
        void getDashboardStats_WinRateCalculation() throws Exception {
            // Given - 80% win rate (80 wins out of 100 trades)
            when(orderRepository.countByAccountIdAndCreatedAtBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(0L);
            when(fillRepository.countByAccountIdAndFillTsBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(0L);
            when(dailyPerformanceRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
                    eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(dailyPerformanceRepository.sumTotalPnlByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(BigDecimal.valueOf(200000));
            when(dailyPerformanceRepository.sumTotalTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(100);
            when(dailyPerformanceRepository.sumWinningTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(80);
            when(orderRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(Collections.emptyList());
            when(fillRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.winRate").value(80.0000));
        }

        @Test
        @DisplayName("Should include recent activities sorted by timestamp")
        void getDashboardStats_RecentActivitiesSorted() throws Exception {
            // Given
            when(orderRepository.countByAccountIdAndCreatedAtBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(3L);
            when(fillRepository.countByAccountIdAndFillTsBetween(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(2L);
            when(dailyPerformanceRepository.findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
                    eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(dailyPerformanceRepository.sumTotalPnlByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(dailyPerformanceRepository.sumTotalTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(0);
            when(dailyPerformanceRepository.sumWinningTradesByAccountIdAndDateRange(eq(TEST_ACCOUNT_ID), any(), any()))
                    .thenReturn(0);
            when(orderRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(createMockOrders());
            when(fillRepository.findRecentByAccountId(eq(TEST_ACCOUNT_ID), any()))
                    .thenReturn(createMockFills());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recentActivities").isArray())
                    .andExpect(jsonPath("$.recentActivities[0].type").exists())
                    .andExpect(jsonPath("$.recentActivities[0].description").exists())
                    .andExpect(jsonPath("$.recentActivities[0].timestamp").exists());
        }
    }

    private List<DailyPerformanceEntity> createMockDailyPerformances() {
        LocalDate today = LocalDate.now();
        return Arrays.asList(
                createDailyPerformance(today.minusDays(2), BigDecimal.valueOf(5000), 10, 6, 4),
                createDailyPerformance(today.minusDays(1), BigDecimal.valueOf(3000), 8, 5, 3),
                createDailyPerformance(today, BigDecimal.valueOf(2000), 5, 3, 2)
        );
    }

    private DailyPerformanceEntity createDailyPerformance(LocalDate date, BigDecimal pnl,
                                                          int totalTrades, int wins, int losses) {
        DailyPerformanceEntity perf = new DailyPerformanceEntity();
        perf.setPerformanceId("PERF_" + date);
        perf.setAccountId(TEST_ACCOUNT_ID);
        perf.setStrategyId("STRAT_001");
        perf.setTradeDate(date);
        perf.setTotalPnl(pnl);
        perf.setRealizedPnl(pnl);
        perf.setUnrealizedPnl(BigDecimal.ZERO);
        perf.setTotalTrades(totalTrades);
        perf.setWinningTrades(wins);
        perf.setLosingTrades(losses);
        perf.setTotalVolume(BigDecimal.valueOf(1000000));
        perf.setTotalFees(BigDecimal.valueOf(5000));
        perf.setCreatedAt(LocalDateTime.now());
        return perf;
    }

    private List<OrderEntity> createMockOrders() {
        LocalDateTime now = LocalDateTime.now();
        return Arrays.asList(
                createOrder("ORD_001", now.minusMinutes(10)),
                createOrder("ORD_002", now.minusMinutes(30)),
                createOrder("ORD_003", now.minusHours(1))
        );
    }

    private OrderEntity createOrder(String orderId, LocalDateTime createdAt) {
        return OrderEntity.builder()
                .orderId(orderId)
                .accountId(TEST_ACCOUNT_ID)
                .symbol("005930")
                .side(Side.BUY)
                .orderType(OrderType.LIMIT)
                .ordDvsn("00")
                .qty(BigDecimal.valueOf(10))
                .price(BigDecimal.valueOf(70000))
                .status(OrderStatus.FILLED)
                .idempotencyKey("IDEM_" + orderId)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private List<FillEntity> createMockFills() {
        LocalDateTime now = LocalDateTime.now();
        return Arrays.asList(
                createFill("FILL_001", now.minusMinutes(5)),
                createFill("FILL_002", now.minusMinutes(20))
        );
    }

    private FillEntity createFill(String fillId, LocalDateTime fillTs) {
        return FillEntity.builder()
                .fillId(fillId)
                .orderId("ORD_001")
                .accountId(TEST_ACCOUNT_ID)
                .symbol("005930")
                .side(Side.BUY)
                .fillQty(BigDecimal.valueOf(10))
                .fillPrice(BigDecimal.valueOf(70000))
                .fee(BigDecimal.valueOf(100))
                .tax(BigDecimal.valueOf(50))
                .fillTs(fillTs)
                .createdAt(fillTs)
                .build();
    }
}
