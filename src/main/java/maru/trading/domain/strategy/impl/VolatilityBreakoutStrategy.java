package maru.trading.domain.strategy.impl;

import maru.trading.domain.market.MarketBar;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.strategy.IndicatorLibrary;
import maru.trading.domain.strategy.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Larry Williams Volatility Breakout Strategy.
 *
 * Strategy Logic:
 * - Target Price = Today's Open + (Yesterday's Range × K)
 * - BUY when current price >= target price
 * - SELL when exitBeforeClose=true and isExitTime=true (close before market end)
 * - HOLD otherwise
 *
 * Parameters:
 * - kFactor: K constant (0.0 to 1.0, default 0.5)
 * - exitBeforeClose: Whether to close position before market close (default true)
 * - isExitTime: External signal indicating exit time has arrived (default false)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: 2 (today + yesterday)
 */
public class VolatilityBreakoutStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(VolatilityBreakoutStrategy.class);
    private static final String STRATEGY_TYPE = "VOLATILITY_BREAKOUT";
    private static final int MIN_BARS = 2;
    private static final double DEFAULT_K_FACTOR = 0.5;

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters with defaults
        double kFactor = getParamWithDefault(context, "kFactor", DEFAULT_K_FACTOR);
        boolean exitBeforeClose = getParamWithDefault(context, "exitBeforeClose", true);
        boolean isExitTime = getParamWithDefault(context, "isExitTime", false);
        int ttlSeconds = getTtlSeconds(context);

        // Validate K factor
        if (kFactor < 0 || kFactor > 1) {
            throw new IllegalArgumentException("kFactor must be between 0 and 1: " + kFactor);
        }

        // Validate minimum bars
        validateMinimumBars(context, MIN_BARS);

        // Extract bar data
        List<MarketBar> bars = context.getBars();
        MarketBar todayBar = bars.get(bars.size() - 1);      // Most recent bar (today)
        MarketBar yesterdayBar = bars.get(bars.size() - 2);  // Previous bar (yesterday)

        // Get prices
        BigDecimal todayOpen = todayBar.getOpen();
        BigDecimal currentPrice = todayBar.getClose();
        BigDecimal yesterdayHigh = yesterdayBar.getHigh();
        BigDecimal yesterdayLow = yesterdayBar.getLow();

        // Calculate target price
        BigDecimal targetPrice = IndicatorLibrary.calculateBreakoutTarget(
                todayOpen, yesterdayHigh, yesterdayLow, kFactor);

        log.debug("Volatility Breakout evaluation: symbol={}, currentPrice={}, targetPrice={}, todayOpen={}, " +
                        "yesterdayRange={}, K={}, isExitTime={}",
                context.getSymbol(), currentPrice, targetPrice, todayOpen,
                yesterdayHigh.subtract(yesterdayLow), kFactor, isExitTime);

        // Check exit condition first (priority for risk management)
        if (exitBeforeClose && isExitTime) {
            String reason = String.format("Exit before market close: exitBeforeClose=%b, isExitTime=%b",
                    exitBeforeClose, isExitTime);
            log.info("SELL signal generated (exit): {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // Check breakout condition
        if (currentPrice.compareTo(targetPrice) >= 0) {
            String reason = String.format("Breakout detected: price=%.2f >= target=%.2f (open=%.2f + range*%.2f)",
                    currentPrice, targetPrice, todayOpen, kFactor);
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // No signal
        String reason = String.format("No breakout: price=%.2f < target=%.2f",
                currentPrice, targetPrice);
        log.debug("HOLD signal: {}", reason);
        return SignalDecision.hold(reason);
    }

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public void validateParams(Map<String, Object> params) {
        if (params == null) {
            throw new IllegalArgumentException("Volatility Breakout strategy params cannot be null");
        }

        // kFactor is optional, validate if present
        if (params.containsKey("kFactor")) {
            Object k = params.get("kFactor");
            if (!(k instanceof Number)) {
                throw new IllegalArgumentException("kFactor must be a number");
            }
            double kValue = ((Number) k).doubleValue();
            if (kValue < 0 || kValue > 1) {
                throw new IllegalArgumentException("kFactor must be between 0 and 1: " + kValue);
            }
        }

        // exitBeforeClose is optional, validate if present
        if (params.containsKey("exitBeforeClose")) {
            Object exit = params.get("exitBeforeClose");
            if (!(exit instanceof Boolean) && !(exit instanceof String)) {
                throw new IllegalArgumentException("exitBeforeClose must be a boolean");
            }
        }

        // isExitTime is optional, validate if present
        if (params.containsKey("isExitTime")) {
            Object exitTime = params.get("isExitTime");
            if (!(exitTime instanceof Boolean) && !(exitTime instanceof String)) {
                throw new IllegalArgumentException("isExitTime must be a boolean");
            }
        }

        // ttlSeconds is optional, validate if present
        if (params.containsKey("ttlSeconds")) {
            Object ttl = params.get("ttlSeconds");
            if (!(ttl instanceof Number)) {
                throw new IllegalArgumentException("ttlSeconds must be a number");
            }
            int ttlValue = ((Number) ttl).intValue();
            if (ttlValue <= 0) {
                throw new IllegalArgumentException("ttlSeconds must be positive: " + ttlValue);
            }
        }

        log.info("Volatility Breakout strategy params validated: kFactor={}, exitBeforeClose={}, ttlSeconds={}",
                params.getOrDefault("kFactor", DEFAULT_K_FACTOR),
                params.getOrDefault("exitBeforeClose", true),
                params.getOrDefault("ttlSeconds", DEFAULT_TTL_SECONDS));
    }
}
