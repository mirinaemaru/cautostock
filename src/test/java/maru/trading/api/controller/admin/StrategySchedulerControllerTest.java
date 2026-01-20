package maru.trading.api.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.SchedulerTriggerRequest;
import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.application.scheduler.StrategyScheduler;
import maru.trading.domain.shared.Environment;
import maru.trading.domain.strategy.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for StrategySchedulerController.
 */
@WebMvcTest(StrategySchedulerController.class)
@DisplayName("StrategySchedulerController Unit Tests")
class StrategySchedulerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StrategyScheduler strategyScheduler;

    @MockBean
    private StrategyRepository strategyRepository;

    private StrategyScheduler.SchedulerStatus defaultStatus;

    @BeforeEach
    void setUp() {
        defaultStatus = new StrategyScheduler.SchedulerStatus(
                true,
                LocalDateTime.now(),
                10,
                9,
                1
        );
    }

    @Nested
    @DisplayName("GET /api/v1/admin/scheduler/status")
    class GetStatusTests {

        @Test
        @DisplayName("Should return scheduler status when enabled")
        void shouldReturnStatusWhenEnabled() throws Exception {
            // Given
            given(strategyScheduler.getStatus()).willReturn(defaultStatus);
            given(strategyRepository.findActiveStrategies()).willReturn(List.of(
                    Strategy.builder().strategyId("STR_001").name("Test").status("ACTIVE")
                            .mode(Environment.PAPER).activeVersionId("VER_001").build()
            ));

            // When & Then
            mockMvc.perform(get("/api/v1/admin/scheduler/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(true))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.cronExpression").value("0 * * * * *"))
                    .andExpect(jsonPath("$.executionCount").value(10))
                    .andExpect(jsonPath("$.successCount").value(9))
                    .andExpect(jsonPath("$.errorCount").value(1))
                    .andExpect(jsonPath("$.activeStrategyCount").value(1));
        }

        @Test
        @DisplayName("Should return status when scheduler is disabled")
        void shouldReturnStatusWhenDisabled() throws Exception {
            // Given
            StrategyScheduler.SchedulerStatus disabledStatus = new StrategyScheduler.SchedulerStatus(
                    false, LocalDateTime.now(), 5, 5, 0
            );
            given(strategyScheduler.getStatus()).willReturn(disabledStatus);
            given(strategyRepository.findActiveStrategies()).willReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/v1/admin/scheduler/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled").value(false))
                    .andExpect(jsonPath("$.message").value("Scheduler is paused (disabled at runtime)"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/scheduler/enable")
    class EnableSchedulerTests {

        @Test
        @DisplayName("Should enable scheduler successfully")
        void shouldEnableScheduler() throws Exception {
            // Given
            given(strategyScheduler.isEnabled()).willReturn(false);

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/enable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Scheduler enabled successfully"));

            verify(strategyScheduler).setEnabled(true);
        }

        @Test
        @DisplayName("Should indicate when scheduler was already enabled")
        void shouldIndicateAlreadyEnabled() throws Exception {
            // Given
            given(strategyScheduler.isEnabled()).willReturn(true);

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/enable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Scheduler was already enabled"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/scheduler/disable")
    class DisableSchedulerTests {

        @Test
        @DisplayName("Should disable scheduler successfully")
        void shouldDisableScheduler() throws Exception {
            // Given
            given(strategyScheduler.isEnabled()).willReturn(true);

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/disable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Scheduler disabled successfully"));

            verify(strategyScheduler).setEnabled(false);
        }

        @Test
        @DisplayName("Should indicate when scheduler was already disabled")
        void shouldIndicateAlreadyDisabled() throws Exception {
            // Given
            given(strategyScheduler.isEnabled()).willReturn(false);

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/disable"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Scheduler was already disabled"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/scheduler/trigger")
    class TriggerStrategyTests {

        @Test
        @DisplayName("Should trigger strategy manually")
        void shouldTriggerStrategyManually() throws Exception {
            // Given
            SchedulerTriggerRequest request = SchedulerTriggerRequest.builder()
                    .strategyId("STR_001")
                    .symbol("005930")
                    .accountId("ACC_001")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Strategy STR_001 triggered successfully for symbol 005930"));

            verify(strategyScheduler).triggerManually("STR_001", "005930", "ACC_001");
        }

        @Test
        @DisplayName("Should return 400 for missing strategyId")
        void shouldReturn400ForMissingStrategyId() throws Exception {
            // Given
            SchedulerTriggerRequest request = SchedulerTriggerRequest.builder()
                    .symbol("005930")
                    .accountId("ACC_001")
                    .build();

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid trigger request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given
            SchedulerTriggerRequest request = SchedulerTriggerRequest.builder()
                    .strategyId("STR_INVALID")
                    .symbol("005930")
                    .accountId("ACC_001")
                    .build();

            doThrow(new IllegalArgumentException("Strategy not found"))
                    .when(strategyScheduler).triggerManually(anyString(), anyString(), anyString());

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/trigger")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.message").value("Strategy not found"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/scheduler/execute-all")
    class ExecuteAllStrategiesTests {

        @Test
        @DisplayName("Should execute all strategies when enabled")
        void shouldExecuteAllStrategiesWhenEnabled() throws Exception {
            // Given
            given(strategyScheduler.isEnabled()).willReturn(true);
            given(strategyRepository.findActiveStrategies()).willReturn(List.of(
                    Strategy.builder().strategyId("STR_001").name("Test1").status("ACTIVE")
                            .mode(Environment.PAPER).activeVersionId("VER_001").build(),
                    Strategy.builder().strategyId("STR_002").name("Test2").status("ACTIVE")
                            .mode(Environment.PAPER).activeVersionId("VER_002").build()
            ));

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/execute-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Executed 2 active strategies"));

            verify(strategyScheduler).executeStrategies();
        }

        @Test
        @DisplayName("Should temporarily enable scheduler if disabled for execution")
        void shouldTemporarilyEnableIfDisabled() throws Exception {
            // Given
            given(strategyScheduler.isEnabled()).willReturn(false);
            given(strategyRepository.findActiveStrategies()).willReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/execute-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true));

            // Verify enable, execute, then disable
            verify(strategyScheduler).setEnabled(true);
            verify(strategyScheduler).executeStrategies();
            verify(strategyScheduler).setEnabled(false);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/scheduler/reset-stats")
    class ResetStatisticsTests {

        @Test
        @DisplayName("Should reset scheduler statistics")
        void shouldResetStatistics() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/admin/scheduler/reset-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.message").value("Scheduler statistics reset successfully"));

            verify(strategyScheduler).resetStatistics();
        }
    }
}
