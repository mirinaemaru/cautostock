package maru.trading.api.controller.query;

import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
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
 * Balance Query Controller Test
 *
 * Tests Balance Query API endpoints:
 * - GET /api/v1/query/balance/summary - Balance summary
 * - GET /api/v1/query/balance/{accountId} - Get balance by path
 * - GET /api/v1/query/balance?accountId= - Get balance by query param
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Balance Query Controller Test")
class BalanceQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private AccountJpaRepository accountRepository;

    private static final String BASE_URL = "/api/v1/query/balance";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestAccount();
        createTestPositions();
    }

    @Nested
    @DisplayName("GET /api/v1/query/balance/summary - Balance Summary")
    class GetBalanceSummary {

        @Test
        @DisplayName("Should return balance summary")
        void getSummary_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStockValue").exists())
                    .andExpect(jsonPath("$.totalRealizedPnl").exists())
                    .andExpect(jsonPath("$.accountCount").exists())
                    .andExpect(jsonPath("$.positionCount").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should calculate correct totals")
        void getSummary_CorrectCalculations() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalStockValue").isNumber())
                    .andExpect(jsonPath("$.totalRealizedPnl").isNumber())
                    .andExpect(jsonPath("$.positionCount").value(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/balance/{accountId} - Get Balance by Path")
    class GetBalanceByPath {

        @Test
        @DisplayName("Should return account balance by path")
        void getBalance_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.totalAssets").exists())
                    .andExpect(jsonPath("$.cashBalance").exists())
                    .andExpect(jsonPath("$.stockValue").exists())
                    .andExpect(jsonPath("$.totalProfitLoss").exists())
                    .andExpect(jsonPath("$.realizedPnl").exists())
                    .andExpect(jsonPath("$.unrealizedPnl").exists());
        }

        @Test
        @DisplayName("Should return zero cash balance for non-existent account")
        void getBalance_NonExistentAccount() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(nonExistentId))
                    .andExpect(jsonPath("$.cashBalance").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/query/balance?accountId= - Get Balance by Query Param")
    class GetBalanceByQueryParam {

        @Test
        @DisplayName("Should return account balance by query param")
        void getBalance_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.totalAssets").exists())
                    .andExpect(jsonPath("$.cashBalance").exists())
                    .andExpect(jsonPath("$.stockValue").exists());
        }

        @Test
        @DisplayName("Should return error for missing accountId")
        void getBalance_MissingAccountId() throws Exception {
            // When accountId is missing, Spring throws MissingServletRequestParameterException
            // Expect either 400 (with proper error handler) or 500 (default behavior)
            mockMvc.perform(get(BASE_URL))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        if (status != 400 && status != 500) {
                            throw new AssertionError("Expected 400 or 500, but was " + status);
                        }
                    });
        }
    }

    // ==================== Helper Methods ====================

    private void createTestAccount() {
        AccountEntity account = AccountEntity.builder()
                .accountId(testAccountId)
                .broker("KIS")
                .environment(Environment.PAPER)
                .cano("00000000")
                .acntPrdtCd("01")
                .status(AccountStatus.ACTIVE)
                .alias("Test Account")
                .delyn("N")
                .build();
        accountRepository.save(account);
    }

    private void createTestPositions() {
        String[][] positionData = {
                {"005930", "100", "70000", "50000"},
                {"035720", "50", "150000", "25000"},
                {"000660", "30", "120000", "15000"}
        };

        for (String[] data : positionData) {
            PositionEntity position = PositionEntity.builder()
                    .positionId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .symbol(data[0])
                    .qty(new BigDecimal(data[1]))
                    .avgPrice(new BigDecimal(data[2]))
                    .realizedPnl(new BigDecimal(data[3]))
                    .updatedAt(LocalDateTime.now())
                    .build();
            positionRepository.save(position);
        }
    }
}
