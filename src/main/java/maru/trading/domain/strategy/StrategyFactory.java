package maru.trading.domain.strategy;

import maru.trading.domain.strategy.impl.BollingerBandsStrategy;
import maru.trading.domain.strategy.impl.MACDStrategy;
import maru.trading.domain.strategy.impl.MACrossoverStrategy;
import maru.trading.domain.strategy.impl.RSIStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating strategy instances.
 *
 * Supports:
 * - MA_CROSSOVER: Moving Average crossover strategy
 * - RSI: RSI overbought/oversold strategy
 * - BOLLINGER_BANDS: Bollinger Bands mean reversion strategy
 * - MACD: MACD crossover strategy
 *
 * Extensible - add new strategies by:
 * 1. Implementing StrategyEngine interface
 * 2. Registering in this factory
 */
public class StrategyFactory {

    private static final Map<String, Class<? extends StrategyEngine>> STRATEGY_REGISTRY = new HashMap<>();

    static {
        // Register built-in strategies
        STRATEGY_REGISTRY.put("MA_CROSSOVER", MACrossoverStrategy.class);
        STRATEGY_REGISTRY.put("RSI", RSIStrategy.class);
        STRATEGY_REGISTRY.put("BOLLINGER_BANDS", BollingerBandsStrategy.class);
        STRATEGY_REGISTRY.put("MACD", MACDStrategy.class);
    }

    /**
     * Create a strategy instance by type.
     *
     * @param strategyType Strategy type identifier (e.g., "MA_CROSSOVER", "RSI")
     * @return New strategy instance
     * @throws IllegalArgumentException if strategy type is unknown
     */
    public static StrategyEngine createStrategy(String strategyType) {
        if (strategyType == null || strategyType.isBlank()) {
            throw new IllegalArgumentException("Strategy type cannot be null or blank");
        }

        Class<? extends StrategyEngine> strategyClass = STRATEGY_REGISTRY.get(strategyType);
        if (strategyClass == null) {
            throw new IllegalArgumentException("Unknown strategy type: " + strategyType +
                    ". Available types: " + STRATEGY_REGISTRY.keySet());
        }

        try {
            return strategyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate strategy: " + strategyType, e);
        }
    }

    /**
     * Register a custom strategy type.
     * Useful for adding strategies without modifying this class.
     *
     * @param strategyType Strategy type identifier
     * @param strategyClass Strategy implementation class
     */
    public static void registerStrategy(String strategyType, Class<? extends StrategyEngine> strategyClass) {
        if (strategyType == null || strategyType.isBlank()) {
            throw new IllegalArgumentException("Strategy type cannot be null or blank");
        }
        if (strategyClass == null) {
            throw new IllegalArgumentException("Strategy class cannot be null");
        }

        STRATEGY_REGISTRY.put(strategyType, strategyClass);
    }

    /**
     * Check if a strategy type is registered.
     *
     * @param strategyType Strategy type identifier
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(String strategyType) {
        return STRATEGY_REGISTRY.containsKey(strategyType);
    }

    /**
     * Get all registered strategy types.
     *
     * @return Set of strategy type identifiers
     */
    public static java.util.Set<String> getRegisteredTypes() {
        return STRATEGY_REGISTRY.keySet();
    }
}
