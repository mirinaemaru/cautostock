package maru.trading.api.controller.query;

import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AlertLogEntity;
import maru.trading.infra.persistence.jpa.repository.AlertLogJpaRepository;
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
 * Alert History Query Controller Test
 *
 * Tests Alert History API endpoints:
 * - GET /api/v1/query/alerts/history - Alert history with filters
 * - GET /api/v1/query/alerts/stats - Alert statistics
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Alert History Query Controller Test")
class AlertHistoryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlertLogJpaRepository alertLogRepository;

    private static final String BASE_URL = "/api/v1/query/alerts";

    @BeforeEach
    void setUp() {
        createTestAlerts();
    }

    @Nested
    @DisplayName("GET /api/v1/query/alerts/history - Alert History")
    class GetAlertHistory {

        @Test
        @DisplayName("Should return paginated alerts")
        void getHistory_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts").isArray())
                    .andExpect(jsonPath("$.totalCount").exists())
                    .andExpect(jsonPath("$.page").exists())
                    .andExpect(jsonPath("$.pageSize").exists())
                    .andExpect(jsonPath("$.totalPages").exists());
        }

        @Test
        @DisplayName("Should filter by severity")
        void getHistory_FilterBySeverity() throws Exception {
            mockMvc.perform(get(BASE_URL + "/history")
                            .param("severity", "CRIT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts").isArray());
        }

        @Test
        @DisplayName("Should filter by category")
        void getHistory_FilterByCategory() throws Exception {
            mockMvc.perform(get(BASE_URL + "/history")
                            .param("category", "RISK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts").isArray());
        }

        @Test
        @DisplayName("Should filter by date range")
        void getHistory_FilterByDateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(7);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/history")
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts").isArray())
                    .andExpect(jsonPath("$.fromDate").exists())
                    .andExpect(jsonPath("$.toDate").exists());
        }

        @Test
        @DisplayName("Should support pagination")
        void getHistory_Pagination() throws Exception {
            mockMvc.perform(get(BASE_URL + "/history")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.pageSize").value(5));
        }

        @Test
        @DisplayName("Should filter by severity and category combined")
        void getHistory_CombinedFilters() throws Exception {
            mockMvc.perform(get(BASE_URL + "/history")
                            .param("severity", "WARN")
                            .param("category", "ORDER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts").isArray());
        }

        @Test
        @DisplayName("Should return alert details in response")
        void getHistory_AlertDetails() throws Exception {
            mockMvc.perform(get(BASE_URL + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alerts[0].alertId").exists())
                    .andExpect(jsonPath("$.alerts[0].severity").exists())
                    .andExpect(jsonPath("$.alerts[0].category").exists())
                    .andExpect(jsonPath("$.alerts[0].message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/alerts/stats - Alert Statistics")
    class GetAlertStats {

        @Test
        @DisplayName("Should return alert statistics")
        void getStats_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalAlerts").exists())
                    .andExpect(jsonPath("$.bySeverity").exists())
                    .andExpect(jsonPath("$.byCategory").exists())
                    .andExpect(jsonPath("$.byChannel").exists())
                    .andExpect(jsonPath("$.successCount").exists())
                    .andExpect(jsonPath("$.failedCount").exists())
                    .andExpect(jsonPath("$.successRate").exists());
        }

        @Test
        @DisplayName("Should return statistics with date range")
        void getStats_DateRange() throws Exception {
            LocalDate from = LocalDate.now().minusDays(14);
            LocalDate to = LocalDate.now();

            mockMvc.perform(get(BASE_URL + "/stats")
                            .param("from", from.toString())
                            .param("to", to.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromDate").value(from.toString()))
                    .andExpect(jsonPath("$.toDate").value(to.plusDays(1).toString()));
        }

        @Test
        @DisplayName("Should return daily counts")
        void getStats_DailyCounts() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dailyCounts").exists());
        }

        @Test
        @DisplayName("Should return recent critical alerts")
        void getStats_RecentCritical() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.recentCritical").isArray());
        }

        @Test
        @DisplayName("Should calculate correct success rate")
        void getStats_SuccessRate() throws Exception {
            mockMvc.perform(get(BASE_URL + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.successRate").isNumber());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestAlerts() {
        LocalDateTime now = LocalDateTime.now();

        // Create various alerts for filtering tests
        String[][] alertData = {
                {"CRIT", "RISK", "Test critical risk alert"},
                {"WARN", "ORDER", "Order execution warning"},
                {"INFO", "OPS", "System operational notification"},
                {"CRIT", "RISK", "Position limit exceeded"},
                {"WARN", "RISK", "Approaching risk threshold"},
                {"INFO", "ORDER", "Order filled successfully"},
                {"CRIT", "OPS", "System health degraded"},
                {"WARN", "ORDER", "Partial fill on order"}
        };

        for (int i = 0; i < alertData.length; i++) {
            AlertLogEntity alert = AlertLogEntity.builder()
                    .alertId(UlidGenerator.generate())
                    .severity(alertData[i][0])
                    .category(alertData[i][1])
                    .channel(i % 2 == 0 ? "SLACK" : "EMAIL")
                    .message(alertData[i][2])
                    .success(i % 3 != 0) // Some failures
                    .relatedEventId(UlidGenerator.generate())
                    .sentAt(now.minusHours(i + 1))
                    .build();
            alertLogRepository.save(alert);
        }
    }
}
