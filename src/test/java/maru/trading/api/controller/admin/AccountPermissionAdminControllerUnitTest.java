package maru.trading.api.controller.admin;

import maru.trading.api.dto.request.AccountPermissionUpdateRequest;
import maru.trading.api.dto.response.AccountPermissionResponse;
import maru.trading.api.exception.GlobalExceptionHandler;
import maru.trading.domain.account.AccountNotFoundException;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.AccountPermissionEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import maru.trading.infra.persistence.jpa.repository.AccountPermissionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Account Permission Admin Controller Unit Test
 *
 * Unit tests with mocked repositories to test controller logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Account Permission Admin Controller Unit Test")
class AccountPermissionAdminControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private AccountJpaRepository accountRepository;

    @Mock
    private AccountPermissionJpaRepository permissionRepository;

    @InjectMocks
    private AccountPermissionAdminController controller;

    private static final String BASE_URL = "/api/v1/admin/accounts";
    private static final String TEST_ACCOUNT_ID = "01KF9BPSTD82MHTFBNPNB3NDNJ";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{accountId}/permissions")
    class UpdatePermissions {

        @Test
        @DisplayName("Should update permissions successfully")
        void updatePermissions_Success() throws Exception {
            // Given
            AccountEntity account = createMockAccount();
            AccountPermissionEntity permission = createMockPermission(TEST_ACCOUNT_ID);

            when(accountRepository.findByIdAndNotDeleted(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.of(account));
            when(permissionRepository.findById(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.of(permission));
            when(permissionRepository.save(any(AccountPermissionEntity.class)))
                    .thenReturn(permission);

            // When & Then
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", TEST_ACCOUNT_ID)
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
                    .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                    .andExpect(jsonPath("$.tradeBuy").exists())
                    .andExpect(jsonPath("$.tradeSell").exists())
                    .andExpect(jsonPath("$.autoTrade").exists())
                    .andExpect(jsonPath("$.manualTrade").exists())
                    .andExpect(jsonPath("$.paperOnly").exists());

            verify(permissionRepository).save(any(AccountPermissionEntity.class));
        }

        @Test
        @DisplayName("Should create permissions when not exists")
        void updatePermissions_CreateWhenNotExists() throws Exception {
            // Given
            AccountEntity account = createMockAccount();
            AccountPermissionEntity newPermission = createMockPermission(TEST_ACCOUNT_ID);

            when(accountRepository.findByIdAndNotDeleted(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.of(account));
            when(permissionRepository.findById(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.empty());
            when(permissionRepository.save(any(AccountPermissionEntity.class)))
                    .thenReturn(newPermission);

            // When & Then
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", TEST_ACCOUNT_ID)
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
                    .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID));

            verify(permissionRepository).save(any(AccountPermissionEntity.class));
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void updatePermissions_AccountNotFound() throws Exception {
            // Given
            when(accountRepository.findByIdAndNotDeleted(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(put(BASE_URL + "/{accountId}/permissions", TEST_ACCOUNT_ID)
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
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts/{accountId}/permissions")
    class GetPermissions {

        @Test
        @DisplayName("Should return permissions when exists")
        void getPermissions_Success() throws Exception {
            // Given
            AccountEntity account = createMockAccount();
            AccountPermissionEntity permission = createMockPermission(TEST_ACCOUNT_ID);

            when(accountRepository.findByIdAndNotDeleted(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.of(account));
            when(permissionRepository.findById(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.of(permission));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                    .andExpect(jsonPath("$.tradeBuy").isBoolean())
                    .andExpect(jsonPath("$.tradeSell").isBoolean())
                    .andExpect(jsonPath("$.autoTrade").isBoolean())
                    .andExpect(jsonPath("$.manualTrade").isBoolean())
                    .andExpect(jsonPath("$.paperOnly").isBoolean());
        }

        @Test
        @DisplayName("Should return default permissions when not exists")
        void getPermissions_DefaultWhenNotExists() throws Exception {
            // Given
            AccountEntity account = createMockAccount();

            when(accountRepository.findByIdAndNotDeleted(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.of(account));
            when(permissionRepository.findById(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(TEST_ACCOUNT_ID))
                    .andExpect(jsonPath("$.tradeBuy").value(false))
                    .andExpect(jsonPath("$.tradeSell").value(false))
                    .andExpect(jsonPath("$.autoTrade").value(false))
                    .andExpect(jsonPath("$.manualTrade").value(false))
                    .andExpect(jsonPath("$.paperOnly").value(true));
        }

        @Test
        @DisplayName("Should return 404 when account not found")
        void getPermissions_AccountNotFound() throws Exception {
            // Given
            when(accountRepository.findByIdAndNotDeleted(TEST_ACCOUNT_ID))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/{accountId}/permissions", TEST_ACCOUNT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    private AccountEntity createMockAccount() {
        return AccountEntity.builder()
                .accountId(TEST_ACCOUNT_ID)
                .broker("KIS")
                .cano("50068999")
                .acntPrdtCd("01")
                .build();
    }

    private AccountPermissionEntity createMockPermission(String accountId) {
        return AccountPermissionEntity.builder()
                .accountId(accountId)
                .tradeBuy(true)
                .tradeSell(true)
                .autoTrade(false)
                .manualTrade(true)
                .paperOnly(true)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
