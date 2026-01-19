package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.StrategyCreateRequest;
import maru.trading.api.dto.request.StrategyParamsUpdateRequest;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyVersionEntity;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyVersionJpaRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Strategy Admin Controller Test
 *
 * Tests Strategy Admin API endpoints:
 * - POST   /api/v1/admin/strategies           - Create strategy
 * - GET    /api/v1/admin/strategies           - List strategies
 * - GET    /api/v1/admin/strategies/{id}      - Get strategy
 * - PUT    /api/v1/admin/strategies/{id}/params    - Update strategy params
 * - POST   /api/v1/admin/strategies/{id}/activate  - Activate strategy
 * - POST   /api/v1/admin/strategies/{id}/deactivate - Deactivate strategy
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Strategy Admin Controller Test")
class StrategyAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    @Autowired
    private StrategyVersionJpaRepository strategyVersionRepository;

    private static final String BASE_URL = "/api/v1/admin/strategies";

    @Nested
    @DisplayName("POST /api/v1/admin/strategies - Create Strategy")
    class CreateStrategy {

        @Test
        @DisplayName("Should create strategy successfully")
        void createStrategy_Success() throws Exception {
            // Given
            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", 5);
            params.put("longPeriod", 20);
            params.put("ttlSeconds", 300);

            StrategyCreateRequest request = StrategyCreateRequest.builder()
                    .name("TEST_MA_CROSS_" + System.currentTimeMillis())
                    .description("Test MA Crossover Strategy")
                    .mode(Environment.PAPER)
                    .params(params)
                    .build();

            // When & Then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.strategyId").exists())
                    .andExpect(jsonPath("$.name").value(request.getName()))
                    .andExpect(jsonPath("$.description").value(request.getDescription()))
                    .andExpect(jsonPath("$.status").value("INACTIVE"))
                    .andExpect(jsonPath("$.mode").value("PAPER"))
                    .andExpect(jsonPath("$.activeVersionId").exists())
                    .andExpect(jsonPath("$.params.shortPeriod").value(5))
                    .andExpect(jsonPath("$.params.longPeriod").value(20));
        }

        @Test
        @DisplayName("Should reject duplicate strategy name")
        void createStrategy_DuplicateName() throws Exception {
            // Given - Create existing strategy
            String existingName = "DUPLICATE_TEST_" + System.currentTimeMillis();
            createTestStrategy(existingName);

            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", 5);
            params.put("longPeriod", 20);

            StrategyCreateRequest request = StrategyCreateRequest.builder()
                    .name(existingName)
                    .mode(Environment.PAPER)
                    .params(params)
                    .build();

            // When & Then - Returns 409 Conflict for duplicate name
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should reject request without name")
        void createStrategy_MissingName() throws Exception {
            // Given
            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", 5);

            StrategyCreateRequest request = StrategyCreateRequest.builder()
                    .mode(Environment.PAPER)
                    .params(params)
                    .build();

            // When & Then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request without mode")
        void createStrategy_MissingMode() throws Exception {
            // Given
            String json = "{\"name\": \"TEST\", \"params\": {\"shortPeriod\": 5}}";

            // When & Then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject request without params")
        void createStrategy_MissingParams() throws Exception {
            // Given
            String json = "{\"name\": \"TEST\", \"mode\": \"PAPER\"}";

            // When & Then
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/strategies - List Strategies")
    class ListStrategies {

        @Test
        @DisplayName("Should return empty list when no strategies")
        void listStrategies_Empty() throws Exception {
            // Given - Clear all strategies
            strategyVersionRepository.deleteAll();
            strategyRepository.deleteAll();

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isEmpty());
        }

        @Test
        @DisplayName("Should return all strategies")
        void listStrategies_Success() throws Exception {
            // Given
            createTestStrategy("LIST_TEST_1_" + System.currentTimeMillis());
            createTestStrategy("LIST_TEST_2_" + System.currentTimeMillis());

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/strategies/{strategyId} - Get Strategy")
    class GetStrategy {

        @Test
        @DisplayName("Should return strategy by ID")
        void getStrategy_Success() throws Exception {
            // Given
            String strategyId = createTestStrategy("GET_TEST_" + System.currentTimeMillis());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/" + strategyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategyId").value(strategyId))
                    .andExpect(jsonPath("$.params").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent strategy")
        void getStrategy_NotFound() throws Exception {
            // Given
            String nonExistentId = UlidGenerator.generate();

            // When & Then
            mockMvc.perform(get(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/strategies/{strategyId}/params - Update Strategy Params")
    class UpdateStrategyParams {

        @Test
        @DisplayName("Should update strategy params and create new version")
        void updateParams_Success() throws Exception {
            // Given
            String strategyId = createTestStrategy("UPDATE_TEST_" + System.currentTimeMillis());
            StrategyEntity beforeUpdate = strategyRepository.findById(strategyId).orElseThrow();
            String originalVersionId = beforeUpdate.getActiveVersionId();

            Map<String, Object> newParams = new HashMap<>();
            newParams.put("shortPeriod", 7);
            newParams.put("longPeriod", 25);
            newParams.put("ttlSeconds", 600);

            StrategyParamsUpdateRequest request = StrategyParamsUpdateRequest.builder()
                    .params(newParams)
                    .build();

            // When & Then
            mockMvc.perform(put(BASE_URL + "/" + strategyId + "/params")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategyId").value(strategyId))
                    .andExpect(jsonPath("$.params.shortPeriod").value(7))
                    .andExpect(jsonPath("$.params.longPeriod").value(25))
                    .andExpect(jsonPath("$.params.ttlSeconds").value(600))
                    .andExpect(jsonPath("$.activeVersionId").exists());

            // Verify new version created
            StrategyEntity afterUpdate = strategyRepository.findById(strategyId).orElseThrow();
            assertThat(afterUpdate.getActiveVersionId()).isNotEqualTo(originalVersionId);

            // Verify version number incremented
            Optional<Integer> maxVersion = strategyVersionRepository.findMaxVersionNoByStrategyId(strategyId);
            assertThat(maxVersion).isPresent();
            assertThat(maxVersion.get()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should return 404 for non-existent strategy")
        void updateParams_NotFound() throws Exception {
            // Given
            String nonExistentId = UlidGenerator.generate();
            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", 7);

            StrategyParamsUpdateRequest request = StrategyParamsUpdateRequest.builder()
                    .params(params)
                    .build();

            // When & Then
            mockMvc.perform(put(BASE_URL + "/" + nonExistentId + "/params")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject request without params")
        void updateParams_MissingParams() throws Exception {
            // Given
            String strategyId = createTestStrategy("UPDATE_INVALID_" + System.currentTimeMillis());
            String json = "{}";

            // When & Then
            mockMvc.perform(put(BASE_URL + "/" + strategyId + "/params")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/strategies/{strategyId}/activate - Activate Strategy")
    class ActivateStrategy {

        @Test
        @DisplayName("Should activate inactive strategy")
        void activateStrategy_Success() throws Exception {
            // Given
            String strategyId = createTestStrategy("ACTIVATE_TEST_" + System.currentTimeMillis());

            // Verify initially INACTIVE
            StrategyEntity before = strategyRepository.findById(strategyId).orElseThrow();
            assertThat(before.getStatus()).isEqualTo("INACTIVE");

            // When & Then
            mockMvc.perform(post(BASE_URL + "/" + strategyId + "/activate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategyId").value(strategyId))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            // Verify state changed
            StrategyEntity after = strategyRepository.findById(strategyId).orElseThrow();
            assertThat(after.getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should return 404 for non-existent strategy")
        void activateStrategy_NotFound() throws Exception {
            // Given
            String nonExistentId = UlidGenerator.generate();

            // When & Then
            mockMvc.perform(post(BASE_URL + "/" + nonExistentId + "/activate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/strategies/{strategyId}/deactivate - Deactivate Strategy")
    class DeactivateStrategy {

        @Test
        @DisplayName("Should deactivate active strategy")
        void deactivateStrategy_Success() throws Exception {
            // Given - Create and activate strategy
            String strategyId = createTestStrategy("DEACTIVATE_TEST_" + System.currentTimeMillis());

            // Activate first
            mockMvc.perform(post(BASE_URL + "/" + strategyId + "/activate"))
                    .andExpect(status().isOk());

            StrategyEntity activated = strategyRepository.findById(strategyId).orElseThrow();
            assertThat(activated.getStatus()).isEqualTo("ACTIVE");

            // When & Then - Deactivate
            mockMvc.perform(post(BASE_URL + "/" + strategyId + "/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.strategyId").value(strategyId))
                    .andExpect(jsonPath("$.status").value("INACTIVE"));

            // Verify state changed
            StrategyEntity deactivated = strategyRepository.findById(strategyId).orElseThrow();
            assertThat(deactivated.getStatus()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("Should return 404 for non-existent strategy")
        void deactivateStrategy_NotFound() throws Exception {
            // Given
            String nonExistentId = UlidGenerator.generate();

            // When & Then
            mockMvc.perform(post(BASE_URL + "/" + nonExistentId + "/deactivate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/strategies/{strategyId} - Delete Strategy (Soft Delete)")
    class DeleteStrategy {

        @Test
        @DisplayName("Should soft delete strategy successfully")
        void deleteStrategy_Success() throws Exception {
            // Given
            String strategyId = createTestStrategy("DELETE_TEST_" + System.currentTimeMillis());

            // Verify strategy exists
            mockMvc.perform(get(BASE_URL + "/" + strategyId))
                    .andExpect(status().isOk());

            // When - Delete strategy
            mockMvc.perform(delete(BASE_URL + "/" + strategyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.strategyId").value(strategyId));

            // Then - Strategy should not be found (soft deleted)
            mockMvc.perform(get(BASE_URL + "/" + strategyId))
                    .andExpect(status().isNotFound());

            // Verify delyn='Y' in database
            StrategyEntity deleted = strategyRepository.findById(strategyId).orElseThrow();
            assertThat(deleted.getDelyn()).isEqualTo("Y");
        }

        @Test
        @DisplayName("Should return 404 for non-existent strategy")
        void deleteStrategy_NotFound() throws Exception {
            // Given
            String nonExistentId = UlidGenerator.generate();

            // When & Then
            mockMvc.perform(delete(BASE_URL + "/" + nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should allow creating strategy with same name after deletion")
        void deleteStrategy_AllowSameNameAfterDeletion() throws Exception {
            // Given - Create and delete a strategy
            String strategyName = "REUSABLE_NAME_" + System.currentTimeMillis();
            String strategyId = createTestStrategy(strategyName);

            // Delete the strategy
            mockMvc.perform(delete(BASE_URL + "/" + strategyId))
                    .andExpect(status().isOk());

            // When - Create new strategy with same name
            Map<String, Object> params = new HashMap<>();
            params.put("shortPeriod", 5);
            params.put("longPeriod", 20);

            StrategyCreateRequest request = StrategyCreateRequest.builder()
                    .name(strategyName)
                    .description("New strategy with reused name")
                    .mode(Environment.PAPER)
                    .params(params)
                    .build();

            // Then - Should succeed
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(strategyName));
        }

        @Test
        @DisplayName("Should not return deleted strategies in list")
        void deleteStrategy_NotInList() throws Exception {
            // Given - Create and delete a strategy
            String strategyId = createTestStrategy("DELETE_LIST_TEST_" + System.currentTimeMillis());

            // Delete the strategy
            mockMvc.perform(delete(BASE_URL + "/" + strategyId))
                    .andExpect(status().isOk());

            // When & Then - Deleted strategy should not appear in list
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[?(@.strategyId=='" + strategyId + "')]").doesNotExist());
        }

        @Test
        @DisplayName("Should return 404 when trying to delete already deleted strategy")
        void deleteStrategy_AlreadyDeleted() throws Exception {
            // Given - Create and delete a strategy
            String strategyId = createTestStrategy("DOUBLE_DELETE_TEST_" + System.currentTimeMillis());

            mockMvc.perform(delete(BASE_URL + "/" + strategyId))
                    .andExpect(status().isOk());

            // When & Then - Try to delete again
            mockMvc.perform(delete(BASE_URL + "/" + strategyId))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Create a test strategy and return its ID.
     */
    private String createTestStrategy(String name) {
        String strategyId = UlidGenerator.generate();
        String versionId = UlidGenerator.generate();

        StrategyEntity strategy = StrategyEntity.builder()
                .strategyId(strategyId)
                .name(name)
                .description("Test strategy: " + name)
                .status("INACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(versionId)
                .build();
        strategyRepository.save(strategy);

        StrategyVersionEntity version = StrategyVersionEntity.builder()
                .strategyVersionId(versionId)
                .strategyId(strategyId)
                .versionNo(1)
                .paramsJson("{\"shortPeriod\":5,\"longPeriod\":20,\"ttlSeconds\":300}")
                .build();
        strategyVersionRepository.save(version);

        return strategyId;
    }
}
