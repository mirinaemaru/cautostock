package maru.trading.domain.strategy.impl;

import maru.trading.domain.market.MarketBar;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.strategy.StrategyContext;
import maru.trading.domain.strategy.StrategyEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for strategy implementations.
 *
 * Provides common functionality:
 * - Parameter validation
 * - Price extraction from bars
 * - Default TTL handling
 *
 * Subclasses implement evaluate() method with specific strategy logic.
 */
public abstract class BaseStrategy implements StrategyEngine {

    protected static final int DEFAULT_TTL_SECONDS = 300; // 5 minutes

    /**
     * Validate that context has minimum required bars.
     *
     * @param context Strategy context
     * @param minBars Minimum number of bars required
     * @throws IllegalArgumentException if insufficient bars
     */
    protected void validateMinimumBars(StrategyContext context, int minBars) {
        if (context.getBarCount() < minBars) {
            throw new IllegalArgumentException(
                    "Insufficient bars for " + getStrategyType() +
                            ": need " + minBars + ", got " + context.getBarCount());
        }
    }

    /**
     * Extract close prices from bars.
     *
     * @param bars List of market bars
     * @return List of close prices
     */
    protected List<BigDecimal> extractClosePrices(List<MarketBar> bars) {
        return bars.stream()
                .map(MarketBar::getClose)
                .collect(Collectors.toList());
    }

    /**
     * Extract open prices from bars.
     *
     * @param bars List of market bars
     * @return List of open prices
     */
    protected List<BigDecimal> extractOpenPrices(List<MarketBar> bars) {
        return bars.stream()
                .map(MarketBar::getOpen)
                .collect(Collectors.toList());
    }

    /**
     * Extract high prices from bars.
     *
     * @param bars List of market bars
     * @return List of high prices
     */
    protected List<BigDecimal> extractHighPrices(List<MarketBar> bars) {
        return bars.stream()
                .map(MarketBar::getHigh)
                .collect(Collectors.toList());
    }

    /**
     * Extract low prices from bars.
     *
     * @param bars List of market bars
     * @return List of low prices
     */
    protected List<BigDecimal> extractLowPrices(List<MarketBar> bars) {
        return bars.stream()
                .map(MarketBar::getLow)
                .collect(Collectors.toList());
    }

    /**
     * Get the most recent close price.
     *
     * @param context Strategy context
     * @return Latest close price
     */
    protected BigDecimal getLatestPrice(StrategyContext context) {
        MarketBar latestBar = context.getLatestBar();
        if (latestBar == null) {
            throw new IllegalStateException("No bars available");
        }
        return latestBar.getClose();
    }

    /**
     * Get default quantity for signal (1 share).
     * Subclasses can override for different position sizing logic.
     *
     * @param context Strategy context
     * @return Default quantity (1)
     */
    protected BigDecimal getDefaultQuantity(StrategyContext context) {
        return BigDecimal.ONE;
    }

    /**
     * Get TTL from params or use default.
     *
     * @param context Strategy context
     * @return TTL in seconds
     */
    protected int getTtlSeconds(StrategyContext context) {
        try {
            return context.getParamAsInt("ttlSeconds");
        } catch (IllegalArgumentException e) {
            return DEFAULT_TTL_SECONDS;
        }
    }

    /**
     * Extract volumes from bars.
     *
     * @param bars List of market bars
     * @return List of volumes
     */
    protected List<Long> extractVolumes(List<MarketBar> bars) {
        return bars.stream()
                .map(MarketBar::getVolume)
                .collect(Collectors.toList());
    }

    /**
     * Get parameter with default value (double).
     *
     * @param context Strategy context
     * @param key Parameter key
     * @param defaultValue Default value if parameter not found
     * @return Parameter value or default
     */
    protected double getParamWithDefault(StrategyContext context, String key, double defaultValue) {
        try {
            return context.getParamAsDouble(key);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Get parameter with default value (int).
     *
     * @param context Strategy context
     * @param key Parameter key
     * @param defaultValue Default value if parameter not found
     * @return Parameter value or default
     */
    protected int getParamWithDefault(StrategyContext context, String key, int defaultValue) {
        try {
            return context.getParamAsInt(key);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Get parameter with default value (boolean).
     *
     * @param context Strategy context
     * @param key Parameter key
     * @param defaultValue Default value if parameter not found
     * @return Parameter value or default
     */
    protected boolean getParamWithDefault(StrategyContext context, String key, boolean defaultValue) {
        try {
            return context.getParamAsBoolean(key);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Abstract method for strategy-specific evaluation logic.
     * Subclasses implement their trading rules here.
     *
     * @param context Strategy context with market data and parameters
     * @return Signal decision (BUY/SELL/HOLD)
     */
    @Override
    public abstract SignalDecision evaluate(StrategyContext context);

    /**
     * Abstract method to get strategy type identifier.
     *
     * @return Strategy type string (e.g., "MA_CROSSOVER", "RSI")
     */
    @Override
    public abstract String getStrategyType();
}
