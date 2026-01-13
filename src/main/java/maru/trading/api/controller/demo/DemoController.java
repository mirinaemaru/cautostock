package maru.trading.api.controller.demo;

import maru.trading.demo.SimulationScenario;
import maru.trading.demo.StrategyDemoRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * Demo Controller for testing autonomous trading pipeline.
 *
 * Provides REST API endpoints to run demo scenarios without real market data.
 *
 * IMPORTANT: This controller is only enabled in development/demo environments.
 * Disabled in production by setting: trading.demo.enabled=false
 *
 * Endpoints:
 * - GET  /api/v1/demo/scenarios - List available scenarios
 * - POST /api/v1/demo/run - Run a demo scenario
 * - POST /api/v1/demo/golden-cross - Quick golden cross demo
 * - POST /api/v1/demo/death-cross - Quick death cross demo
 * - POST /api/v1/demo/rsi-oversold - Quick RSI oversold demo
 * - POST /api/v1/demo/rsi-overbought - Quick RSI overbought demo
 */
@RestController
@RequestMapping("/api/v1/demo")
@ConditionalOnProperty(
        name = "trading.demo.enabled",
        havingValue = "true",
        matchIfMissing = true  // Enabled by default (disable in production!)
)
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final StrategyDemoRunner demoRunner;

    public DemoController(StrategyDemoRunner demoRunner) {
        this.demoRunner = demoRunner;
    }

    /**
     * List available simulation scenarios.
     *
     * GET /api/v1/demo/scenarios
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<ScenarioInfo>> listScenarios() {
        List<ScenarioInfo> scenarios = Stream.of(SimulationScenario.values())
                .map(s -> new ScenarioInfo(
                        s.name(),
                        getScenarioDescription(s),
                        getRecommendedStrategy(s)
                ))
                .toList();

        return ResponseEntity.ok(scenarios);
    }

    /**
     * Run a custom demo scenario.
     *
     * POST /api/v1/demo/run
     *
     * Request body:
     * {
     *   "scenario": "GOLDEN_CROSS",
     *   "strategyType": "MA_CROSSOVER",
     *   "symbol": "DEMO_005930",
     *   "accountId": "ACC_DEMO_001"
     * }
     */
    @PostMapping("/run")
    public ResponseEntity<DemoResponse> runDemo(@RequestBody DemoRequest request) {
        log.info("Received demo request: scenario={}, strategy={}",
                request.getScenario(), request.getStrategyType());

        try {
            // Validate request
            validateRequest(request);

            // Run demo
            StrategyDemoRunner.DemoResults results = demoRunner.runDemo(
                    StrategyDemoRunner.DemoConfig.builder()
                            .scenario(request.getScenario())
                            .strategyType(request.getStrategyType())
                            .symbol(request.getSymbol() != null ? request.getSymbol() : "DEMO_STOCK")
                            .accountId(request.getAccountId() != null ? request.getAccountId() : "ACC_DEMO_001")
                            .build()
            );

            // Build response
            DemoResponse response = DemoResponse.builder()
                    .success(true)
                    .scenario(request.getScenario().name())
                    .strategyType(request.getStrategyType())
                    .strategyId(results.getStrategyId())
                    .symbol(results.getSymbol())
                    .signalsGenerated(results.getSignals().size())
                    .ordersPlaced(results.getOrders().size())
                    .message("Demo completed successfully")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid demo request", e);
            return ResponseEntity.badRequest()
                    .body(DemoResponse.builder()
                            .success(false)
                            .message("Invalid request: " + e.getMessage())
                            .build());

        } catch (Exception e) {
            log.error("Demo execution failed", e);
            return ResponseEntity.internalServerError()
                    .body(DemoResponse.builder()
                            .success(false)
                            .message("Demo failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Quick demo: Golden Cross (MA5 crosses above MA20).
     *
     * POST /api/v1/demo/golden-cross
     */
    @PostMapping("/golden-cross")
    public ResponseEntity<DemoResponse> runGoldenCrossDemo() {
        return runDemo(DemoRequest.builder()
                .scenario(SimulationScenario.GOLDEN_CROSS)
                .strategyType("MA_CROSSOVER")
                .build());
    }

    /**
     * Quick demo: Death Cross (MA5 crosses below MA20).
     *
     * POST /api/v1/demo/death-cross
     */
    @PostMapping("/death-cross")
    public ResponseEntity<DemoResponse> runDeathCrossDemo() {
        return runDemo(DemoRequest.builder()
                .scenario(SimulationScenario.DEATH_CROSS)
                .strategyType("MA_CROSSOVER")
                .build());
    }

    /**
     * Quick demo: RSI Oversold (RSI crosses below 30).
     *
     * POST /api/v1/demo/rsi-oversold
     */
    @PostMapping("/rsi-oversold")
    public ResponseEntity<DemoResponse> runRsiOversoldDemo() {
        return runDemo(DemoRequest.builder()
                .scenario(SimulationScenario.RSI_OVERSOLD)
                .strategyType("RSI")
                .build());
    }

    /**
     * Quick demo: RSI Overbought (RSI crosses above 70).
     *
     * POST /api/v1/demo/rsi-overbought
     */
    @PostMapping("/rsi-overbought")
    public ResponseEntity<DemoResponse> runRsiOverboughtDemo() {
        return runDemo(DemoRequest.builder()
                .scenario(SimulationScenario.RSI_OVERBOUGHT)
                .strategyType("RSI")
                .build());
    }

    /**
     * Quick demo: Volatile market.
     *
     * POST /api/v1/demo/volatile
     */
    @PostMapping("/volatile")
    public ResponseEntity<DemoResponse> runVolatileDemo() {
        return runDemo(DemoRequest.builder()
                .scenario(SimulationScenario.VOLATILE)
                .strategyType("MA_CROSSOVER")
                .build());
    }

    /**
     * Quick demo: Stable market.
     *
     * POST /api/v1/demo/stable
     */
    @PostMapping("/stable")
    public ResponseEntity<DemoResponse> runStableDemo() {
        return runDemo(DemoRequest.builder()
                .scenario(SimulationScenario.STABLE)
                .strategyType("MA_CROSSOVER")
                .build());
    }

    // ==================== Helper Methods ====================

    private void validateRequest(DemoRequest request) {
        if (request.getScenario() == null) {
            throw new IllegalArgumentException("scenario is required");
        }
        if (request.getStrategyType() == null || request.getStrategyType().isBlank()) {
            throw new IllegalArgumentException("strategyType is required");
        }
    }

    private String getScenarioDescription(SimulationScenario scenario) {
        return switch (scenario) {
            case GOLDEN_CROSS -> "MA5 crosses above MA20 (bullish signal)";
            case DEATH_CROSS -> "MA5 crosses below MA20 (bearish signal)";
            case RSI_OVERSOLD -> "RSI crosses below 30 (buy signal)";
            case RSI_OVERBOUGHT -> "RSI crosses above 70 (sell signal)";
            case VOLATILE -> "Random volatile price movements (±5%)";
            case STABLE -> "Stable price with small fluctuations (±0.1%)";
        };
    }

    private String getRecommendedStrategy(SimulationScenario scenario) {
        return switch (scenario) {
            case GOLDEN_CROSS, DEATH_CROSS, VOLATILE, STABLE -> "MA_CROSSOVER";
            case RSI_OVERSOLD, RSI_OVERBOUGHT -> "RSI";
        };
    }

    // ==================== DTOs ====================

    /**
     * Scenario information.
     */
    public static class ScenarioInfo {
        private String name;
        private String description;
        private String recommendedStrategy;

        public ScenarioInfo(String name, String description, String recommendedStrategy) {
            this.name = name;
            this.description = description;
            this.recommendedStrategy = recommendedStrategy;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getRecommendedStrategy() {
            return recommendedStrategy;
        }
    }

    /**
     * Demo request.
     */
    public static class DemoRequest {
        private SimulationScenario scenario;
        private String strategyType;
        private String symbol;
        private String accountId;

        public SimulationScenario getScenario() {
            return scenario;
        }

        public void setScenario(SimulationScenario scenario) {
            this.scenario = scenario;
        }

        public String getStrategyType() {
            return strategyType;
        }

        public void setStrategyType(String strategyType) {
            this.strategyType = strategyType;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SimulationScenario scenario;
            private String strategyType;
            private String symbol;
            private String accountId;

            public Builder scenario(SimulationScenario scenario) {
                this.scenario = scenario;
                return this;
            }

            public Builder strategyType(String strategyType) {
                this.strategyType = strategyType;
                return this;
            }

            public Builder symbol(String symbol) {
                this.symbol = symbol;
                return this;
            }

            public Builder accountId(String accountId) {
                this.accountId = accountId;
                return this;
            }

            public DemoRequest build() {
                DemoRequest request = new DemoRequest();
                request.scenario = this.scenario;
                request.strategyType = this.strategyType;
                request.symbol = this.symbol;
                request.accountId = this.accountId;
                return request;
            }
        }
    }

    /**
     * Demo response.
     */
    public static class DemoResponse {
        private boolean success;
        private String scenario;
        private String strategyType;
        private String strategyId;
        private String symbol;
        private Integer signalsGenerated;
        private Integer ordersPlaced;
        private String message;

        public boolean isSuccess() {
            return success;
        }

        public String getScenario() {
            return scenario;
        }

        public String getStrategyType() {
            return strategyType;
        }

        public String getStrategyId() {
            return strategyId;
        }

        public String getSymbol() {
            return symbol;
        }

        public Integer getSignalsGenerated() {
            return signalsGenerated;
        }

        public Integer getOrdersPlaced() {
            return ordersPlaced;
        }

        public String getMessage() {
            return message;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean success;
            private String scenario;
            private String strategyType;
            private String strategyId;
            private String symbol;
            private Integer signalsGenerated;
            private Integer ordersPlaced;
            private String message;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder scenario(String scenario) {
                this.scenario = scenario;
                return this;
            }

            public Builder strategyType(String strategyType) {
                this.strategyType = strategyType;
                return this;
            }

            public Builder strategyId(String strategyId) {
                this.strategyId = strategyId;
                return this;
            }

            public Builder symbol(String symbol) {
                this.symbol = symbol;
                return this;
            }

            public Builder signalsGenerated(Integer signalsGenerated) {
                this.signalsGenerated = signalsGenerated;
                return this;
            }

            public Builder ordersPlaced(Integer ordersPlaced) {
                this.ordersPlaced = ordersPlaced;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public DemoResponse build() {
                DemoResponse response = new DemoResponse();
                response.success = this.success;
                response.scenario = this.scenario;
                response.strategyType = this.strategyType;
                response.strategyId = this.strategyId;
                response.symbol = this.symbol;
                response.signalsGenerated = this.signalsGenerated;
                response.ordersPlaced = this.ordersPlaced;
                response.message = this.message;
                return response;
            }
        }
    }
}
