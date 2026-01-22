package maru.trading.api.controller.query;

import maru.trading.application.ports.repo.FillRepository;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.order.Side;
import maru.trading.infra.config.UlidGenerator;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fill Query Controller Test
 *
 * Tests Fill Query API endpoints:
 * - GET /api/v1/query/fills - Query fills with filters
 * - GET /api/v1/query/fills/statistics - Fill statistics
 * - GET /api/v1/query/fills/{fillId} - Get fill by ID
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Fill Query Controller Test")
class FillQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FillRepository fillRepository;

    private static final String BASE_URL = "/api/v1/query/fills";
    private String testAccountId;
    private String testOrderId;
    private String testFillId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        testOrderId = UlidGenerator.generate();
        createTestFills();
    }

    @Nested
    @DisplayName("GET /api/v1/query/fills - Query Fills")
    class QueryFills {

        @Test
        @DisplayName("Should return all fills without filters")
        void queryFills_NoFilters() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty());
        }

        @Test
        @DisplayName("Should filter fills by orderId")
        void queryFills_ByOrderId() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("orderId", testOrderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should filter fills by accountId and symbol")
        void queryFills_ByAccountIdAndSymbol() throws Exception {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId)
                            .param("symbol", "005930")
                            .param("from", from.format(DateTimeFormatter.ISO_DATE_TIME))
                            .param("to", to.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should filter fills by accountId only")
        void queryFills_ByAccountIdOnly() throws Exception {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId)
                            .param("from", from.format(DateTimeFormatter.ISO_DATE_TIME))
                            .param("to", to.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray());
        }

        @Test
        @DisplayName("Should return fill details in response")
        void queryFills_FillDetails() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].fillId").exists())
                    .andExpect(jsonPath("$.items[0].orderId").exists())
                    .andExpect(jsonPath("$.items[0].accountId").exists())
                    .andExpect(jsonPath("$.items[0].symbol").exists())
                    .andExpect(jsonPath("$.items[0].side").exists())
                    .andExpect(jsonPath("$.items[0].fillPrice").exists())
                    .andExpect(jsonPath("$.items[0].fillQty").exists())
                    .andExpect(jsonPath("$.items[0].fee").exists())
                    .andExpect(jsonPath("$.items[0].tax").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/fills/statistics - Fill Statistics")
    class GetFillStatistics {

        @Test
        @DisplayName("Should return fill statistics without filters")
        void getStatistics_NoFilters() throws Exception {
            mockMvc.perform(get(BASE_URL + "/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalFills").exists())
                    .andExpect(jsonPath("$.totalVolume").exists())
                    .andExpect(jsonPath("$.totalFees").exists())
                    .andExpect(jsonPath("$.buyCount").exists())
                    .andExpect(jsonPath("$.sellCount").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return fill statistics with accountId filter")
        void getStatistics_ByAccountId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/statistics")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.totalFills").exists())
                    .andExpect(jsonPath("$.totalVolume").exists());
        }

        @Test
        @DisplayName("Should return fill statistics with date range")
        void getStatistics_WithDateRange() throws Exception {
            LocalDateTime from = LocalDateTime.now().minusDays(7);
            LocalDateTime to = LocalDateTime.now().plusDays(1);

            mockMvc.perform(get(BASE_URL + "/statistics")
                            .param("accountId", testAccountId)
                            .param("from", from.format(DateTimeFormatter.ISO_DATE_TIME))
                            .param("to", to.format(DateTimeFormatter.ISO_DATE_TIME)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fromDate").exists())
                    .andExpect(jsonPath("$.toDate").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/fills/{fillId} - Get Fill by ID")
    class GetFillById {

        @Test
        @DisplayName("Should return fill by ID")
        void getFill_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testFillId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fillId").value(testFillId))
                    .andExpect(jsonPath("$.orderId").exists())
                    .andExpect(jsonPath("$.accountId").exists())
                    .andExpect(jsonPath("$.symbol").exists())
                    .andExpect(jsonPath("$.side").exists())
                    .andExpect(jsonPath("$.fillPrice").exists())
                    .andExpect(jsonPath("$.fillQty").exists())
                    .andExpect(jsonPath("$.fee").exists())
                    .andExpect(jsonPath("$.tax").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent fill")
        void getFill_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestFills() {
        LocalDateTime now = LocalDateTime.now();
        String[][] fillData = {
                {"005930", "BUY", "70000", "100", "1050", "1610"},
                {"005930", "SELL", "72000", "50", "540", "828"},
                {"035720", "BUY", "150000", "30", "675", "1035"},
                {"000660", "SELL", "125000", "20", "375", "575"}
        };

        for (int i = 0; i < fillData.length; i++) {
            String fillId = UlidGenerator.generate();
            if (i == 0) {
                testFillId = fillId;
            }

            Fill fill = new Fill(
                    fillId,
                    testOrderId,
                    testAccountId,
                    fillData[i][0],
                    Side.valueOf(fillData[i][1]),
                    new BigDecimal(fillData[i][2]),
                    Integer.parseInt(fillData[i][3]),
                    new BigDecimal(fillData[i][4]),
                    new BigDecimal(fillData[i][5]),
                    now.minusHours(i),
                    "KIS" + String.format("%06d", i)
            );
            fillRepository.save(fill);
        }
    }
}
