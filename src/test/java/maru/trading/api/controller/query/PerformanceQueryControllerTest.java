package maru.trading.api.controller.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance Query Controller Test
 *
 * Tests Performance Analysis API endpoints:
 * - GET /api/v1/query/performance - Get account performance
 * - GET /api/v1/query/performance/strategies - Get all strategies statistics
 * - GET /api/v1/query/performance/strategies/{id} - Get specific strategy statistics
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Performance Query Controller Test")
class PerformanceQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    private static final String BASE_URL = "/api/v1/query/performance";
    private String testAccountId;
    private String testStrategyId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        testStrategyId = createTestStrategy("PERF_TEST_" + System.currentTimeMillis());
        createTestPerformanceData();
    }

    @Nested
    @DisplayName("GET /api/v1/query/performance - Get Account Performance")
    class GetPerformance {

        @Test
        @DisplayName("Should return performance data for account")
        void getPerformance_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.totalPnl").exists())
                    .andExpect(jsonPath("$.totalTrades").exists())
                    .andExpect(jsonPath("$.winRate").exists());
        }

        @Test
        @DisplayName("Should return performance with date range filter")
        void getPerformance_WithDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId)
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.fromDate").value(from.toString()))
                    .andExpect(jsonPath("$.toDate").value(to.toString()));
        }

        @Test
        @DisplayName("Should return empty performance for account with no data")
        void getPerformance_NoData() throws Exception {
            String emptyAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", emptyAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(emptyAccountId))
                    .andExpect(jsonPath("$.totalTrades").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/performance/strategies - Get All Strategies Statistics")
    class GetAllStrategiesStatistics {

        @Test
        @DisplayName("Should return all strategies statistics")
        void getAllStrategiesStats_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/strategies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategies").isArray())
                    .andExpect(jsonPath("$.count").exists());
        }

        @Test
        @DisplayName("Should return statistics with date range")
        void getAllStrategiesStats_WithDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/strategies")
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromDate").value(from.toString()))
                    .andExpect(jsonPath("$.toDate").value(to.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/performance/strategies/{id} - Get Strategy Statistics")
    class GetStrategyStatistics {

        @Test
        @DisplayName("Should return strategy statistics")
        void getStrategyStats_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/strategies/" + testStrategyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategyId").value(testStrategyId))
                    .andExpect(jsonPath("$.strategyName").exists())
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent strategy")
        void getStrategyStats_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/strategies/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Helper Methods ====================

    private String createTestStrategy(String name) {
        String strategyId = UlidGenerator.generate();
        String versionId = UlidGenerator.generate();

        StrategyEntity strategy = StrategyEntity.builder()
                .strategyId(strategyId)
                .name(name)
                .description("Test strategy")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(versionId)
                .build();
        strategyRepository.save(strategy);

        return strategyId;
    }

    private void createTestPerformanceData() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            DailyPerformanceEntity perf = DailyPerformanceEntity.builder()
                    .performanceId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .strategyId(testStrategyId)
                    .tradeDate(today.minusDays(i))
                    .totalTrades(10)
                    .winningTrades(6)
                    .losingTrades(4)
                    .totalPnl(BigDecimal.valueOf(100000 * (i % 2 == 0 ? 1 : -1)))
                    .realizedPnl(BigDecimal.valueOf(80000))
                    .unrealizedPnl(BigDecimal.valueOf(20000))
                    .totalVolume(BigDecimal.valueOf(10000000))
                    .totalFees(BigDecimal.valueOf(15000))
                    .build();
            dailyPerformanceRepository.save(perf);
        }
    }
}
