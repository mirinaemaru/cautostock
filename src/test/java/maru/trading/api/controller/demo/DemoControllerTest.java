package maru.trading.api.controller.demo;

import maru.trading.demo.SimulationScenario;
import maru.trading.demo.StrategyDemoRunner;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.infra.persistence.jpa.entity.OrderEntity;
import maru.trading.infra.persistence.jpa.entity.SignalEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for DemoController.
 *
 * Priority 3 (Medium) - API Controller Tests
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DemoController Tests")
class DemoControllerTest {

    @Mock
    private StrategyDemoRunner demoRunner;

    @InjectMocks
    private DemoController controller;

    private StrategyDemoRunner.DemoResults mockResults;

    @BeforeEach
    void setUp() {
        // Setup mock demo results
        SignalEntity mockSignal = SignalEntity.builder()
                .signalId("SIG_001")
                .strategyId("STR_001")
                .accountId("ACC_DEMO_001")
                .symbol("DEMO_005930")
                .signalType("BUY")
                .reason("Golden cross detected")
                .createdAt(LocalDateTime.now())
                .expired(false)
                .build();

        OrderEntity mockOrder = OrderEntity.builder()
                .orderId("ORD_001")
                .accountId("ACC_DEMO_001")
                .strategyId("STR_001")
                .symbol("DEMO_005930")
                .side(Side.BUY)
                .orderType(OrderType.LIMIT)
                .ordDvsn("00")
                .qty(BigDecimal.TEN)
                .price(BigDecimal.valueOf(70000))
                .status(OrderStatus.SENT)
                .idempotencyKey("IDEM_001")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockResults = StrategyDemoRunner.DemoResults.builder()
                .strategyId("STR_001")
                .symbol("DEMO_005930")
                .signals(List.of(mockSignal))
                .orders(List.of(mockOrder))
                .build();
    }

    @Test
    @DisplayName("GET /scenarios - should return all simulation scenarios")
    void testListScenarios() {
        // When
        ResponseEntity<List<DemoController.ScenarioInfo>> response = controller.listScenarios();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(6);

        // Verify all scenarios are present
        List<String> scenarioNames = response.getBody().stream()
                .map(DemoController.ScenarioInfo::getName)
                .toList();

        assertThat(scenarioNames).containsExactlyInAnyOrder(
                "GOLDEN_CROSS",
                "DEATH_CROSS",
                "RSI_OVERSOLD",
                "RSI_OVERBOUGHT",
                "VOLATILE",
                "STABLE"
        );

        // Verify descriptions are present
        response.getBody().forEach(info -> {
            assertThat(info.getDescription()).isNotBlank();
            assertThat(info.getRecommendedStrategy()).isNotBlank();
        });
    }

    @Test
    @DisplayName("POST /run - should execute custom demo successfully")
    void testRunDemo_success() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("MA_CROSSOVER")
                .symbol("DEMO_005930")
                .accountId("ACC_DEMO_001")
                .build();

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDemo(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getScenario()).isEqualTo("GOLDEN_CROSS");
        assertThat(response.getBody().getStrategyType()).isEqualTo("MA_CROSSOVER");
        assertThat(response.getBody().getStrategyId()).isEqualTo("STR_001");
        assertThat(response.getBody().getSymbol()).isEqualTo("DEMO_005930");
        assertThat(response.getBody().getSignalsGenerated()).isEqualTo(1);
        assertThat(response.getBody().getOrdersPlaced()).isEqualTo(1);
        assertThat(response.getBody().getMessage()).isEqualTo("Demo completed successfully");

        // Verify runner was called
        verify(demoRunner, times(1)).runDemo(any(StrategyDemoRunner.DemoConfig.class));
    }

    @Test
    @DisplayName("POST /run - should use default symbol and accountId when not provided")
    void testRunDemo_withDefaults() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("MA_CROSSOVER")
                // No symbol or accountId
                .build();

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDemo(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // Verify config passed to runner uses defaults
        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getSymbol().equals("DEMO_STOCK") &&
                        config.getAccountId().equals("ACC_DEMO_001")
        ));
    }

    @Test
    @DisplayName("POST /run - should return 400 when scenario is null")
    void testRunDemo_nullScenario() {
        // Given
        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(null)  // Missing scenario
                .strategyType("MA_CROSSOVER")
                .build();

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDemo(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("scenario is required");

        // Verify runner was NOT called
        verify(demoRunner, never()).runDemo(any());
    }

    @Test
    @DisplayName("POST /run - should return 400 when strategyType is null")
    void testRunDemo_nullStrategyType() {
        // Given
        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType(null)  // Missing strategyType
                .build();

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDemo(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("strategyType is required");

        // Verify runner was NOT called
        verify(demoRunner, never()).runDemo(any());
    }

    @Test
    @DisplayName("POST /run - should return 400 when strategyType is blank")
    void testRunDemo_blankStrategyType() {
        // Given
        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("   ")  // Blank strategyType
                .build();

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDemo(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("strategyType is required");
    }

    @Test
    @DisplayName("POST /run - should return 500 when demo execution fails")
    void testRunDemo_executionFailure() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("MA_CROSSOVER")
                .build();

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDemo(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Demo failed");
        assertThat(response.getBody().getMessage()).contains("Database connection failed");
    }

    @Test
    @DisplayName("POST /golden-cross - should execute golden cross demo")
    void testRunGoldenCrossDemo() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runGoldenCrossDemo();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        // Verify correct scenario and strategy
        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getScenario() == SimulationScenario.GOLDEN_CROSS &&
                        config.getStrategyType().equals("MA_CROSSOVER")
        ));
    }

    @Test
    @DisplayName("POST /death-cross - should execute death cross demo")
    void testRunDeathCrossDemo() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runDeathCrossDemo();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getScenario() == SimulationScenario.DEATH_CROSS &&
                        config.getStrategyType().equals("MA_CROSSOVER")
        ));
    }

    @Test
    @DisplayName("POST /rsi-oversold - should execute RSI oversold demo")
    void testRunRsiOversoldDemo() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runRsiOversoldDemo();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getScenario() == SimulationScenario.RSI_OVERSOLD &&
                        config.getStrategyType().equals("RSI")
        ));
    }

    @Test
    @DisplayName("POST /rsi-overbought - should execute RSI overbought demo")
    void testRunRsiOverboughtDemo() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runRsiOverboughtDemo();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getScenario() == SimulationScenario.RSI_OVERBOUGHT &&
                        config.getStrategyType().equals("RSI")
        ));
    }

    @Test
    @DisplayName("POST /volatile - should execute volatile market demo")
    void testRunVolatileDemo() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runVolatileDemo();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getScenario() == SimulationScenario.VOLATILE &&
                        config.getStrategyType().equals("MA_CROSSOVER")
        ));
    }

    @Test
    @DisplayName("POST /stable - should execute stable market demo")
    void testRunStableDemo() {
        // Given
        when(demoRunner.runDemo(any(StrategyDemoRunner.DemoConfig.class)))
                .thenReturn(mockResults);

        // When
        ResponseEntity<DemoController.DemoResponse> response = controller.runStableDemo();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();

        verify(demoRunner, times(1)).runDemo(argThat(config ->
                config.getScenario() == SimulationScenario.STABLE &&
                        config.getStrategyType().equals("MA_CROSSOVER")
        ));
    }

    @Test
    @DisplayName("Scenario descriptions should match expected values")
    void testScenarioDescriptions() {
        // When
        ResponseEntity<List<DemoController.ScenarioInfo>> response = controller.listScenarios();

        // Then
        List<DemoController.ScenarioInfo> scenarios = response.getBody();
        assertThat(scenarios).isNotNull();

        // Find and verify each scenario
        DemoController.ScenarioInfo goldenCross = scenarios.stream()
                .filter(s -> s.getName().equals("GOLDEN_CROSS"))
                .findFirst()
                .orElseThrow();
        assertThat(goldenCross.getDescription()).contains("MA5 crosses above MA20");
        assertThat(goldenCross.getDescription()).contains("bullish");
        assertThat(goldenCross.getRecommendedStrategy()).isEqualTo("MA_CROSSOVER");

        DemoController.ScenarioInfo deathCross = scenarios.stream()
                .filter(s -> s.getName().equals("DEATH_CROSS"))
                .findFirst()
                .orElseThrow();
        assertThat(deathCross.getDescription()).contains("MA5 crosses below MA20");
        assertThat(deathCross.getDescription()).contains("bearish");
        assertThat(deathCross.getRecommendedStrategy()).isEqualTo("MA_CROSSOVER");

        DemoController.ScenarioInfo rsiOversold = scenarios.stream()
                .filter(s -> s.getName().equals("RSI_OVERSOLD"))
                .findFirst()
                .orElseThrow();
        assertThat(rsiOversold.getDescription()).contains("RSI crosses below 30");
        assertThat(rsiOversold.getDescription()).contains("buy signal");
        assertThat(rsiOversold.getRecommendedStrategy()).isEqualTo("RSI");

        DemoController.ScenarioInfo rsiOverbought = scenarios.stream()
                .filter(s -> s.getName().equals("RSI_OVERBOUGHT"))
                .findFirst()
                .orElseThrow();
        assertThat(rsiOverbought.getDescription()).contains("RSI crosses above 70");
        assertThat(rsiOverbought.getDescription()).contains("sell signal");
        assertThat(rsiOverbought.getRecommendedStrategy()).isEqualTo("RSI");
    }

    @Test
    @DisplayName("DemoRequest builder should set all fields correctly")
    void testDemoRequestBuilder() {
        // When
        DemoController.DemoRequest request = DemoController.DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("MA_CROSSOVER")
                .symbol("TEST_SYMBOL")
                .accountId("ACC_TEST")
                .build();

        // Then
        assertThat(request.getScenario()).isEqualTo(SimulationScenario.GOLDEN_CROSS);
        assertThat(request.getStrategyType()).isEqualTo("MA_CROSSOVER");
        assertThat(request.getSymbol()).isEqualTo("TEST_SYMBOL");
        assertThat(request.getAccountId()).isEqualTo("ACC_TEST");
    }

    @Test
    @DisplayName("DemoResponse builder should set all fields correctly")
    void testDemoResponseBuilder() {
        // When
        DemoController.DemoResponse response = DemoController.DemoResponse.builder()
                .success(true)
                .scenario("GOLDEN_CROSS")
                .strategyType("MA_CROSSOVER")
                .strategyId("STR_001")
                .symbol("TEST_SYMBOL")
                .signalsGenerated(5)
                .ordersPlaced(3)
                .message("Test message")
                .build();

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getScenario()).isEqualTo("GOLDEN_CROSS");
        assertThat(response.getStrategyType()).isEqualTo("MA_CROSSOVER");
        assertThat(response.getStrategyId()).isEqualTo("STR_001");
        assertThat(response.getSymbol()).isEqualTo("TEST_SYMBOL");
        assertThat(response.getSignalsGenerated()).isEqualTo(5);
        assertThat(response.getOrdersPlaced()).isEqualTo(3);
        assertThat(response.getMessage()).isEqualTo("Test message");
    }
}
