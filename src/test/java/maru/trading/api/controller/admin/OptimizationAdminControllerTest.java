package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Optimization Admin Controller Test
 *
 * Tests Optimization Admin API endpoints:
 * - GET /api/v1/admin/optimization - List all optimizations
 * - POST /api/v1/admin/optimization - Create optimization
 * - GET /api/v1/admin/optimization/{id} - Get optimization details
 * - POST /api/v1/admin/optimization/run - Run optimization
 * - GET /api/v1/admin/optimization/methods - Get available optimization methods
 * - DELETE /api/v1/admin/optimization/{id} - Delete optimization
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Optimization Admin Controller Test")
class OptimizationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/admin/optimization";

    @Nested
    @DisplayName("GET /api/v1/admin/optimization - List Optimizations")
    class ListOptimizations {

        @Test
        @DisplayName("Should return all optimizations")
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
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("Should filter by strategyId")
        void filterByStrategyId_Success() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("strategyId", "MA_CROSS_5_20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/optimization - Create Optimization")
    class CreateOptimization {

        @Test
        @DisplayName("Should create new optimization")
        void createOptimization_Success() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", "MA_CROSS_5_20");
            request.put("method", "GRID_SEARCH");
            request.put("parameters", Map.of(
                    "stopLoss", Map.of("min", 1, "max", 10, "step", 1),
                    "takeProfit", Map.of("min", 2, "max", 20, "step", 2)
            ));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optimizationId").exists())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.strategyId").value("MA_CROSS_5_20"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/optimization/{id} - Get Optimization")
    class GetOptimization {

        @Test
        @DisplayName("Should return optimization by ID")
        void getOptimization_Success() throws Exception {
            // First create an optimization
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", "MA_CROSS_5_20");
            request.put("method", "GRID_SEARCH");

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            String optimizationId = (String) response.get("optimizationId");

            // Then get it
            mockMvc.perform(get(BASE_URL + "/" + optimizationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optimizationId").value(optimizationId));
        }

        @Test
        @DisplayName("Should return 404 for non-existent optimization")
        void getOptimization_NotFound() throws Exception {
            mockMvc.perform(get(BASE_URL + "/OPT-99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/optimization/run - Run Optimization")
    class RunOptimization {

        @Test
        @DisplayName("Should run optimization")
        void runOptimization_Success() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", "MA_CROSS_5_20");
            request.put("method", "GRID_SEARCH");

            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optimizationId").exists())
                    .andExpect(jsonPath("$.status").value("RUNNING"))
                    .andExpect(jsonPath("$.progress").value(0))
                    .andExpect(jsonPath("$.parameters").exists());
        }

        @Test
        @DisplayName("Should run optimization with default method")
        void runOptimizationDefaultMethod_Success() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", "RSI_STRATEGY");

            mockMvc.perform(post(BASE_URL + "/run")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.method").value("GRID_SEARCH"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/optimization/methods - Get Methods")
    class GetMethods {

        @Test
        @DisplayName("Should return available optimization methods")
        void getMethods_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/methods"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.methods").isArray())
                    .andExpect(jsonPath("$.total").isNumber());
        }

        @Test
        @DisplayName("Should include all optimization method details")
        void getMethods_IncludesDetails() throws Exception {
            mockMvc.perform(get(BASE_URL + "/methods"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.methods[0].id").exists())
                    .andExpect(jsonPath("$.methods[0].name").exists())
                    .andExpect(jsonPath("$.methods[0].description").exists())
                    .andExpect(jsonPath("$.methods[0].complexity").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/optimization/{id}/status - Get Status")
    class GetStatus {

        @Test
        @DisplayName("Should return optimization status")
        void getStatus_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/OPT-12345/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optimizationId").value("OPT-12345"))
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.progress").isNumber());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/optimization/{id}/apply - Apply Result")
    class ApplyResult {

        @Test
        @DisplayName("Should apply optimization result")
        void applyResult_Success() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", "MA_CROSS_5_20");

            mockMvc.perform(post(BASE_URL + "/OPT-12345/apply")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.applied").value(true))
                    .andExpect(jsonPath("$.appliedParameters").exists())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/optimization/{id} - Delete Optimization")
    class DeleteOptimization {

        @Test
        @DisplayName("Should delete optimization")
        void deleteOptimization_Success() throws Exception {
            // First create an optimization
            Map<String, Object> request = new HashMap<>();
            request.put("strategyId", "MA_CROSS_5_20");

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseJson, Map.class);
            String optimizationId = (String) response.get("optimizationId");

            // Then delete it
            mockMvc.perform(delete(BASE_URL + "/" + optimizationId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 for non-existent optimization")
        void deleteOptimization_NotFound() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/OPT-99999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/optimization/{id}/results - Get Results")
    class GetResults {

        @Test
        @DisplayName("Should return optimization results")
        void getResults_Success() throws Exception {
            mockMvc.perform(get(BASE_URL + "/OPT-12345/results"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.optimizationId").value("OPT-12345"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.bestParameters").exists())
                    .andExpect(jsonPath("$.metrics").exists())
                    .andExpect(jsonPath("$.totalIterations").exists());
        }
    }
}
