package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.KillSwitchToggleRequest;
import maru.trading.domain.risk.KillSwitchStatus;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kill Switch Admin Controller Test
 *
 * Tests Kill Switch Admin API endpoints:
 * - GET /api/v1/admin/kill-switch - Get kill switch state
 * - POST /api/v1/admin/kill-switch - Toggle kill switch
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Kill Switch Admin Controller Test")
class KillSwitchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RiskStateJpaRepository riskStateRepository;

    private static final String BASE_URL = "/api/v1/admin/kill-switch";
    private String testAccountId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        createTestRiskState();
    }

    @Nested
    @DisplayName("GET /api/v1/admin/kill-switch - Get Kill Switch State")
    class GetKillSwitchState {

        @Test
        @DisplayName("Should return global kill switch state")
        void getState_Global() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should return account-specific kill switch state")
        void getState_ByAccountId() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("accountId", testAccountId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @DisplayName("Should return default OFF state for non-existent account")
        void getState_NonExistentAccount() throws Exception {
            String nonExistentId = UlidGenerator.generate();

            mockMvc.perform(get(BASE_URL)
                            .param("accountId", nonExistentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("OFF"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/kill-switch - Toggle Kill Switch")
    class ToggleKillSwitch {

        @Test
        @DisplayName("Should toggle kill switch to ON")
        void toggle_ToOn() throws Exception {
            KillSwitchToggleRequest request = KillSwitchToggleRequest.builder()
                    .accountId(testAccountId)
                    .status(KillSwitchStatus.ON)
                    .reason("Emergency stop - manual trigger")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(testAccountId))
                    .andExpect(jsonPath("$.status").value("ON"))
                    .andExpect(jsonPath("$.reason").value("Emergency stop - manual trigger"))
                    .andExpect(jsonPath("$.updatedAt").exists());
        }

        @Test
        @DisplayName("Should toggle kill switch to ARMED")
        void toggle_ToArmed() throws Exception {
            KillSwitchToggleRequest request = KillSwitchToggleRequest.builder()
                    .accountId(testAccountId)
                    .status(KillSwitchStatus.ARMED)
                    .reason("Risk threshold approaching")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARMED"));
        }

        @Test
        @DisplayName("Should toggle global kill switch when no accountId")
        void toggle_Global() throws Exception {
            KillSwitchToggleRequest request = KillSwitchToggleRequest.builder()
                    .accountId(null)
                    .status(KillSwitchStatus.ON)
                    .reason("Global trading halt")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ON"));
        }

        @Test
        @DisplayName("Should return 400 for missing status")
        void toggle_MissingStatus() throws Exception {
            String invalidRequest = "{\"accountId\": \"" + testAccountId + "\", \"reason\": \"test\"}";

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for missing reason")
        void toggle_MissingReason() throws Exception {
            String invalidRequest = "{\"accountId\": \"" + testAccountId + "\", \"status\": \"ON\"}";

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Helper Methods ====================

    private void createTestRiskState() {
        RiskStateEntity riskState = RiskStateEntity.builder()
                .riskStateId(UlidGenerator.generate())
                .scope("ACCOUNT")
                .accountId(testAccountId)
                .killSwitchStatus(KillSwitchStatus.OFF)
                .build();
        riskStateRepository.save(riskState);
    }
}
