package maru.trading.api.controller.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Health Controller Test
 *
 * Tests Health Check API endpoints:
 * - GET /health - Basic health check
 * - GET /health/details - Detailed health check
 * - GET /health/db - Database health
 * - GET /health/api - API health
 * - GET /health/metrics - JVM metrics
 * - GET /health/info - Application info
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Health Controller Test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("GET /health - Basic Health Check")
    class BasicHealth {

        @Test
        @DisplayName("Should return UP status")
        void health_Success() throws Exception {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.components").exists())
                    .andExpect(jsonPath("$.components.db").exists())
                    .andExpect(jsonPath("$.components.kisRest").exists())
                    .andExpect(jsonPath("$.components.kisWs").exists())
                    .andExpect(jsonPath("$.components.token").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/details - Detailed Health Check")
    class DetailedHealth {

        @Test
        @DisplayName("Should return detailed status")
        void healthDetails_Success() throws Exception {
            mockMvc.perform(get("/health/details"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.components.db").exists())
                    .andExpect(jsonPath("$.components.db.status").value("UP"))
                    .andExpect(jsonPath("$.components.db.database").exists())
                    .andExpect(jsonPath("$.components.api").exists())
                    .andExpect(jsonPath("$.components.api.status").value("UP"))
                    .andExpect(jsonPath("$.components.cache").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/db - Database Health")
    class DatabaseHealth {

        @Test
        @DisplayName("Should return database health status")
        void healthDb_Success() throws Exception {
            mockMvc.perform(get("/health/db"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.database").value("MySQL"))
                    .andExpect(jsonPath("$.connectionPool").value("HikariCP"))
                    .andExpect(jsonPath("$.activeConnections").exists())
                    .andExpect(jsonPath("$.maxConnections").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/api - API Health")
    class ApiHealth {

        @Test
        @DisplayName("Should return API health status")
        void healthApi_Success() throws Exception {
            mockMvc.perform(get("/health/api"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.kisApi").exists())
                    .andExpect(jsonPath("$.kisApi.rest").value("UP"))
                    .andExpect(jsonPath("$.kisApi.webSocket").value("UP"))
                    .andExpect(jsonPath("$.kisApi.tokenStatus").value("VALID"));
        }
    }

    @Nested
    @DisplayName("GET /health/metrics - JVM Metrics")
    class JvmMetrics {

        @Test
        @DisplayName("Should return JVM metrics")
        void healthMetrics_Success() throws Exception {
            mockMvc.perform(get("/health/metrics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.jvm").exists())
                    .andExpect(jsonPath("$.jvm.uptime").exists())
                    .andExpect(jsonPath("$.jvm.heapMemoryUsed").exists())
                    .andExpect(jsonPath("$.jvm.heapMemoryMax").exists())
                    .andExpect(jsonPath("$.system").exists())
                    .andExpect(jsonPath("$.system.availableProcessors").exists())
                    .andExpect(jsonPath("$.system.freeMemory").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/info - Application Info")
    class AppInfo {

        @Test
        @DisplayName("Should return application info")
        void healthInfo_Success() throws Exception {
            mockMvc.perform(get("/health/info"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.app").exists())
                    .andExpect(jsonPath("$.app.name").value("cautostock"))
                    .andExpect(jsonPath("$.app.version").exists())
                    .andExpect(jsonPath("$.app.description").exists())
                    .andExpect(jsonPath("$.build").exists())
                    .andExpect(jsonPath("$.build.java").exists());
        }
    }
}
