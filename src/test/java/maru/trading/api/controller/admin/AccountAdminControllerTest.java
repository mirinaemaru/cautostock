package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.AccountRegisterRequest;
import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
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

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Account Admin Controller Test
 *
 * Tests Account Admin API endpoints:
 * - POST /api/v1/admin/accounts - Register account
 * - GET /api/v1/admin/accounts - List accounts
 * - GET /api/v1/admin/accounts/{accountId} - Get account
 * - PUT /api/v1/admin/accounts/{accountId}/status - Update status
 * - PUT /api/v1/admin/accounts/{accountId} - Update account
 * - DELETE /api/v1/admin/accounts/{accountId} - Delete account
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Account Admin Controller Test")
class AccountAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountJpaRepository accountRepository;

    private static final String BASE_URL = "/api/v1/admin/accounts";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestAccount();
    }

    @Nested
    @DisplayName("POST /api/v1/admin/accounts - Register Account")
    class RegisterAccount {

        @Test
        @DisplayName("Should register account successfully")
        void register_Success() throws Exception {
            AccountRegisterRequest request = AccountRegisterRequest.builder()
                    .broker("KIS")
                    .environment(Environment.PAPER)
                    .cano("12345678")
                    .acntPrdtCd("01")
                    .alias("Test Account")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accountId").exists())
                    .andExpect(jsonPath("$.broker").value("KIS"))
                    .andExpect(jsonPath("$.environment").value("PAPER"))
                    .andExpect(jsonPath("$.cano").value("12345678"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void register_MissingFields() throws Exception {
            String invalidRequest = "{\"broker\": \"KIS\"}";

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts - List Accounts")
    class ListAccounts {

        @Test
        @DisplayName("Should return list of accounts")
        void list_Success() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts/{accountId} - Get Account")
    class GetAccount {

        @Test
        @DisplayName("Should return account by ID")
        void get_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.broker").exists())
                    .andExpect(jsonPath("$.environment").exists())
                    .andExpect(jsonPath("$.cano").exists())
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent account")
        void get_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{accountId}/status - Update Account Status")
    class UpdateAccountStatus {

        @Test
        @DisplayName("Should update account status to SUSPENDED")
        void updateStatus_Success() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("status", "SUSPENDED");

            mockMvc.perform(put(BASE_URL + "/" + testAccountId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("Should return 404 for non-existent account")
        void updateStatus_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();
            Map<String, String> request = new HashMap<>();
            request.put("status", "SUSPENDED");

            mockMvc.perform(put(BASE_URL + "/" + nonExistentId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{accountId} - Update Account")
    class UpdateAccount {

        @Test
        @DisplayName("Should update account alias")
        void update_Alias() throws Exception {
            Map<String, String> request = new HashMap<>();
            request.put("alias", "Updated Alias");

            mockMvc.perform(put(BASE_URL + "/" + testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.alias").value("Updated Alias"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/accounts/{accountId} - Delete Account")
    class DeleteAccount {

        @Test
        @DisplayName("Should soft delete account")
        void delete_Success() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + testAccountId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 for non-existent account")
        void delete_NotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(delete(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
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
}
