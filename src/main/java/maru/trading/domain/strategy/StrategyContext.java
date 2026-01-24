package maru.trading.domain.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import maru.trading.domain.market.MarketBar;

import java.util.List;
import java.util.Map;

/**
 * Strategy execution context.
 *
 * Contains all market data and strategy parameters needed for
 * a strategy to make a trading decision.
 *
 * Immutable value object - all collections should be unmodifiable.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyContext {

    /**
     * Strategy ID (for logging and tracking).
     */
    private String strategyId;

    /**
     * Symbol being evaluated (e.g., "005930").
     */
    private String symbol;

    /**
     * Account ID (for position/risk context).
     */
    private String accountId;

    /**
     * Historical market bars (OHLCV data).
     * Ordered from oldest to newest.
     * Minimum size depends on strategy requirements (e.g., MA period).
     */
    private List<MarketBar> bars;

    /**
     * Strategy-specific parameters (e.g., {"shortPeriod": 5, "longPeriod": 20}).
     * Parsed from Strategy.paramsJson.
     */
    private Map<String, Object> params;

    /**
     * Current timeframe (e.g., "1m", "5m", "1d").
     */
    private String timeframe;

    /**
     * Validate context data.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (strategyId == null || strategyId.isBlank()) {
            throw new IllegalArgumentException("Strategy ID cannot be null or blank");
        }
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (bars == null || bars.isEmpty()) {
            throw new IllegalArgumentException("Bars cannot be null or empty");
        }
        if (params == null) {
            throw new IllegalArgumentException("Params cannot be null");
        }
        if (timeframe == null || timeframe.isBlank()) {
            throw new IllegalArgumentException("Timeframe cannot be null or blank");
        }
    }

    /**
     * Get the number of available bars.
     */
    public int getBarCount() {
        return bars != null ? bars.size() : 0;
    }

    /**
     * Get the most recent bar (last closed bar).
     * Returns null if no bars available.
     */
    public MarketBar getLatestBar() {
        if (bars == null || bars.isEmpty()) {
            return null;
        }
        return bars.get(bars.size() - 1);
    }

    /**
     * Get parameter as Integer.
     * Throws IllegalArgumentException if parameter is missing or not convertible.
     */
    public Integer getParamAsInt(String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("Parameter " + key + " is not an integer: " + value);
    }

    /**
     * Get parameter as Double.
     * Throws IllegalArgumentException if parameter is missing or not convertible.
     */
    public Double getParamAsDouble(String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Parameter " + key + " is not a double: " + value);
    }

    /**
     * Get parameter as Boolean.
     * Throws IllegalArgumentException if parameter is missing or not convertible.
     */
    public Boolean getParamAsBoolean(String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new IllegalArgumentException("Parameter " + key + " is not a boolean: " + value);
    }
}
