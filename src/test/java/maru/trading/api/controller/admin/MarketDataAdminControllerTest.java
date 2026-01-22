package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.AddSymbolsRequest;
import maru.trading.api.dto.request.RemoveSymbolsRequest;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Market Data Admin Controller Test
 *
 * Tests Market Data Admin API endpoints:
 * - POST /api/v1/admin/market-data/symbols - Add symbols to subscription
 * - DELETE /api/v1/admin/market-data/symbols - Remove symbols from subscription
 * - GET /api/v1/admin/market-data/symbols - Get subscribed symbols
 * - POST /api/v1/admin/market-data/resubscribe - Resubscribe to market data
 * - GET /api/v1/admin/market-data/status - Get subscription status
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Market Data Admin Controller Test")
class MarketDataAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/admin/market-data";

    @Nested
    @DisplayName("POST /api/v1/admin/market-data/symbols - Add Symbols")
    class AddSymbols {

        @Test
        @DisplayName("Should add symbols to subscription")
        void addSymbols_Success() throws Exception {
            AddSymbolsRequest request = AddSymbolsRequest.builder()
                    .symbols(List.of("005490", "000270"))
                    .build();

            mockMvc.perform(post(BASE_URL + "/symbols")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return error for blank symbol")
        void addSymbols_BlankSymbol() throws Exception {
            AddSymbolsRequest request = AddSymbolsRequest.builder()
                    .symbols(List.of("005490", ""))
                    .build();

            mockMvc.perform(post(BASE_URL + "/symbols")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.ok").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/market-data/symbols - Remove Symbols")
    class RemoveSymbols {

        @Test
        @DisplayName("Should remove symbols from subscription")
        void removeSymbols_Success() throws Exception {
            // First add some symbols
            AddSymbolsRequest addRequest = AddSymbolsRequest.builder()
                    .symbols(List.of("005490", "000270", "035720"))
                    .build();
            mockMvc.perform(post(BASE_URL + "/symbols")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(addRequest)))
                    .andExpect(status().isOk());

            // Then remove one
            RemoveSymbolsRequest removeRequest = RemoveSymbolsRequest.builder()
                    .symbols(List.of("005490"))
                    .build();

            mockMvc.perform(delete(BASE_URL + "/symbols")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(removeRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));
        }

        @Test
        @DisplayName("Should return error for blank symbol")
        void removeSymbols_BlankSymbol() throws Exception {
            RemoveSymbolsRequest request = RemoveSymbolsRequest.builder()
                    .symbols(List.of(""))
                    .build();

            mockMvc.perform(delete(BASE_URL + "/symbols")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.ok").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/market-data/symbols - Get Subscribed Symbols")
    class GetSubscribedSymbols {

        @Test
        @DisplayName("Should return subscribed symbols")
        void getSymbols_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/symbols"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.symbols").isArray())
                    .andExpect(jsonPath("$.total").isNumber())
                    .andExpect(jsonPath("$.active").isBoolean());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/market-data/resubscribe - Resubscribe")
    class Resubscribe {

        @Test
        @DisplayName("Should resubscribe to market data")
        void resubscribe_Success() throws Exception {
            mockMvc.perform(post(BASE_URL + "/resubscribe"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/market-data/status - Get Status")
    class GetStatus {

        @Test
        @DisplayName("Should return subscription status")
        void getStatus_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subscribed").isBoolean())
                    .andExpect(jsonPath("$.symbolCount").isNumber())
                    .andExpect(jsonPath("$.connected").isBoolean())
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
