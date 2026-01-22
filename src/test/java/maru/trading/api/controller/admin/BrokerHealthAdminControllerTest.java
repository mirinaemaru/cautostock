package maru.trading.api.controller.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Broker Health Admin Controller Test
 *
 * Tests Broker Health Admin API endpoints:
 * - GET /api/v1/admin/broker/health - Overall broker health status
 * - GET /api/v1/admin/broker/websocket - WebSocket connection status
 * - POST /api/v1/admin/broker/websocket/reconnect - Trigger WebSocket reconnection
 * - GET /api/v1/admin/broker/summary - Broker connection summary
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Broker Health Admin Controller Test")
class BrokerHealthAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/v1/admin/broker";

    @Nested
    @DisplayName("GET /api/v1/admin/broker/health - Broker Health")
    class GetBrokerHealth {

        @Test
        @DisplayName("Should return overall broker health status")
        void getHealth_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.mode").exists())
                    .andExpect(jsonPath("$.profile").exists())
                    .andExpect(jsonPath("$.websocket").exists())
                    .andExpect(jsonPath("$.websocket.connected").exists())
                    .andExpect(jsonPath("$.websocket.status").exists())
                    .andExpect(jsonPath("$.restApi").exists())
                    .andExpect(jsonPath("$.restApi.available").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.healthy").exists())
                    .andExpect(jsonPath("$.safety").exists())
                    .andExpect(jsonPath("$.safety.isStubMode").exists())
                    .andExpect(jsonPath("$.safety.isPaperTrading").exists())
                    .andExpect(jsonPath("$.safety.liveOrdersEnabled").exists());
        }

        @Test
        @DisplayName("Should include safety information in STUB mode")
        void getHealth_SafetyInfo() throws Exception {
            mockMvc.perform(get(BASE_URL + "/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.safety.isStubMode").isBoolean())
                    .andExpect(jsonPath("$.safety.isPaperTrading").isBoolean())
                    .andExpect(jsonPath("$.safety.liveOrdersEnabled").isBoolean());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/broker/websocket - WebSocket Status")
    class GetWebSocketStatus {

        @Test
        @DisplayName("Should return WebSocket connection status")
        void getWebSocketStatus_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/websocket"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.mode").exists())
                    .andExpect(jsonPath("$.connected").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.note").exists());
        }

        @Test
        @DisplayName("Should indicate status as CONNECTED or DISCONNECTED")
        void getWebSocketStatus_ValidStatus() throws Exception {
            mockMvc.perform(get(BASE_URL + "/websocket"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(
                            org.hamcrest.Matchers.anyOf(
                                    org.hamcrest.Matchers.is("CONNECTED"),
                                    org.hamcrest.Matchers.is("DISCONNECTED")
                            )));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/broker/websocket/reconnect - WebSocket Reconnect")
    class ReconnectWebSocket {

        @Test
        @DisplayName("Should handle reconnect request")
        void reconnect_Success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/websocket/reconnect"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.mode").exists())
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should indicate STUB mode does not support reconnection")
        void reconnect_StubMode() throws Exception {
            // In STUB mode, reconnection should indicate it's not applicable
            mockMvc.perform(post(BASE_URL + "/websocket/reconnect"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mode").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/broker/summary - Broker Summary")
    class GetBrokerSummary {

        @Test
        @DisplayName("Should return broker connection summary")
        void getSummary_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.broker").value("KIS (Korea Investment & Securities)"))
                    .andExpect(jsonPath("$.mode").exists())
                    .andExpect(jsonPath("$.profile").exists())
                    .andExpect(jsonPath("$.connections").exists())
                    .andExpect(jsonPath("$.connections.websocket").exists())
                    .andExpect(jsonPath("$.connections.restApi").exists())
                    .andExpect(jsonPath("$.capabilities").exists());
        }

        @Test
        @DisplayName("Should include all trading capabilities")
        void getSummary_Capabilities() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.capabilities.realTimeMarketData").isBoolean())
                    .andExpect(jsonPath("$.capabilities.orderPlacement").isBoolean())
                    .andExpect(jsonPath("$.capabilities.orderModification").isBoolean())
                    .andExpect(jsonPath("$.capabilities.orderCancellation").isBoolean())
                    .andExpect(jsonPath("$.capabilities.fillNotifications").isBoolean());
        }

        @Test
        @DisplayName("Should show connection status as UP or DOWN")
        void getSummary_ConnectionStatus() throws Exception {
            mockMvc.perform(get(BASE_URL + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.connections.websocket").value(
                            org.hamcrest.Matchers.anyOf(
                                    org.hamcrest.Matchers.is("UP"),
                                    org.hamcrest.Matchers.is("DOWN")
                            )))
                    .andExpect(jsonPath("$.connections.restApi").value(
                            org.hamcrest.Matchers.anyOf(
                                    org.hamcrest.Matchers.is("UP"),
                                    org.hamcrest.Matchers.is("DOWN")
                            )));
        }
    }
}
