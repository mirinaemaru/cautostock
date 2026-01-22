package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.UpdateRiskRuleRequest;
import maru.trading.application.ports.repo.RiskRuleRepository;
import maru.trading.domain.risk.RiskRule;
import maru.trading.domain.risk.RiskRuleScope;
import maru.trading.infra.config.UlidGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Risk Rule Admin Controller Test
 *
 * Tests Risk Rule Admin API endpoints:
 * - GET /api/v1/admin/risk-rules - List all risk rules
 * - POST /api/v1/admin/risk-rules - Create a new risk rule
 * - GET /api/v1/admin/risk-rules/{ruleId} - Get a specific risk rule
 * - POST /api/v1/admin/risk-rules/global - Update global risk rule
 * - POST /api/v1/admin/risk-rules/account/{accountId} - Update account-specific rule
 * - POST /api/v1/admin/risk-rules/account/{accountId}/symbol/{symbol} - Update symbol-specific rule
 * - GET /api/v1/admin/risk-rules/account/{accountId} - Get all rules for an account
 * - DELETE /api/v1/admin/risk-rules/{ruleId} - Delete a risk rule
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Risk Rule Admin Controller Test")
class RiskRuleAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RiskRuleRepository riskRuleRepository;

    private static final String BASE_URL = "/api/v1/admin/risk-rules";
    private String testAccountId;
    private String testRuleId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestRiskRules();
    }

    @Nested
    @DisplayName("GET /api/v1/admin/risk-rules - List Risk Rules")
    class ListRiskRules {

        @Test
        @DisplayName("Should return all risk rules")
        void listAll_Success() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").isNumber())
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        @DisplayName("Should support pagination")
        void listWithPagination_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.size").value(10));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/risk-rules - Create Risk Rule")
    class CreateRiskRule {

        @Test
        @DisplayName("Should create new risk rule")
        void createRule_Success() throws Exception {
            UpdateRiskRuleRequest request = UpdateRiskRuleRequest.builder()
                    .maxPositionValuePerSymbol(BigDecimal.valueOf(50000000))
                    .maxOpenOrders(20)
                    .maxOrdersPerMinute(10)
                    .dailyLossLimit(BigDecimal.valueOf(2000000))
                    .consecutiveOrderFailuresLimit(5)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riskRuleId").exists())
                    .andExpect(jsonPath("$.scope").value("GLOBAL"))
                    .andExpect(jsonPath("$.maxOpenOrders").value(20));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/risk-rules/{ruleId} - Get Risk Rule")
    class GetRiskRule {

        @Test
        @DisplayName("Should return risk rule by ID")
        void getRule_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testRuleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riskRuleId").value(testRuleId))
                    .andExpect(jsonPath("$.scope").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent rule")
        void getRule_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();
            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/risk-rules/global - Update Global Rule")
    class UpdateGlobalRule {

        @Test
        @DisplayName("Should update global risk rule")
        void updateGlobal_Success() throws Exception {
            UpdateRiskRuleRequest request = UpdateRiskRuleRequest.builder()
                    .maxPositionValuePerSymbol(BigDecimal.valueOf(100000000))
                    .maxOpenOrders(50)
                    .maxOrdersPerMinute(20)
                    .dailyLossLimit(BigDecimal.valueOf(5000000))
                    .consecutiveOrderFailuresLimit(10)
                    .build();

            mockMvc.perform(post(BASE_URL + "/global")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("GLOBAL"))
                    .andExpect(jsonPath("$.maxOpenOrders").value(50));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/risk-rules/account/{accountId} - Update Account Rule")
    class UpdateAccountRule {

        @Test
        @DisplayName("Should update account-specific rule")
        void updateAccount_Success() throws Exception {
            UpdateRiskRuleRequest request = UpdateRiskRuleRequest.builder()
                    .maxPositionValuePerSymbol(BigDecimal.valueOf(30000000))
                    .maxOpenOrders(15)
                    .maxOrdersPerMinute(8)
                    .dailyLossLimit(BigDecimal.valueOf(1500000))
                    .consecutiveOrderFailuresLimit(3)
                    .build();

            mockMvc.perform(post(BASE_URL + "/account/" + testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("PER_ACCOUNT"))
                    .andExpect(jsonPath("$.accountId").value(testAccountId));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/risk-rules/account/{accountId}/symbol/{symbol} - Update Symbol Rule")
    class UpdateSymbolRule {

        @Test
        @DisplayName("Should update symbol-specific rule")
        void updateSymbol_Success() throws Exception {
            UpdateRiskRuleRequest request = UpdateRiskRuleRequest.builder()
                    .maxPositionValuePerSymbol(BigDecimal.valueOf(10000000))
                    .maxOpenOrders(5)
                    .maxOrdersPerMinute(3)
                    .dailyLossLimit(BigDecimal.valueOf(500000))
                    .consecutiveOrderFailuresLimit(2)
                    .build();

            mockMvc.perform(post(BASE_URL + "/account/" + testAccountId + "/symbol/005930")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("PER_SYMBOL"))
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.symbol").value("005930"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/risk-rules/account/{accountId} - Get Rules for Account")
    class GetRulesForAccount {

        @Test
        @DisplayName("Should return all rules for account")
        void getRulesForAccount_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/account/" + testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.rules").isArray())
                    .andExpect(jsonPath("$.count").isNumber());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/risk-rules/{ruleId} - Delete Risk Rule")
    class DeleteRiskRule {

        @Test
        @DisplayName("Should delete risk rule")
        void deleteRule_Success() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + testRuleId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));
        }
    }

    // ==================== Helper Methods ====================

    private void createTestRiskRules() {
        RiskRule globalRule = RiskRule.builder()
                .riskRuleId(UlidGenerator.generate())
                .scope(RiskRuleScope.GLOBAL)
                .maxPositionValuePerSymbol(BigDecimal.valueOf(50000000))
                .maxOpenOrders(20)
                .maxOrdersPerMinute(10)
                .dailyLossLimit(BigDecimal.valueOf(2000000))
                .consecutiveOrderFailuresLimit(5)
                .build();
        riskRuleRepository.save(globalRule);
        testRuleId = globalRule.getRiskRuleId();

        RiskRule accountRule = RiskRule.builder()
                .riskRuleId(UlidGenerator.generate())
                .scope(RiskRuleScope.PER_ACCOUNT)
                .accountId(testAccountId)
                .maxPositionValuePerSymbol(BigDecimal.valueOf(30000000))
                .maxOpenOrders(15)
                .maxOrdersPerMinute(8)
                .dailyLossLimit(BigDecimal.valueOf(1500000))
                .consecutiveOrderFailuresLimit(3)
                .build();
        riskRuleRepository.save(accountRule);
    }
}
