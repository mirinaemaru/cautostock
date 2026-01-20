package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.domain.account.AccountStatus;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.AccountPermissionEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import maru.trading.infra.persistence.jpa.repository.AccountPermissionJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Account Permission Admin Controller Integration Test
 *
 * Tests Account Permission API endpoints with real database:
 * - PUT /api/v1/admin/accounts/{accountId}/permissions - Update permissions
 * - GET /api/v1/admin/accounts/{accountId}/permissions - Get permissions
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Account Permission Admin Controller Integration Test")
class AccountPermissionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Autowired
    private AccountPermissionJpaRepository permissionRepository;

    private static final String BASE_URL = "/api/v1/admin/accounts";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = createTestAccount("Test Account");
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{accountId}/permissions - Update Permissions")
    class UpdatePermissions {

        @Test
        @DisplayName("Should update permissions successfully")
        void updatePermissions_Success() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true,
                                    "tradeSell": true,
                                    "autoTrade": false,
                                    "manualTrade": true,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.tradeBuy").value(true))
                    .andExpect(jsonPath("$.tradeSell").value(true))
                    .andExpect(jsonPath("$.autoTrade").value(false))
                    .andExpect(jsonPath("$.manualTrade").value(true))
                    .andExpect(jsonPath("$.paperOnly").value(true))
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should create permissions when not exists")
        void updatePermissions_CreateWhenNotExists() throws Exception {
            // 기존 permission이 없는 상태로 시작
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true,
                                    "tradeSell": false,
                                    "autoTrade": false,
                                    "manualTrade": false,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.tradeBuy").value(true))
                    .andExpect(jsonPath("$.tradeSell").value(false));
        }

        @Test
        @DisplayName("Should update existing permissions")
        void updatePermissions_UpdateExisting() throws Exception {
            // 먼저 permission 생성
            createTestPermission(testAccountId, false, false, false, false, true);

            // 그 다음 수정
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true,
                                    "tradeSell": true,
                                    "autoTrade": true,
                                    "manualTrade": true,
                                    "paperOnly": false
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(true))
                    .andExpect(jsonPath("$.tradeSell").value(true))
                    .andExpect(jsonPath("$.autoTrade").value(true))
                    .andExpect(jsonPath("$.manualTrade").value(true))
                    .andExpect(jsonPath("$.paperOnly").value(false));
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void updatePermissions_AccountNotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true,
                                    "tradeSell": true,
                                    "autoTrade": false,
                                    "manualTrade": true,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void updatePermissions_MissingFields() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true
                                }
                                """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts/{accountId}/permissions - Get Permissions")
    class GetPermissions {

        @Test
        @DisplayName("Should return permissions when exists")
        void getPermissions_Success() throws Exception {
            // Given - permission 생성
            createTestPermission(testAccountId, true, true, false, true, true);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.tradeBuy").value(true))
                    .andExpect(jsonPath("$.tradeSell").value(true))
                    .andExpect(jsonPath("$.autoTrade").value(false))
                    .andExpect(jsonPath("$.manualTrade").value(true))
                    .andExpect(jsonPath("$.paperOnly").value(true))
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return default permissions when not exists")
        void getPermissions_DefaultWhenNotExists() throws Exception {
            // When & Then - permission이 없는 상태
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.tradeBuy").value(false))
                    .andExpect(jsonPath("$.tradeSell").value(false))
                    .andExpect(jsonPath("$.autoTrade").value(false))
                    .andExpect(jsonPath("$.manualTrade").value(false))
                    .andExpect(jsonPath("$.paperOnly").value(true));
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void getPermissions_AccountNotFound() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("End-to-End Scenarios")
    class E2EScenarios {

        @Test
        @DisplayName("Should handle complete permission lifecycle")
        void permissionLifecycle() throws Exception {
            // 1. 최초 조회 - 기본값
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(false))
                    .andExpect(jsonPath("$.paperOnly").value(true));

            // 2. 권한 설정
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true,
                                    "tradeSell": true,
                                    "autoTrade": false,
                                    "manualTrade": true,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isOk());

            // 3. 설정된 권한 조회
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(true))
                    .andExpect(jsonPath("$.tradeSell").value(true));

            // 4. 권한 수정
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", testAccountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": false,
                                    "tradeSell": false,
                                    "autoTrade": false,
                                    "manualTrade": false,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(false));

            // 5. 수정된 권한 조회
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(false))
                    .andExpect(jsonPath("$.tradeSell").value(false));
        }

        @Test
        @DisplayName("Should handle multiple accounts independently")
        void multipleAccountsIndependent() throws Exception {
            String account1 = createTestAccount("Account 1");
            String account2 = createTestAccount("Account 2");

            // Account 1 권한 설정
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", account1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": true,
                                    "tradeSell": false,
                                    "autoTrade": false,
                                    "manualTrade": false,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isOk());

            // Account 2 권한 설정
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", account2)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                    "tradeBuy": false,
                                    "tradeSell": true,
                                    "autoTrade": false,
                                    "manualTrade": false,
                                    "paperOnly": true
                                }
                                """))
                    .andExpect(status().isOk());

            // 각 계좌별 권한이 독립적으로 유지되는지 확인
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", account1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(true))
                    .andExpect(jsonPath("$.tradeSell").value(false));

            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", account2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tradeBuy").value(false))
                    .andExpect(jsonPath("$.tradeSell").value(true));
        }
    }

    private String createTestAccount(String alias) {
        AccountEntity account = AccountEntity.builder()
                .accountId(UlidGenerator.generate())
                .broker("KIS")
                .environment(Environment.PAPER)
                .cano("50068999")
                .acntPrdtCd("01")
                .status(AccountStatus.ACTIVE)
                .alias(alias)
                .build();

        return accountRepository.save(account).getAccountId();
    }

    private void createTestPermission(String accountId, boolean tradeBuy, boolean tradeSell,
                                      boolean autoTrade, boolean manualTrade, boolean paperOnly) {
        AccountPermissionEntity permission = AccountPermissionEntity.builder()
                .accountId(accountId)
                .tradeBuy(tradeBuy)
                .tradeSell(tradeSell)
                .autoTrade(autoTrade)
                .manualTrade(manualTrade)
                .paperOnly(paperOnly)
                .build();

        permissionRepository.save(permission);
    }
}
