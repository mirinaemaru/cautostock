package maru.trading.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.TestFixtures;
import maru.trading.api.dto.request.DemoSignalRequest;
import maru.trading.api.dto.request.KillSwitchToggleRequest;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.KillSwitchStatus;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.persistence.jpa.entity.AccountEntity;
import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.repository.AccountJpaRepository;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Admin API Controller Integration Test.
 *
 * Tests Admin API endpoints:
 * - Kill Switch management
 * - Demo signal injection
 * - Health checks
 *
 * Focus areas:
 * - Request validation
 * - Response format
 * - State management
 * - Error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Admin API Controller Integration Test")
class AdminApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RiskStateJpaRepository riskStateRepository;

    @Autowired
    private AccountJpaRepository accountRepository;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String strategyId;

    @BeforeEach
    void setUp() {
        accountId = "ACC_API_001";
        strategyId = "STR_API_001";

        // 테스트용 Account 생성
        if (!accountRepository.existsById(accountId)) {
            AccountEntity account = AccountEntity.builder()
                    .accountId(accountId)
                    .broker("KIS")
                    .environment(Environment.PAPER)
                    .cano("12345678")
                    .acntPrdtCd("01")
                    .status(maru.trading.domain.account.AccountStatus.ACTIVE)
                    .alias("Test Account")
                    .delyn("N")
                    .build();
            accountRepository.save(account);
        }

        // 테스트용 Strategy 생성
        if (!strategyRepository.existsById(strategyId)) {
            StrategyEntity strategy = StrategyEntity.builder()
                    .strategyId(strategyId)
                    .name("TEST_STRATEGY_API")
                    .description("Test strategy for API tests")
                    .status("ACTIVE")
                    .mode(Environment.PAPER)
                    .activeVersionId("VER_API_001")
                    .delyn("N")
                    .build();
            strategyRepository.save(strategy);
        }

        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-API-TEST"));
    }

    // ==================== Kill Switch Admin Tests ====================

    @Test
    @DisplayName("GET /api/v1/admin/kill-switch - Should return default GLOBAL kill switch state")
    void testGetKillSwitch_Global_Default() throws Exception {
        mockMvc.perform(get("/api/v1/admin/kill-switch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFF"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/admin/kill-switch?accountId=X - Should return account-specific state")
    void testGetKillSwitch_Account() throws Exception {
        // Given - Create account-specific kill switch state
        RiskStateEntity riskState = RiskStateEntity.builder()
                .riskStateId(UlidGenerator.generate())
                .scope("ACCOUNT")
                .accountId(accountId)
                .killSwitchStatus(KillSwitchStatus.ON)
                .killSwitchReason("Test reason")
                .dailyPnl(BigDecimal.ZERO)
                .exposure(BigDecimal.ZERO)
                .openOrderCount(0)
                .consecutiveOrderFailures(0)
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        riskStateRepository.save(riskState);

        // When & Then
        mockMvc.perform(get("/api/v1/admin/kill-switch")
                        .param("accountId", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.status").value("ON"))
                .andExpect(jsonPath("$.reason").value("Test reason"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/kill-switch - Should toggle GLOBAL kill switch")
    void testToggleKillSwitch_Global() throws Exception {
        // Given
        KillSwitchToggleRequest request = KillSwitchToggleRequest.builder()
                .status(KillSwitchStatus.ON)
                .reason("Emergency stop")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/admin/kill-switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON"))
                .andExpect(jsonPath("$.reason").value("Emergency stop"));

        // Verify persistence
        RiskStateEntity saved = riskStateRepository.findFirstByScopeOrderByUpdatedAtDesc("GLOBAL")
                .orElseThrow();
        assertThat(saved.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.ON);
        assertThat(saved.getKillSwitchReason()).isEqualTo("Emergency stop");
    }

    @Test
    @DisplayName("POST /api/v1/admin/kill-switch - Should toggle account-specific kill switch")
    void testToggleKillSwitch_Account() throws Exception {
        // Given
        KillSwitchToggleRequest request = KillSwitchToggleRequest.builder()
                .accountId(accountId)
                .status(KillSwitchStatus.ON)
                .reason("Account risk exceeded")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/admin/kill-switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.status").value("ON"))
                .andExpect(jsonPath("$.reason").value("Account risk exceeded"));

        // Verify persistence
        RiskStateEntity saved = riskStateRepository.findByScopeAndAccountId("ACCOUNT", accountId)
                .orElseThrow();
        assertThat(saved.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.ON);
    }

    @Test
    @DisplayName("POST /api/v1/admin/kill-switch - Should turn OFF kill switch")
    void testToggleKillSwitch_TurnOff() throws Exception {
        // Given - Kill switch is ON
        RiskStateEntity existing = RiskStateEntity.builder()
                .riskStateId(UlidGenerator.generate())
                .scope("GLOBAL")
                .killSwitchStatus(KillSwitchStatus.ON)
                .killSwitchReason("Previous emergency")
                .dailyPnl(BigDecimal.ZERO)
                .exposure(BigDecimal.ZERO)
                .openOrderCount(0)
                .consecutiveOrderFailures(0)
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        riskStateRepository.save(existing);

        KillSwitchToggleRequest request = KillSwitchToggleRequest.builder()
                .status(KillSwitchStatus.OFF)
                .reason("Emergency resolved")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/admin/kill-switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OFF"));

        // Verify persistence
        RiskStateEntity saved = riskStateRepository.findFirstByScopeOrderByUpdatedAtDesc("GLOBAL")
                .orElseThrow();
        assertThat(saved.getKillSwitchStatus()).isEqualTo(KillSwitchStatus.OFF);
    }

    // ==================== Demo Signal Controller Tests ====================

    @Test
    @DisplayName("POST /api/v1/demo/signal - Should inject BUY signal successfully")
    void testInjectSignal_Buy() throws Exception {
        // Given
        DemoSignalRequest request = DemoSignalRequest.builder()
                .accountId(accountId)
                .symbol("005930")
                .side(Side.BUY)
                .targetValue(BigDecimal.valueOf(10))
                .targetType("QTY")
                .ttlSeconds(60)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/demo/signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.message").value("Signal processed and order sent successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/demo/signal - Should inject SELL signal successfully")
    void testInjectSignal_Sell() throws Exception {
        // Given
        DemoSignalRequest request = DemoSignalRequest.builder()
                .accountId(accountId)
                .symbol("035420")
                .side(Side.SELL)
                .targetValue(BigDecimal.valueOf(5))
                .targetType("QTY")
                .ttlSeconds(120)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/demo/signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/demo/signal - Should use default TTL and targetType")
    void testInjectSignal_DefaultValues() throws Exception {
        // Given - No TTL or targetType specified
        DemoSignalRequest request = DemoSignalRequest.builder()
                .accountId(accountId)
                .symbol("005930")
                .side(Side.BUY)
                .targetValue(BigDecimal.valueOf(3))
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/demo/signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ok").value(true));
    }

    // ==================== Health Check Tests ====================

    @Test
    @DisplayName("GET /health - Should return healthy status")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ==================== Error Handling Tests ====================

    @Test
    @DisplayName("POST /api/v1/admin/kill-switch - Should reject invalid request")
    void testToggleKillSwitch_InvalidRequest() throws Exception {
        // Given - Empty request body
        String invalidJson = "{}";

        // When & Then - Should fail validation
        mockMvc.perform(post("/api/v1/admin/kill-switch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/demo/signal - Should reject invalid signal")
    void testInjectSignal_InvalidRequest() throws Exception {
        // Given - Missing required fields
        String invalidJson = "{\"symbol\": \"005930\"}";

        // When & Then - Should fail validation
        mockMvc.perform(post("/api/v1/demo/signal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
