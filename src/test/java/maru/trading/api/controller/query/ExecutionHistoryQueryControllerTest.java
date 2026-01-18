package maru.trading.api.controller.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.ExecutionHistoryEntity;
import maru.trading.infra.persistence.jpa.repository.ExecutionHistoryJpaRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Execution History Query Controller Test
 *
 * Tests Execution History API endpoints:
 * - GET /api/v1/query/execution-history - List execution history
 * - GET /api/v1/query/execution-history/{id} - Get specific execution
 * - GET /api/v1/query/execution-history/strategy/{strategyId} - Get by strategy
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Execution History Query Controller Test")
class ExecutionHistoryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExecutionHistoryJpaRepository executionHistoryRepository;

    private static final String BASE_URL = "/api/v1/query/execution-history";
    private String testAccountId;
    private String testStrategyId;
    private String testExecutionId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        testStrategyId = UlidGenerator.generate();
        testExecutionId = createTestExecutionHistory();
    }

    @Nested
    @DisplayName("GET /api/v1/query/execution-history - List Execution History")
    class ListExecutionHistory {

        @Test
        @DisplayName("Should list execution history with filters")
        void listExecutionHistory_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("strategyId", testStrategyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executions").isArray())
                    .andExpect(jsonPath("$.totalCount").exists())
                    .andExpect(jsonPath("$.successCount").exists())
                    .andExpect(jsonPath("$.failedCount").exists());
        }

        @Test
        @DisplayName("Should filter by account ID")
        void listExecutionHistory_ByAccount() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executions").isArray());
        }

        @Test
        @DisplayName("Should filter by execution type")
        void listExecutionHistory_ByType() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("strategyId", testStrategyId)
                            .param("executionType", "SIGNAL_GENERATED"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should filter by status")
        void listExecutionHistory_ByStatus() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("strategyId", testStrategyId)
                            .param("status", "SUCCESS"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should filter by date range")
        void listExecutionHistory_ByDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL)
                            .param("strategyId", testStrategyId)
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should support pagination")
        void listExecutionHistory_Pagination() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("strategyId", testStrategyId)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/execution-history/{id} - Get Execution")
    class GetExecution {

        @Test
        @DisplayName("Should return execution by ID")
        void getExecution_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testExecutionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executionId").value(testExecutionId))
                    .andExpect(jsonPath("$.strategyId").value(testStrategyId))
                    .andExpect(jsonPath("$.executionType").exists())
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent execution")
        void getExecution_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/execution-history/strategy/{strategyId} - Get By Strategy")
    class GetByStrategy {

        @Test
        @DisplayName("Should return execution history for strategy")
        void getByStrategy_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/strategy/" + testStrategyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executions").isArray())
                    .andExpect(jsonPath("$.successCount").exists())
                    .andExpect(jsonPath("$.failedCount").exists());
        }

        @Test
        @DisplayName("Should filter by date range")
        void getByStrategy_WithDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(30);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/strategy/" + testStrategyId)
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return empty list for strategy with no executions")
        void getByStrategy_Empty() throws Exception {
            String emptyStrategyId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/strategy/" + emptyStrategyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.executions").isArray())
                    .andExpect(jsonPath("$.totalCount").value(0));
        }
    }

    // ==================== Helper Methods ====================

    private String createTestExecutionHistory() {
        // Create multiple execution history entries
        String[] types = {"SIGNAL_GENERATED", "ORDER_PLACED", "ORDER_FILLED", "RISK_CHECK"};
        String[] statuses = {"SUCCESS", "SUCCESS", "FAILED", "SUCCESS"};

        String firstId = null;
        for (int i = 0; i < types.length; i++) {
            ExecutionHistoryEntity execution = ExecutionHistoryEntity.builder()
                    .executionId(UlidGenerator.generate())
                    .strategyId(testStrategyId)
                    .accountId(testAccountId)
                    .executionType(types[i])
                    .status(statuses[i])
                    .symbol("005930")
                    .description("Test execution " + i)
                    .details("{\"key\": \"value\"}")
                    .executionTimeMs(100 + i * 50)
                    .createdAt(LocalDateTime.now().minusMinutes(i * 10))
                    .build();

            if (statuses[i].equals("FAILED")) {
                execution = ExecutionHistoryEntity.builder()
                        .executionId(execution.getExecutionId())
                        .strategyId(testStrategyId)
                        .accountId(testAccountId)
                        .executionType(types[i])
                        .status(statuses[i])
                        .symbol("005930")
                        .description("Test execution " + i)
                        .details("{\"key\": \"value\"}")
                        .errorMessage("Order rejected: insufficient balance")
                        .executionTimeMs(100 + i * 50)
                        .createdAt(LocalDateTime.now().minusMinutes(i * 10))
                        .build();
            }

            ExecutionHistoryEntity saved = executionHistoryRepository.save(execution);
            if (firstId == null) {
                firstId = saved.getExecutionId();
            }
        }

        return firstId;
    }
}
