package maru.trading.application.usecase.strategy;

import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.strategy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use case for executing a strategy.
 *
 * Orchestrates the full strategy execution pipeline:
 * 1. Load strategy and version from database
 * 2. Load market data context
 * 3. Instantiate strategy engine
 * 4. Evaluate strategy to generate decision
 * 5. Generate and persist signal (if not HOLD)
 *
 * This is the entry point called by StrategyScheduler.
 */
@Service
public class ExecuteStrategyUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExecuteStrategyUseCase.class);

    private final StrategyRepository strategyRepository;
    private final LoadStrategyContextUseCase loadContextUseCase;
    private final GenerateSignalUseCase generateSignalUseCase;

    public ExecuteStrategyUseCase(
            StrategyRepository strategyRepository,
            LoadStrategyContextUseCase loadContextUseCase,
            GenerateSignalUseCase generateSignalUseCase) {
        this.strategyRepository = strategyRepository;
        this.loadContextUseCase = loadContextUseCase;
        this.generateSignalUseCase = generateSignalUseCase;
    }

    /**
     * Execute strategy for a specific symbol.
     *
     * @param strategyId Strategy ID
     * @param symbol Symbol to evaluate
     * @param accountId Account ID
     * @return Generated signal (or null if HOLD or error)
     */
    public Signal execute(String strategyId, String symbol, String accountId) {
        log.info("Executing strategy: strategyId={}, symbol={}, accountId={}",
                strategyId, symbol, accountId);

        try {
            // Step 1: Load strategy configuration
            Strategy strategy = strategyRepository.findById(strategyId)
                    .orElseThrow(() -> new IllegalArgumentException("Strategy not found: " + strategyId));

            if (!strategy.isActive()) {
                log.warn("Strategy is not active, skipping: strategyId={}, status={}",
                        strategyId, strategy.getStatus());
                return null;
            }

            // Step 2: Load strategy version
            String activeVersionId = strategy.getActiveVersionId();
            StrategyVersion version = strategyRepository.findVersionById(activeVersionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Strategy version not found: versionId=" + activeVersionId));

            // Step 3: Load strategy context (bars + params)
            StrategyContext context = loadContextUseCase.execute(strategy, version, symbol, accountId);

            // Validate context
            context.validate();

            // Step 4: Instantiate strategy engine based on strategy name
            StrategyEngine engine = createStrategyEngine(strategy.getName(), context);

            // Step 5: Evaluate strategy
            SignalDecision decision = engine.evaluate(context);

            log.info("Strategy evaluated: strategyId={}, symbol={}, decision={}",
                    strategyId, symbol, decision.getSignalType());

            // Step 6: Generate and persist signal (if not HOLD)
            Signal signal = generateSignalUseCase.execute(decision, strategy, version, symbol, accountId);

            if (signal != null) {
                log.info("Signal generated: signalId={}, type={}", signal.getSignalId(), signal.getSignalType());
            }

            return signal;

        } catch (Exception e) {
            log.error("Error executing strategy: strategyId={}, symbol={}", strategyId, symbol, e);
            return null;
        }
    }

    /**
     * Create strategy engine instance based on strategy name.
     *
     * @param strategyName Strategy name (e.g., "MA_CROSSOVER", "RSI")
     * @param context Strategy context
     * @return Strategy engine instance
     */
    private StrategyEngine createStrategyEngine(String strategyName, StrategyContext context) {
        // Determine strategy type from name
        // Convention: strategy name should match strategy type
        String strategyType = extractStrategyType(strategyName);

        // Validate parameters before instantiation
        StrategyEngine engine = StrategyFactory.createStrategy(strategyType);
        engine.validateParams(context.getParams());

        return engine;
    }

    /**
     * Extract strategy type from strategy name.
     * Examples:
     * - "MA Crossover Strategy" -> "MA_CROSSOVER"
     * - "RSI Oversold" -> "RSI"
     *
     * @param strategyName Strategy name
     * @return Strategy type identifier
     */
    private String extractStrategyType(String strategyName) {
        if (strategyName == null) {
            throw new IllegalArgumentException("Strategy name cannot be null");
        }

        String upperName = strategyName.toUpperCase();

        // Simple pattern matching
        if (upperName.contains("MA") && upperName.contains("CROSS")) {
            return "MA_CROSSOVER";
        } else if (upperName.contains("RSI")) {
            return "RSI";
        }

        // Fallback: assume strategy name IS the strategy type
        return strategyName.toUpperCase().replace(" ", "_");
    }
}
