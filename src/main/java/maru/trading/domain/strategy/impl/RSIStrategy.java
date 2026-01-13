package maru.trading.domain.strategy.impl;

import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.strategy.IndicatorLibrary;
import maru.trading.domain.strategy.StrategyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * RSI (Relative Strength Index) Strategy.
 *
 * Strategy Logic:
 * - BUY when RSI crosses below oversoldThreshold (oversold)
 * - SELL when RSI crosses above overboughtThreshold (overbought)
 * - HOLD otherwise
 *
 * Parameters:
 * - period: RSI period (typically 14)
 * - overboughtThreshold: RSI value considered overbought (e.g., 70)
 * - oversoldThreshold: RSI value considered oversold (e.g., 30)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: period + 2 (to detect crossover)
 */
public class RSIStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(RSIStrategy.class);
    private static final String STRATEGY_TYPE = "RSI";

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters
        int period = context.getParamAsInt("period");
        double overboughtThreshold = context.getParamAsDouble("overboughtThreshold");
        double oversoldThreshold = context.getParamAsDouble("oversoldThreshold");
        int ttlSeconds = getTtlSeconds(context);

        // Validate parameters
        if (oversoldThreshold >= overboughtThreshold) {
            throw new IllegalArgumentException(
                    "Oversold threshold must be less than overbought threshold: oversold=" +
                            oversoldThreshold + ", overbought=" + overboughtThreshold);
        }

        // Validate minimum bars
        int minBars = period + 2; // Need period+1 for RSI, +1 more for crossover detection
        validateMinimumBars(context, minBars);

        // Extract close prices
        List<BigDecimal> closePrices = extractClosePrices(context.getBars());

        // Calculate RSI
        List<BigDecimal> rsiValues = IndicatorLibrary.calculateRSI(closePrices, period);

        // Get latest and previous RSI values
        BigDecimal rsiNow = rsiValues.get(rsiValues.size() - 1);
        BigDecimal rsiPrev = rsiValues.get(rsiValues.size() - 2);

        log.debug("RSI evaluation: symbol={}, RSI(now)={}, RSI(prev)={}, overbought={}, oversold={}",
                context.getSymbol(), rsiNow, rsiPrev, overboughtThreshold, oversoldThreshold);

        // Detect crossovers
        boolean oversoldCrossover = rsiPrev.doubleValue() >= oversoldThreshold &&
                rsiNow.doubleValue() < oversoldThreshold;

        boolean overboughtCrossover = rsiPrev.doubleValue() <= overboughtThreshold &&
                rsiNow.doubleValue() > overboughtThreshold;

        if (oversoldCrossover) {
            String reason = String.format("RSI Oversold: RSI(%.2f) crossed below threshold(%.2f)",
                    rsiNow, oversoldThreshold);
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        if (overboughtCrossover) {
            String reason = String.format("RSI Overbought: RSI(%.2f) crossed above threshold(%.2f)",
                    rsiNow, overboughtThreshold);
            log.info("SELL signal generated: {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // No crossover detected
        String reason = String.format("RSI Neutral: RSI=%.2f (oversold=%.2f, overbought=%.2f)",
                rsiNow, oversoldThreshold, overboughtThreshold);
        log.debug("HOLD signal: {}", reason);
        return SignalDecision.hold(reason);
    }

    @Override
    public String getStrategyType() {
        return STRATEGY_TYPE;
    }

    @Override
    public void validateParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("RSI strategy requires parameters");
        }

        // Validate required parameters
        if (!params.containsKey("period")) {
            throw new IllegalArgumentException("Missing required parameter: period");
        }
        if (!params.containsKey("overboughtThreshold")) {
            throw new IllegalArgumentException("Missing required parameter: overboughtThreshold");
        }
        if (!params.containsKey("oversoldThreshold")) {
            throw new IllegalArgumentException("Missing required parameter: oversoldThreshold");
        }

        // Validate parameter types and values
        Object periodObj = params.get("period");
        Object overboughtObj = params.get("overboughtThreshold");
        Object oversoldObj = params.get("oversoldThreshold");

        if (!(periodObj instanceof Number)) {
            throw new IllegalArgumentException("period must be a number");
        }
        if (!(overboughtObj instanceof Number)) {
            throw new IllegalArgumentException("overboughtThreshold must be a number");
        }
        if (!(oversoldObj instanceof Number)) {
            throw new IllegalArgumentException("oversoldThreshold must be a number");
        }

        int period = ((Number) periodObj).intValue();
        double overbought = ((Number) overboughtObj).doubleValue();
        double oversold = ((Number) oversoldObj).doubleValue();

        if (period <= 0) {
            throw new IllegalArgumentException("period must be positive: " + period);
        }
        if (overbought < 0 || overbought > 100) {
            throw new IllegalArgumentException("overboughtThreshold must be between 0 and 100: " + overbought);
        }
        if (oversold < 0 || oversold > 100) {
            throw new IllegalArgumentException("oversoldThreshold must be between 0 and 100: " + oversold);
        }
        if (oversold >= overbought) {
            throw new IllegalArgumentException(
                    "oversoldThreshold must be less than overboughtThreshold: oversold=" + oversold +
                            ", overbought=" + overbought);
        }

        log.info("RSI strategy params validated: period={}, overbought={}, oversold={}",
                period, overbought, oversold);
    }
}
