package maru.trading.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Controller Integration Test.
 *
 * Tests REST API endpoints to ensure they respond correctly.
 * Focuses on health check endpoints that are critical for
 * monitoring and deployment readiness.
 *
 * Note: Query API tests require additional repository implementations
 * and will be added as Phase 3 components are completed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("API Controller Integration Test")
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ==================== Health Check Tests ====================

    @Test
    @DisplayName("Health check endpoint should return OK")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Health check should include timestamp")
    void testHealthCheckWithTimestamp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Health check should be accessible without authentication")
    void testHealthCheckAccessibility() throws Exception {
        // Health endpoint should always be accessible for monitoring
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }
}
