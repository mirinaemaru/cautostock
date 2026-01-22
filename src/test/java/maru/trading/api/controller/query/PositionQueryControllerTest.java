package maru.trading.api.controller.query;

import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
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
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Position Query Controller Test
 *
 * Tests Position Query API endpoints:
 * - GET /api/v1/query/positions - Search positions
 * - GET /api/v1/query/positions/summary - Position summary
 * - GET /api/v1/query/positions/{positionId} - Get position by ID
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Position Query Controller Test")
class PositionQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PositionJpaRepository positionRepository;

    private static final String BASE_URL = "/api/v1/query/positions";
    private String testAccountId;
    private String testPositionId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestPositions();
    }

    @Nested
    @DisplayName("GET /api/v1/query/positions - Search Positions")
    class SearchPositions {

        @Test
        @DisplayName("Should return all positions without filters")
        void searchPositions_NoFilters() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("Should filter positions by accountId")
        void searchPositions_ByAccountId() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.total").exists());
        }

        @Test
        @DisplayName("Should return empty for non-existent account")
        void searchPositions_NonExistentAccount() throws Exception {
            String nonExistentAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", nonExistentAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.total").value(0));
        }

        @Test
        @DisplayName("Should return position details in response")
        void searchPositions_PositionDetails() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].positionId").exists())
                    .andExpect(jsonPath("$.items[0].accountId").exists())
                    .andExpect(jsonPath("$.items[0].symbol").exists())
                    .andExpect(jsonPath("$.items[0].qty").exists())
                    .andExpect(jsonPath("$.items[0].avgPrice").exists())
                    .andExpect(jsonPath("$.items[0].realizedPnl").exists())
                    .andExpect(jsonPath("$.items[0].currentPrice").exists())
                    .andExpect(jsonPath("$.items[0].unrealizedPnl").exists())
                    .andExpect(jsonPath("$.items[0].totalValue").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/positions/summary - Position Summary")
    class GetPositionSummary {

        @Test
        @DisplayName("Should return position summary with accountId")
        void getSummary_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary")
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.totalPositions").exists())
                    .andExpect(jsonPath("$.totalValue").exists())
                    .andExpect(jsonPath("$.totalRealizedPnl").exists())
                    .andExpect(jsonPath("$.totalUnrealizedPnl").exists())
                    .andExpect(jsonPath("$.symbolCount").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return summary without accountId filter")
        void getSummary_NoAccountId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPositions").exists())
                    .andExpect(jsonPath("$.totalValue").exists());
        }

        @Test
        @DisplayName("Should return zero values for empty account")
        void getSummary_EmptyAccount() throws Exception {
            String emptyAccountId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/summary")
                            .param("accountId", emptyAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPositions").value(0))
                    .andExpect(jsonPath("$.totalValue").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/positions/{positionId} - Get Position by ID")
    class GetPositionById {

        @Test
        @DisplayName("Should return position by ID")
        void getPosition_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testPositionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.positionId").value(testPositionId))
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.symbol").exists())
                    .andExpect(jsonPath("$.qty").exists())
                    .andExpect(jsonPath("$.avgPrice").exists())
                    .andExpect(jsonPath("$.realizedPnl").exists())
                    .andExpect(jsonPath("$.currentPrice").exists())
                    .andExpect(jsonPath("$.unrealizedPnl").exists())
                    .andExpect(jsonPath("$.totalValue").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return 500 for non-existent position")
        void getPosition_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestPositions() {
        String[][] positionData = {
                {"005930", "100", "70000", "50000"},    // Samsung Electronics
                {"035720", "50", "150000", "25000"},    // Kakao
                {"000660", "30", "120000", "15000"}     // SK Hynix
        };

        for (int i = 0; i < positionData.length; i++) {
            String positionId = UlidGenerator.generate();
            if (i == 0) {
                testPositionId = positionId;
            }

            PositionEntity position = PositionEntity.builder()
                    .positionId(positionId)
                    .accountId(testAccountId)
                    .symbol(positionData[i][0])
                    .qty(new BigDecimal(positionData[i][1]))
                    .avgPrice(new BigDecimal(positionData[i][2]))
                    .realizedPnl(new BigDecimal(positionData[i][3]))
                    .updatedAt(LocalDateTime.now().minusHours(i))
                    .build();
            positionRepository.save(position);
        }
    }
}
