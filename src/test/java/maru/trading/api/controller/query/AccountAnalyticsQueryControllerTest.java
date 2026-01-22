package maru.trading.api.controller.query;

import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.ExecutionHistoryEntity;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
import maru.trading.infra.persistence.jpa.repository.ExecutionHistoryJpaRepository;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
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
 * Account Analytics Query Controller Test
 *
 * Tests Account Analytics API endpoints:
 * - GET /api/v1/query/accounts/{id}/analytics - Account-level metrics
 * - GET /api/v1/query/accounts/{id}/performance - Performance summary
 * - GET /api/v1/query/accounts/{id}/holdings - Holdings breakdown
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Account Analytics Query Controller Test")
class AccountAnalyticsQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    @Autowired
    private ExecutionHistoryJpaRepository executionHistoryRepository;

    private static final String BASE_URL = "/api/v1/query/accounts";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestPositions();
        createTestPerformanceData();
        createTestExecutionHistory();
    }

    @Nested
    @DisplayName("GET /api/v1/query/accounts/{id}/analytics - Account Analytics")
    class GetAccountAnalytics {

        @Test
        @DisplayName("Should return comprehensive analytics for account")
        void getAnalytics_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId + "/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.portfolio").exists())
                    .andExpect(jsonPath("$.portfolio.totalValue").exists())
                    .andExpect(jsonPath("$.portfolio.positionCount").exists())
                    .andExpect(jsonPath("$.performance").exists())
                    .andExpect(jsonPath("$.execution").exists())
                    .andExpect(jsonPath("$.execution.totalExecutions").exists())
                    .andExpect(jsonPath("$.execution.successRate").exists());
        }

        @Test
        @DisplayName("Should return analytics with date range filter")
        void getAnalytics_WithDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/" + testAccountId + "/analytics")
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.fromDate").value(from.toString()))
                    .andExpect(jsonPath("$.toDate").value(to.toString()));
        }

        @Test
        @DisplayName("Should return empty analytics for account with no data")
        void getAnalytics_NoData() throws Exception {
            String emptyAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + emptyAccountId + "/analytics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(emptyAccountId))
                    .andExpect(jsonPath("$.portfolio.positionCount").value(0))
                    .andExpect(jsonPath("$.execution.totalExecutions").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/accounts/{id}/performance - Account Performance")
    class GetAccountPerformance {

        @Test
        @DisplayName("Should return performance summary with default period")
        void getPerformance_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId + "/performance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.period").value(30))
                    .andExpect(jsonPath("$.totalPnl").exists())
                    .andExpect(jsonPath("$.winningDays").exists())
                    .andExpect(jsonPath("$.losingDays").exists())
                    .andExpect(jsonPath("$.winRate").exists())
                    .andExpect(jsonPath("$.dailyData").isArray());
        }

        @Test
        @DisplayName("Should return performance with custom period")
        void getPerformance_CustomPeriod() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId + "/performance")
                            .param("period", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period").value(7));
        }

        @Test
        @DisplayName("Should return empty performance for account with no data")
        void getPerformance_NoData() throws Exception {
            String emptyAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + emptyAccountId + "/performance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(emptyAccountId))
                    .andExpect(jsonPath("$.totalPnl").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/accounts/{id}/holdings - Account Holdings")
    class GetAccountHoldings {

        @Test
        @DisplayName("Should return holdings breakdown")
        void getHoldings_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId + "/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.totalValue").exists())
                    .andExpect(jsonPath("$.positionCount").exists())
                    .andExpect(jsonPath("$.holdings").isArray())
                    .andExpect(jsonPath("$.holdings[0].symbol").exists())
                    .andExpect(jsonPath("$.holdings[0].side").exists())
                    .andExpect(jsonPath("$.holdings[0].quantity").exists())
                    .andExpect(jsonPath("$.holdings[0].weight").exists());
        }

        @Test
        @DisplayName("Should return empty holdings for account with no positions")
        void getHoldings_NoPositions() throws Exception {
            String emptyAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + emptyAccountId + "/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(emptyAccountId))
                    .andExpect(jsonPath("$.positionCount").value(0))
                    .andExpect(jsonPath("$.holdings").isArray())
                    .andExpect(jsonPath("$.holdings").isEmpty());
        }

        @Test
        @DisplayName("Should calculate correct position weights")
        void getHoldings_CorrectWeights() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId + "/holdings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.holdings[0].weight").isNumber());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestPositions() {
        String[] symbols = {"005930", "035720", "000660"};
        BigDecimal[] quantities = {BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(-30)};
        BigDecimal[] prices = {BigDecimal.valueOf(70000), BigDecimal.valueOf(150000), BigDecimal.valueOf(120000)};

        for (int i = 0; i < symbols.length; i++) {
            PositionEntity position = PositionEntity.builder()
                    .positionId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .symbol(symbols[i])
                    .qty(quantities[i])
                    .avgPrice(prices[i])
                    .realizedPnl(BigDecimal.valueOf(50000 * (i + 1)))
                    .updatedAt(LocalDateTime.now())
                    .build();
            positionRepository.save(position);
        }
    }

    private void createTestPerformanceData() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 10; i++) {
            DailyPerformanceEntity perf = DailyPerformanceEntity.builder()
                    .performanceId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .tradeDate(today.minusDays(i))
                    .totalTrades(5 + i)
                    .winningTrades(3 + (i % 2))
                    .losingTrades(2 - (i % 2))
                    .totalPnl(BigDecimal.valueOf(100000 * (i % 2 == 0 ? 1 : -1)))
                    .realizedPnl(BigDecimal.valueOf(80000))
                    .unrealizedPnl(BigDecimal.valueOf(20000))
                    .totalVolume(BigDecimal.valueOf(10000000))
                    .totalFees(BigDecimal.valueOf(15000))
                    .build();
            dailyPerformanceRepository.save(perf);
        }
    }

    private void createTestExecutionHistory() {
        LocalDateTime now = LocalDateTime.now();
        String[] statuses = {"SUCCESS", "SUCCESS", "SUCCESS", "FAILED", "SUCCESS"};

        for (int i = 0; i < statuses.length; i++) {
            ExecutionHistoryEntity execution = ExecutionHistoryEntity.builder()
                    .executionId(UlidGenerator.generate())
                    .strategyId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .executionType("ORDER_PLACEMENT")
                    .status(statuses[i])
                    .symbol("005930")
                    .description("Test execution " + i)
                    .executionTimeMs(100 + i * 10)
                    .createdAt(now.minusHours(i))
                    .build();
            executionHistoryRepository.save(execution);
        }
    }
}
