package maru.trading.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.api.dto.request.CreateNotificationRequest;
import maru.trading.api.dto.request.NotificationSettingsRequest;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.*;
import maru.trading.infra.persistence.jpa.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test for New APIs
 *
 * Tests the complete flow of:
 * 1. Performance Analysis APIs
 * 2. Risk Analysis APIs (VaR, Correlation)
 * 3. Notification APIs
 * 4. Execution History APIs
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("New API Integration Test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    @Autowired
    private DailyPerformanceJpaRepository dailyPerformanceRepository;

    @Autowired
    private PositionJpaRepository positionRepository;

    @Autowired
    private NotificationJpaRepository notificationRepository;

    @Autowired
    private NotificationSettingsJpaRepository settingsRepository;

    @Autowired
    private ExecutionHistoryJpaRepository executionHistoryRepository;

    private String testAccountId;
    private String testStrategyId;

    @BeforeEach
    void setUp() {
        testAccountId = UlidGenerator.generate();
        testStrategyId = setupTestStrategy();
        setupTestData();
    }

    // ==================== Performance API Integration Tests ====================

    @Test
    @Order(1)
    @DisplayName("Integration: Complete performance analysis flow")
    void performanceAnalysisFlow() throws Exception {
        // 1. Get account performance
        MvcResult performanceResult = mockMvc.perform(get("/api/v1/query/performance")
                        .param("accountId", testAccountId)
                        .param("from", LocalDate.now().minusDays(30).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(testAccountId))
                .andExpect(jsonPath("$.totalPnl").exists())
                .andExpect(jsonPath("$.totalTrades").exists())
                .andExpect(jsonPath("$.winRate").exists())
                .andReturn();

        // 2. Get all strategies statistics
        mockMvc.perform(get("/api/v1/query/performance/strategies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategies").isArray())
                .andExpect(jsonPath("$.count").exists());

        // 3. Get specific strategy statistics
        mockMvc.perform(get("/api/v1/query/performance/strategies/" + testStrategyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyId").value(testStrategyId))
                .andExpect(jsonPath("$.strategyName").exists())
                .andExpect(jsonPath("$.totalTrades").exists());
    }

    // ==================== Risk Analysis API Integration Tests ====================

    @Test
    @Order(2)
    @DisplayName("Integration: Complete VaR analysis flow")
    void varAnalysisFlow() throws Exception {
        // 1. Historical VaR at 95% confidence
        mockMvc.perform(get("/api/v1/query/risk/var")
                        .param("accountId", testAccountId)
                        .param("method", "HISTORICAL")
                        .param("confidenceLevel", "95"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(testAccountId))
                .andExpect(jsonPath("$.method").value("HISTORICAL"))
                .andExpect(jsonPath("$.confidenceLevel").value(95))
                .andExpect(jsonPath("$.portfolioValue").exists())
                .andExpect(jsonPath("$.var").exists())
                .andExpect(jsonPath("$.cvar").exists());

        // 2. Parametric VaR at 99% confidence
        mockMvc.perform(get("/api/v1/query/risk/var")
                        .param("accountId", testAccountId)
                        .param("method", "PARAMETRIC")
                        .param("confidenceLevel", "99")
                        .param("holdingPeriod", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("PARAMETRIC"))
                .andExpect(jsonPath("$.confidenceLevel").value(99))
                .andExpect(jsonPath("$.holdingPeriod").value(10));
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Complete correlation analysis flow")
    void correlationAnalysisFlow() throws Exception {
        mockMvc.perform(get("/api/v1/query/risk/correlation")
                        .param("accountId", testAccountId)
                        .param("from", LocalDate.now().minusDays(90).toString())
                        .param("to", LocalDate.now().toString())
                        .param("timeframe", "daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(testAccountId))
                .andExpect(jsonPath("$.symbols").isArray())
                .andExpect(jsonPath("$.timeframe").value("daily"))
                .andExpect(jsonPath("$.portfolioAnalysis").exists());
    }

    // ==================== Notification API Integration Tests ====================

    @Test
    @Order(4)
    @DisplayName("Integration: Complete notification lifecycle")
    void notificationLifecycleFlow() throws Exception {
        // 1. Create notification
        CreateNotificationRequest createRequest = CreateNotificationRequest.builder()
                .accountId(testAccountId)
                .notificationType("TRADE")
                .severity("INFO")
                .title("Order Filled")
                .message("Buy order for 005930 has been filled")
                .refType("ORDER")
                .refId(UlidGenerator.generate())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").exists())
                .andReturn();

        String notificationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("notificationId").asText();

        // 2. List notifications
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications").isArray())
                .andExpect(jsonPath("$.unreadCount").value(org.hamcrest.Matchers.greaterThan(0)));

        // 3. Get unread count
        mockMvc.perform(get("/api/v1/admin/notifications/unread-count")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(org.hamcrest.Matchers.greaterThan(0)));

        // 4. Mark as read
        mockMvc.perform(post("/api/v1/admin/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        // 5. Verify read status
        NotificationEntity notification = notificationRepository.findById(notificationId).orElseThrow();
        assertThat(notification.isUnread()).isFalse();

        // 6. Delete notification
        mockMvc.perform(delete("/api/v1/admin/notifications/" + notificationId))
                .andExpect(status().isOk());

        assertThat(notificationRepository.existsById(notificationId)).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Notification settings management")
    void notificationSettingsFlow() throws Exception {
        // 1. Get default settings
        mockMvc.perform(get("/api/v1/admin/notifications/settings")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushEnabled").value(true))
                .andExpect(jsonPath("$.tradeAlerts").value(true));

        // 2. Update settings
        NotificationSettingsRequest updateRequest = NotificationSettingsRequest.builder()
                .emailEnabled(true)
                .emailAddress("trader@example.com")
                .pushEnabled(true)
                .tradeAlerts(true)
                .riskAlerts(true)
                .systemAlerts(false)
                .dailySummary(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/notifications/settings")
                        .param("accountId", testAccountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.emailAddress").value("trader@example.com"))
                .andExpect(jsonPath("$.systemAlerts").value(false))
                .andExpect(jsonPath("$.dailySummary").value(true));

        // 3. Verify settings persisted
        mockMvc.perform(get("/api/v1/admin/notifications/settings")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true))
                .andExpect(jsonPath("$.dailySummary").value(true));
    }

    // ==================== Execution History API Integration Tests ====================

    @Test
    @Order(6)
    @DisplayName("Integration: Complete execution history flow")
    void executionHistoryFlow() throws Exception {
        // Create test execution history
        String executionId = createTestExecution();

        // 1. List execution history
        mockMvc.perform(get("/api/v1/query/execution-history")
                        .param("strategyId", testStrategyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executions").isArray())
                .andExpect(jsonPath("$.totalCount").exists())
                .andExpect(jsonPath("$.successCount").exists())
                .andExpect(jsonPath("$.failedCount").exists());

        // 2. Get specific execution
        mockMvc.perform(get("/api/v1/query/execution-history/" + executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId))
                .andExpect(jsonPath("$.strategyId").value(testStrategyId))
                .andExpect(jsonPath("$.executionType").exists())
                .andExpect(jsonPath("$.status").exists());

        // 3. Get by strategy with filters
        mockMvc.perform(get("/api/v1/query/execution-history/strategy/" + testStrategyId)
                        .param("from", LocalDate.now().minusDays(7).toString())
                        .param("to", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executions").isArray());

        // 4. Filter by execution type
        mockMvc.perform(get("/api/v1/query/execution-history")
                        .param("strategyId", testStrategyId)
                        .param("executionType", "SIGNAL_GENERATED"))
                .andExpect(status().isOk());

        // 5. Filter by status
        mockMvc.perform(get("/api/v1/query/execution-history")
                        .param("strategyId", testStrategyId)
                        .param("status", "SUCCESS"))
                .andExpect(status().isOk());
    }

    // ==================== End-to-End Scenario Tests ====================

    @Test
    @Order(7)
    @DisplayName("Integration: Trading day scenario - performance tracking and notifications")
    void tradingDayScenario() throws Exception {
        // Simulate a trading day with performance tracking and notifications

        // 1. Check morning performance
        mockMvc.perform(get("/api/v1/query/performance")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk());

        // 2. Check risk exposure (VaR)
        mockMvc.perform(get("/api/v1/query/risk/var")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk());

        // 3. Trade execution notification
        CreateNotificationRequest tradeNotification = CreateNotificationRequest.builder()
                .accountId(testAccountId)
                .notificationType("TRADE")
                .severity("INFO")
                .title("Trade Executed")
                .message("Bought 100 shares of 005930 at 70,000 KRW")
                .build();

        mockMvc.perform(post("/api/v1/admin/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tradeNotification)))
                .andExpect(status().isCreated());

        // 4. Risk alert notification
        CreateNotificationRequest riskNotification = CreateNotificationRequest.builder()
                .accountId(testAccountId)
                .notificationType("RISK")
                .severity("WARNING")
                .title("Position Limit Warning")
                .message("Position in 005930 approaching maximum limit")
                .build();

        mockMvc.perform(post("/api/v1/admin/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(riskNotification)))
                .andExpect(status().isCreated());

        // 5. Check execution history
        mockMvc.perform(get("/api/v1/query/execution-history")
                        .param("strategyId", testStrategyId))
                .andExpect(status().isOk());

        // 6. End of day - check correlation
        mockMvc.perform(get("/api/v1/query/risk/correlation")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk());

        // 7. End of day - check notifications
        mockMvc.perform(get("/api/v1/admin/notifications")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(org.hamcrest.Matchers.greaterThan(0)));

        // 8. Mark all as read
        mockMvc.perform(post("/api/v1/admin/notifications/read-all")
                        .param("accountId", testAccountId))
                .andExpect(status().isOk());
    }

    // ==================== Helper Methods ====================

    private String setupTestStrategy() {
        String strategyId = UlidGenerator.generate();
        String versionId = UlidGenerator.generate();

        StrategyEntity strategy = StrategyEntity.builder()
                .strategyId(strategyId)
                .name("INTEGRATION_TEST_" + System.currentTimeMillis())
                .description("Integration test strategy")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(versionId)
                .build();
        strategyRepository.save(strategy);

        return strategyId;
    }

    private void setupTestData() {
        // Create positions
        String[] symbols = {"005930", "035720", "000660"};
        for (int i = 0; i < symbols.length; i++) {
            PositionEntity position = PositionEntity.builder()
                    .positionId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .symbol(symbols[i])
                    .qty(BigDecimal.valueOf(100))
                    .avgPrice(BigDecimal.valueOf(50000 + i * 10000))
                    .realizedPnl(BigDecimal.ZERO)
                    .updatedAt(LocalDateTime.now())
                    .build();
            positionRepository.save(position);
        }

        // Create daily performance data
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 30; i++) {
            DailyPerformanceEntity perf = DailyPerformanceEntity.builder()
                    .performanceId(UlidGenerator.generate())
                    .accountId(testAccountId)
                    .strategyId(testStrategyId)
                    .tradeDate(today.minusDays(i))
                    .totalTrades(10)
                    .winningTrades(6)
                    .losingTrades(4)
                    .totalPnl(BigDecimal.valueOf(Math.random() * 200000 - 100000))
                    .realizedPnl(BigDecimal.valueOf(80000))
                    .unrealizedPnl(BigDecimal.valueOf(20000))
                    .totalVolume(BigDecimal.valueOf(10000000))
                    .totalFees(BigDecimal.valueOf(15000))
                    .build();
            dailyPerformanceRepository.save(perf);
        }
    }

    private String createTestExecution() {
        ExecutionHistoryEntity execution = ExecutionHistoryEntity.builder()
                .executionId(UlidGenerator.generate())
                .strategyId(testStrategyId)
                .accountId(testAccountId)
                .executionType("SIGNAL_GENERATED")
                .status("SUCCESS")
                .symbol("005930")
                .description("Buy signal generated for 005930")
                .details("{\"signal\": \"BUY\", \"price\": 70000}")
                .executionTimeMs(150)
                .createdAt(LocalDateTime.now())
                .build();
        return executionHistoryRepository.save(execution).getExecutionId();
    }
}
