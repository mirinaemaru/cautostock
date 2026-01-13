package maru.trading.domain.strategy;

import maru.trading.domain.signal.SignalDecision;

/**
 * Strategy execution interface.
 *
 * All trading strategies must implement this interface to participate
 * in automated signal generation.
 *
 * Strategy implementations should be stateless and thread-safe.
 */
public interface StrategyEngine {

    /**
     * Evaluate market data and strategy parameters to generate a signal decision.
     *
     * @param context Market data (bars, ticks) and strategy parameters
     * @return SignalDecision (BUY/SELL/HOLD with optional target and reason)
     * @throws IllegalArgumentException if context is invalid
     */
    SignalDecision evaluate(StrategyContext context);

    /**
     * Get the strategy type identifier (e.g., "MA_CROSSOVER", "RSI", "CUSTOM").
     * Used for factory instantiation and logging.
     *
     * @return Strategy type string
     */
    String getStrategyType();

    /**
     * Validate strategy parameters before execution.
     * Throws IllegalArgumentException if parameters are invalid.
     *
     * @param params Strategy parameters (JSON map)
     */
    void validateParams(java.util.Map<String, Object> params);
}
