package maru.trading.api.controller.query;

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
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Strategy Compare Query Controller Test
 *
 * Tests Strategy Comparison API endpoints:
 * - GET /api/v1/query/strategies/compare - Compare multiple strategies
 * - GET /api/v1/query/strategies/ranking - Strategy performance ranking
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Strategy Compare Query Controller Test")
class StrategyCompareQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    @Autowired
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    private static final String BASE_URL = "/api/v1/query/strategies";
    private List<String> testStrategyIds;

    @BeforeEach
    void setUp() {
        testStrategyIds = new ArrayList<>();
        createTestStrategies();
    }

    @Nested
    @DisplayName("GET /api/v1/query/strategies/compare - Compare Strategies")
    class CompareStrategies {

        @Test
        @DisplayName("Should compare multiple strategies successfully")
        void compareStrategies_Success() throws Exception {
            String strategyIds = String.join(",", testStrategyIds);

            mockMvc.perform(get(BASE_URL + "/compare")
                            .param("strategyIds", strategyIds))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategies").isArray())
                    .andExpect(jsonPath("$.totalStrategies").value(testStrategyIds.size()))
                    .andExpect(jsonPath("$.strategies[0].strategyId").exists())
                    .andExpect(jsonPath("$.strategies[0].totalPnl").exists())
                    .andExpect(jsonPath("$.strategies[0].totalReturn").exists())
                    .andExpect(jsonPath("$.strategies[0].winRate").exists())
                    .andExpect(jsonPath("$.strategies[0].sharpeRatio").exists())
                    .andExpect(jsonPath("$.strategies[0].maxDrawdown").exists())
                    .andExpect(jsonPath("$.strategies[0].rank").exists())
                    .andExpect(jsonPath("$.summary").exists())
                    .andExpect(jsonPath("$.summary.bestPerformer").exists())
                    .andExpect(jsonPath("$.summary.worstPerformer").exists());
        }

        @Test
        @DisplayName("Should compare with date range filter")
        void compareStrategies_WithDateRange() throws Exception {
            String strategyIds = String.join(",", testStrategyIds);
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/compare")
                            .param("strategyIds", strategyIds)
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromDate").value(from.toString()))
                    .andExpect(jsonPath("$.toDate").value(to.toString()));
        }

        @Test
        @DisplayName("Should return bad request for empty strategy IDs")
        void compareStrategies_EmptyIds() throws Exception {
            mockMvc.perform(get(BASE_URL + "/compare")
                            .param("strategyIds", ""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle single strategy comparison")
        void compareStrategies_SingleStrategy() throws Exception {
            mockMvc.perform(get(BASE_URL + "/compare")
                            .param("strategyIds", testStrategyIds.get(0)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStrategies").value(1))
                    .andExpect(jsonPath("$.strategies[0].rank").value(1));
        }

        @Test
        @DisplayName("Should skip non-existent strategies")
        void compareStrategies_MixedExistence() throws Exception {
            String nonExistentId = UlidGenerator.generate();
            String strategyIds = testStrategyIds.get(0) + "," + nonExistentId;

            mockMvc.perform(get(BASE_URL + "/compare")
                            .param("strategyIds", strategyIds))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStrategies").value(1));
        }

        @Test
        @DisplayName("Should rank strategies by total return")
        void compareStrategies_RankingOrder() throws Exception {
            String strategyIds = String.join(",", testStrategyIds);

            mockMvc.perform(get(BASE_URL + "/compare")
                            .param("strategyIds", strategyIds))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategies[0].rank").value(1))
                    .andExpect(jsonPath("$.strategies[1].rank").value(2))
                    .andExpect(jsonPath("$.strategies[2].rank").value(3));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/strategies/ranking - Strategy Ranking")
    class StrategyRanking {

        @Test
        @DisplayName("Should return strategy ranking with default parameters")
        void getRanking_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ranking"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rankings").isArray())
                    .andExpect(jsonPath("$.period").value(30))
                    .andExpect(jsonPath("$.sortBy").value("totalReturn"))
                    .andExpect(jsonPath("$.totalStrategies").exists());
        }

        @Test
        @DisplayName("Should return ranking sorted by win rate")
        void getRanking_SortByWinRate() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ranking")
                            .param("sortBy", "winRate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortBy").value("winRate"));
        }

        @Test
        @DisplayName("Should return ranking sorted by Sharpe ratio")
        void getRanking_SortBySharpeRatio() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ranking")
                            .param("sortBy", "sharpeRatio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sortBy").value("sharpeRatio"));
        }

        @Test
        @DisplayName("Should return ranking with custom period")
        void getRanking_CustomPeriod() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ranking")
                            .param("period", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.period").value(7));
        }

        @Test
        @DisplayName("Should limit number of results")
        void getRanking_WithLimit() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ranking")
                            .param("limit", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rankings.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("Should include rank in each strategy")
        void getRanking_IncludesRanks() throws Exception {
            mockMvc.perform(get(BASE_URL + "/ranking"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rankings[0].rank").value(1));
        }
    }

    // ==================== Helper Methods ====================

    private void createTestStrategies() {
        String[] names = {"Momentum Strategy", "Mean Reversion Strategy", "Trend Following Strategy"};
        BigDecimal[] pnlMultipliers = {BigDecimal.valueOf(3), BigDecimal.valueOf(1), BigDecimal.valueOf(2)};

        for (int i = 0; i < names.length; i++) {
            String strategyId = createStrategy(names[i]);
            testStrategyIds.add(strategyId);
            createPerformanceData(strategyId, pnlMultipliers[i]);
        }
    }

    private String createStrategy(String name) {
        String strategyId = UlidGenerator.generate();
        String versionId = UlidGenerator.generate();

        StrategyEntity strategy = StrategyEntity.builder()
                .strategyId(strategyId)
                .name(name)
                .description("Test strategy: " + name)
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(versionId)
                .delyn("N")
                .build();
        strategyRepository.save(strategy);

        return strategyId;
    }

    private void createPerformanceData(String strategyId, BigDecimal pnlMultiplier) {
        LocalDate today = LocalDate.now();
        String accountId = UlidGenerator.generate();

        for (int i = 0; i < 30; i++) {
            BigDecimal dailyPnl = BigDecimal.valueOf(50000)
                    .multiply(pnlMultiplier)
                    .multiply(BigDecimal.valueOf(i % 3 == 0 ? -1 : 1));

            DailyPerformanceEntity perf = DailyPerformanceEntity.builder()
                    .performanceId(UlidGenerator.generate())
                    .accountId(accountId)
                    .strategyId(strategyId)
                    .tradeDate(today.minusDays(i))
                    .totalTrades(5 + i % 5)
                    .winningTrades(3 + i % 3)
                    .losingTrades(2 + i % 2)
                    .totalPnl(dailyPnl)
                    .realizedPnl(dailyPnl.multiply(BigDecimal.valueOf(0.8)))
                    .unrealizedPnl(dailyPnl.multiply(BigDecimal.valueOf(0.2)))
                    .totalVolume(BigDecimal.valueOf(10000000))
                    .totalFees(BigDecimal.valueOf(15000))
                    .build();
            dailyPerformanceRepository.save(perf);
        }
    }
}
