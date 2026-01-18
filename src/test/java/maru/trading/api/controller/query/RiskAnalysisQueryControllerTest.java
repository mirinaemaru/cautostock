package maru.trading.api.controller.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.DailyPerformanceJpaRepository;
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
 * Risk Analysis Query Controller Test
 *
 * Tests Risk Analysis API endpoints:
 * - GET /api/v1/query/risk/var - Value at Risk analysis
 * - GET /api/v1/query/risk/correlation - Correlation analysis
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Risk Analysis Query Controller Test")
class RiskAnalysisQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    private static final String BASE_URL = "/api/v1/query/risk";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestPositions();
        createTestPerformanceData();
    }

    @Nested
    @DisplayName("GET /api/v1/query/risk/var - VaR Analysis")
    class VaRAnalysis {

        @Test
        @DisplayName("Should return VaR analysis with default parameters")
        void getVaR_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/var")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.method").value("HISTORICAL"))
                    .andExpect(jsonPath("$.confidenceLevel").value(95))
                    .andExpect(jsonPath("$.holdingPeriod").value(1))
                    .andExpect(jsonPath("$.portfolioValue").exists())
                    .andExpect(jsonPath("$.var").exists())
                    .andExpect(jsonPath("$.varPct").exists())
                    .andExpect(jsonPath("$.cvar").exists());
        }

        @Test
        @DisplayName("Should return VaR analysis with parametric method")
        void getVaR_ParametricMethod() throws Exception {
            mockMvc.perform(get(BASE_URL + "/var")
                            .param("accountId", testAccountId)
                            .param("method", "PARAMETRIC")
                            .param("confidenceLevel", "99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.method").value("PARAMETRIC"))
                    .andExpect(jsonPath("$.confidenceLevel").value(99));
        }

        @Test
        @DisplayName("Should return VaR analysis with custom holding period")
        void getVaR_CustomHoldingPeriod() throws Exception {
            mockMvc.perform(get(BASE_URL + "/var")
                            .param("accountId", testAccountId)
                            .param("holdingPeriod", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.holdingPeriod").value(10));
        }

        @Test
        @DisplayName("Should return empty VaR for account with no positions")
        void getVaR_NoPositions() throws Exception {
            String emptyAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/var")
                            .param("accountId", emptyAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.portfolioValue").value(0))
                    .andExpect(jsonPath("$.var").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/risk/correlation - Correlation Analysis")
    class CorrelationAnalysis {

        @Test
        @DisplayName("Should return correlation analysis")
        void getCorrelation_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/correlation")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.symbols").isArray())
                    .andExpect(jsonPath("$.timeframe").value("daily"));
        }

        @Test
        @DisplayName("Should return correlation with date range")
        void getCorrelation_WithDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/correlation")
                            .param("accountId", testAccountId)
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromDate").value(from.toString()))
                    .andExpect(jsonPath("$.toDate").value(to.toString()));
        }

        @Test
        @DisplayName("Should handle account with single position")
        void getCorrelation_SinglePosition() throws Exception {
            // Create account with single position
            String singlePosAccountId = UlidGenerator.generate();
            createSinglePosition(singlePosAccountId);

            mockMvc.perform(get(BASE_URL + "/correlation")
                            .param("accountId", singlePosAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbols").isArray());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestPositions() {
        // Create multiple positions for correlation analysis
        String[] symbols = {"005930", "035720", "000660"};
        for (int i = 0; i < symbols.length; i++) {
            PositionEntity position = PositionEntity.builder()
                    .positionId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .symbol(symbols[i])
                    .qty(BigDecimal.valueOf(100))
                    .avgPrice(BigDecimal.valueOf(50000 + i * 10000))
                    .realizedPnl(BigDecimal.ZERO)
                    .updatedAt(LocalDateTime.now())
                    .build();
            positionRepository.save(position);
        }
    }

    private void createSinglePosition(String accountId) {
        PositionEntity position = PositionEntity.builder()
                .positionId(UlidGenerator.generate())
                .accountId(accountId)
                .symbol("005930")
                .qty(BigDecimal.valueOf(100))
                .avgPrice(BigDecimal.valueOf(70000))
                .realizedPnl(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
        positionRepository.save(position);
    }

    private void createTestPerformanceData() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            DailyPerformanceEntity perf = DailyPerformanceEntity.builder()
                    .performanceId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .tradeDate(today.minusDays(i))
                    .totalTrades(5)
                    .winningTrades(3)
                    .losingTrades(2)
                    .totalPnl(BigDecimal.valueOf(Math.random() * 200000 - 100000))
                    .realizedPnl(BigDecimal.valueOf(50000))
                    .unrealizedPnl(BigDecimal.valueOf(20000))
                    .totalVolume(BigDecimal.valueOf(5000000))
                    .totalFees(BigDecimal.valueOf(7500))
                    .build();
            dailyPerformanceRepository.save(perf);
        }
    }
}
