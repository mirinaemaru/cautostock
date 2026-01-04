package maru.trading.demo;

import maru.trading.application.scheduler.StrategyScheduler;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.*;
import maru.trading.infra.persistence.jpa.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy Demo Runner.
 *
 * Sets up and runs a complete demo of the autonomous trading system:
 * 1. Creates a strategy with parameters
 * 2. Maps strategy to symbols
 * 3. Simulates market data
 * 4. Triggers strategy execution
 * 5. Reports results (signals, orders)
 *
 * Usage:
 * <pre>
 * StrategyDemoRunner runner = ...;
 * runner.runDemo(DemoConfig.builder()
 *     .scenario(SimulationScenario.GOLDEN_CROSS)
 *     .strategyType("MA_CROSSOVER")
 *     .symbol("DEMO_STOCK")
 *     .accountId("ACC_DEMO_001")
 *     .build());
 * </pre>
 */
@Component
public class StrategyDemoRunner {

    private static final Logger log = LoggerFactory.getLogger(StrategyDemoRunner.class);

    private final MarketDataSimulator simulator;
    private final StrategyScheduler strategyScheduler;
    private final StrategyJpaRepository strategyRepository;
    private final StrategyVersionJpaRepository strategyVersionRepository;
    private final StrategySymbolJpaRepository strategySymbolRepository;
    private final SignalJpaRepository signalRepository;
    private final OrderJpaRepository orderRepository;

    public StrategyDemoRunner(
            MarketDataSimulator simulator,
            StrategyScheduler strategyScheduler,
            StrategyJpaRepository strategyRepository,
            StrategyVersionJpaRepository strategyVersionRepository,
            StrategySymbolJpaRepository strategySymbolRepository,
            SignalJpaRepository signalRepository,
            OrderJpaRepository orderRepository) {
        this.simulator = simulator;
        this.strategyScheduler = strategyScheduler;
        this.strategyRepository = strategyRepository;
        this.strategyVersionRepository = strategyVersionRepository;
        this.strategySymbolRepository = strategySymbolRepository;
        this.signalRepository = signalRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Run a complete demo scenario.
     *
     * @param config Demo configuration
     * @return Demo results
     */
    public DemoResults runDemo(DemoConfig config) {
        log.info("========================================");
        log.info("Starting Strategy Demo");
        log.info("========================================");
        log.info("Scenario: {}", config.getScenario());
        log.info("Strategy Type: {}", config.getStrategyType());
        log.info("Symbol: {}", config.getSymbol());
        log.info("Account: {}", config.getAccountId());
        log.info("========================================");

        // Step 1: Setup strategy
        String strategyId = setupStrategy(config);

        // Step 2: Run simulation
        log.info("Step 2: Running market data simulation...");
        simulator.runSync(config.getScenario(), config.getSymbol());
        log.info("Simulation complete");

        // Step 3: Execute strategy
        log.info("Step 3: Executing strategy...");
        strategyScheduler.executeStrategies();
        log.info("Strategy execution complete");

        // Step 4: Collect results
        log.info("Step 4: Collecting results...");
        DemoResults results = collectResults(strategyId, config.getSymbol());

        // Step 5: Print results
        printResults(results);

        log.info("========================================");
        log.info("Demo Complete");
        log.info("========================================");

        return results;
    }

    private String setupStrategy(DemoConfig config) {
        log.info("Step 1: Setting up strategy...");

        // Create strategy ID
        String strategyId = UlidGenerator.generate();
        String versionId = UlidGenerator.generate();

        // Create strategy first (must be saved before version due to FK constraint)
        StrategyEntity strategy = StrategyEntity.builder()
                .strategyId(strategyId)
                .name(config.getStrategyType() + " Demo " + strategyId.substring(0, 8))
                .description("Demo strategy for scenario: " + config.getScenario())
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(versionId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        strategyRepository.save(strategy);

        // Create strategy version after strategy (FK constraint)
        Map<String, Object> params = createStrategyParams(config.getStrategyType());
        StrategyVersionEntity version = StrategyVersionEntity.builder()
                .strategyVersionId(versionId)
                .strategyId(strategyId)
                .versionNo(1)
                .paramsJson(convertToJson(params))
                .createdAt(LocalDateTime.now())
                .build();
        strategyVersionRepository.save(version);

        // Create StrategySymbol mapping
        StrategySymbolEntity strategySymbol = StrategySymbolEntity.builder()
                .strategySymbolId(UlidGenerator.generate())
                .strategyId(strategyId)
                .symbol(config.getSymbol())
                .accountId(config.getAccountId())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        strategySymbolRepository.save(strategySymbol);

        log.info("Strategy created: id={}, type={}", strategyId, config.getStrategyType());
        log.info("Strategy mapped to symbol: {}", config.getSymbol());

        return strategyId;
    }

    private Map<String, Object> createStrategyParams(String strategyType) {
        Map<String, Object> params = new HashMap<>();

        switch (strategyType) {
            case "MA_CROSSOVER":
                params.put("shortPeriod", 5);
                params.put("longPeriod", 20);
                params.put("ttlSeconds", 300);
                break;
            case "RSI":
                params.put("period", 14);
                params.put("oversold", 30);
                params.put("overbought", 70);
                params.put("ttlSeconds", 300);
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy type: " + strategyType);
        }

        return params;
    }

    private DemoResults collectResults(String strategyId, String symbol) {
        List<SignalEntity> signals = signalRepository.findAll().stream()
                .filter(s -> s.getStrategyId().equals(strategyId))
                .toList();

        List<OrderEntity> orders = orderRepository.findAll().stream()
                .filter(o -> o.getStrategyId() != null && o.getStrategyId().equals(strategyId))
                .toList();

        return DemoResults.builder()
                .strategyId(strategyId)
                .symbol(symbol)
                .signals(signals)
                .orders(orders)
                .build();
    }

    private void printResults(DemoResults results) {
        log.info("========================================");
        log.info("Demo Results");
        log.info("========================================");
        log.info("Strategy ID: {}", results.getStrategyId());
        log.info("Symbol: {}", results.getSymbol());
        log.info("Signals Generated: {}", results.getSignals().size());
        log.info("Orders Placed: {}", results.getOrders().size());

        if (!results.getSignals().isEmpty()) {
            log.info("----------------------------------------");
            log.info("Signals:");
            for (SignalEntity signal : results.getSignals()) {
                log.info("  - Type: {}, Symbol: {}, Reason: {}",
                        signal.getSignalType(),
                        signal.getSymbol(),
                        signal.getReason());
            }
        }

        if (!results.getOrders().isEmpty()) {
            log.info("----------------------------------------");
            log.info("Orders:");
            for (OrderEntity order : results.getOrders()) {
                log.info("  - Side: {}, Symbol: {}, Qty: {}, Price: {}, Status: {}",
                        order.getSide(),
                        order.getSymbol(),
                        order.getQty(),
                        order.getPrice(),
                        order.getStatus());
            }
        }

        log.info("========================================");
    }

    private String convertToJson(Map<String, Object> params) {
        StringBuilder json = new StringBuilder("{");
        params.forEach((key, value) -> {
            json.append("\"").append(key).append("\":");
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            json.append(",");
        });
        if (json.length() > 1) {
            json.setLength(json.length() - 1);
        }
        json.append("}");
        return json.toString();
    }

    /**
     * Demo configuration.
     */
    public static class DemoConfig {
        private final SimulationScenario scenario;
        private final String strategyType;
        private final String symbol;
        private final String accountId;

        private DemoConfig(Builder builder) {
            this.scenario = builder.scenario;
            this.strategyType = builder.strategyType;
            this.symbol = builder.symbol;
            this.accountId = builder.accountId;
        }

        public SimulationScenario getScenario() {
            return scenario;
        }

        public String getStrategyType() {
            return strategyType;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getAccountId() {
            return accountId;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SimulationScenario scenario;
            private String strategyType;
            private String symbol = "DEMO_STOCK";
            private String accountId = "ACC_DEMO_001";

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

            public DemoConfig build() {
                if (scenario == null) {
                    throw new IllegalStateException("scenario is required");
                }
                if (strategyType == null) {
                    throw new IllegalStateException("strategyType is required");
                }
                return new DemoConfig(this);
            }
        }
    }

    /**
     * Demo results.
     */
    public static class DemoResults {
        private final String strategyId;
        private final String symbol;
        private final List<SignalEntity> signals;
        private final List<OrderEntity> orders;

        private DemoResults(Builder builder) {
            this.strategyId = builder.strategyId;
            this.symbol = builder.symbol;
            this.signals = builder.signals;
            this.orders = builder.orders;
        }

        public String getStrategyId() {
            return strategyId;
        }

        public String getSymbol() {
            return symbol;
        }

        public List<SignalEntity> getSignals() {
            return signals;
        }

        public List<OrderEntity> getOrders() {
            return orders;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String strategyId;
            private String symbol;
            private List<SignalEntity> signals;
            private List<OrderEntity> orders;

            public Builder strategyId(String strategyId) {
                this.strategyId = strategyId;
                return this;
            }

            public Builder symbol(String symbol) {
                this.symbol = symbol;
                return this;
            }

            public Builder signals(List<SignalEntity> signals) {
                this.signals = signals;
                return this;
            }

            public Builder orders(List<OrderEntity> orders) {
                this.orders = orders;
                return this;
            }

            public DemoResults build() {
                return new DemoResults(this);
            }
        }
    }
}
