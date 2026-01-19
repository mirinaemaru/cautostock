package maru.trading.api.controller.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.FillEntity;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.FillJpaRepository;
import maru.trading.infra.persistence.jpa.repository.OrderJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dashboard Query Controller Test
 *
 * Tests Dashboard Statistics API endpoints:
 * - GET /api/v1/query/dashboard/stats - Get dashboard statistics
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Dashboard Query Controller Test")
class DashboardQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private FillJpaRepository fillRepository;

    @Autowired
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    private static final String BASE_URL = "/api/v1/query/dashboard";
    private String testAccountId;
    private String testStrategyId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        testStrategyId = createTestStrategy("DASH_TEST_" + System.currentTimeMillis());
        createTestData();
    }

    @Nested
    @DisplayName("GET /api/v1/query/dashboard/stats - Get Dashboard Stats")
    class GetDashboardStats {

        @Test
        @DisplayName("Should return dashboard stats with accountId")
        void getDashboardStats_WithAccountId_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayOrders").exists())
                    .andExpect(jsonPath("$.todayFills").exists())
                    .andExpect(jsonPath("$.todayProfitLoss").exists())
                    .andExpect(jsonPath("$.totalProfitLoss").exists())
                    .andExpect(jsonPath("$.winRate").exists())
                    .andExpect(jsonPath("$.recentActivities").isArray())
                    .andExpect(jsonPath("$.dailyStats").isArray());
        }

        @Test
        @DisplayName("Should return dashboard stats without accountId")
        void getDashboardStats_WithoutAccountId_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayOrders").exists())
                    .andExpect(jsonPath("$.todayFills").exists())
                    .andExpect(jsonPath("$.recentActivities").isArray());
        }

        @Test
        @DisplayName("Should return dashboard stats with custom days parameter")
        void getDashboardStats_WithDaysParam_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", testAccountId)
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyStats").isArray());
        }

        @Test
        @DisplayName("Should include recent activities")
        void getDashboardStats_ShouldIncludeRecentActivities() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recentActivities").isArray())
                    .andExpect(jsonPath("$.recentActivities[0].id").exists())
                    .andExpect(jsonPath("$.recentActivities[0].type").exists())
                    .andExpect(jsonPath("$.recentActivities[0].description").exists())
                    .andExpect(jsonPath("$.recentActivities[0].timestamp").exists())
                    .andExpect(jsonPath("$.recentActivities[0].status").exists());
        }

        @Test
        @DisplayName("Should include daily stats with cumulative profit/loss")
        void getDashboardStats_ShouldIncludeDailyStats() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyStats").isArray());
        }
    }

    private String createTestStrategy(String name) {
        StrategyEntity strategy = StrategyEntity.builder()
                .strategyId(UlidGenerator.generate())
                .name(name)
                .description("Test strategy for dashboard")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(UlidGenerator.generate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return strategyRepository.save(strategy).getStrategyId();
    }

    private void createTestData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // Create test orders
        for (int i = 0; i < 5; i++) {
            OrderEntity order = OrderEntity.builder()
                    .orderId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .symbol("005930")
                    .side(i % 2 == 0 ? Side.BUY : Side.SELL)
                    .orderType(OrderType.LIMIT)
                    .ordDvsn("00")
                    .qty(BigDecimal.valueOf(10))
                    .price(BigDecimal.valueOf(70000 + i * 100))
                    .status(OrderStatus.FILLED)
                    .idempotencyKey(UlidGenerator.generate())
                    .createdAt(now.minusHours(i))
                    .updatedAt(now.minusHours(i))
                    .build();
            orderRepository.save(order);
        }

        // Create test fills
        for (int i = 0; i < 3; i++) {
            FillEntity fill = FillEntity.builder()
                    .fillId(UlidGenerator.generate())
                    .orderId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .symbol("005930")
                    .side(Side.BUY)
                    .fillQty(BigDecimal.valueOf(10))
                    .fillPrice(BigDecimal.valueOf(70000 + i * 50))
                    .fee(BigDecimal.valueOf(100))
                    .tax(BigDecimal.valueOf(50))
                    .fillTs(now.minusHours(i))
                    .createdAt(now.minusHours(i))
                    .build();
            fillRepository.save(fill);
        }

        // Create daily performance data
        for (int i = 0; i < 7; i++) {
            DailyPerformanceEntity perf = new DailyPerformanceEntity();
            perf.setPerformanceId(UlidGenerator.generate());
            perf.setAccountId(testAccountId);
            perf.setStrategyId(testStrategyId);
            perf.setTradeDate(today.minusDays(i));
            perf.setTotalPnl(BigDecimal.valueOf(10000 - i * 1000));
            perf.setRealizedPnl(BigDecimal.valueOf(10000 - i * 1000));
            perf.setUnrealizedPnl(BigDecimal.ZERO);
            perf.setTotalTrades(10 - i);
            perf.setWinningTrades(6 - i / 2);
            perf.setLosingTrades(4 - i / 3);
            perf.setTotalVolume(BigDecimal.valueOf(1000000));
            perf.setTotalFees(BigDecimal.valueOf(5000));
            perf.setCreatedAt(LocalDateTime.now());
            dailyPerformanceRepository.save(perf);
        }
    }
}
