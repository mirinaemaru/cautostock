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
 * Bollinger Bands Strategy.
 *
 * Strategy Logic:
 * - BUY when price touches or falls below lower band (oversold)
 * - SELL when price touches or rises above upper band (overbought)
 * - HOLD when price is within the bands
 *
 * Parameters:
 * - period: Bollinger Bands period (typically 20)
 * - stdDevMultiplier: Standard deviation multiplier (typically 2.0)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: period + 1 (to detect band touch)
 */
public class BollingerBandsStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(BollingerBandsStrategy.class);
    private static final String STRATEGY_TYPE = "BOLLINGER_BANDS";

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters
        int period = context.getParamAsInt("period");
        double stdDevMultiplier = context.getParamAsDouble("stdDevMultiplier");
        int ttlSeconds = getTtlSeconds(context);

        // Validate parameters
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive: " + period);
        }
        if (stdDevMultiplier <= 0) {
            throw new IllegalArgumentException("Standard deviation multiplier must be positive: " + stdDevMultiplier);
        }

        // Validate minimum bars
        int minBars = period + 1; // Need extra bar to detect band touch
        validateMinimumBars(context, minBars);

        // Extract close prices
        List<BigDecimal> closePrices = extractClosePrices(context.getBars());

        // Calculate Bollinger Bands
        List<IndicatorLibrary.BollingerBands> bbList =
                IndicatorLibrary.calculateBollingerBands(closePrices, period, stdDevMultiplier);

        // Get latest BB and price
        IndicatorLibrary.BollingerBands bbNow = bbList.get(bbList.size() - 1);
        BigDecimal priceNow = closePrices.get(closePrices.size() - 1);

        log.debug("Bollinger Bands evaluation: symbol={}, price={}, upper={}, middle={}, lower={}",
                context.getSymbol(), priceNow, bbNow.getUpper(), bbNow.getMiddle(), bbNow.getLower());

        // BUY signal: Price at or below lower band (oversold)
        if (priceNow.compareTo(bbNow.getLower()) <= 0) {
            String reason = String.format("Price %.2f at/below lower band %.2f (oversold)",
                    priceNow, bbNow.getLower());
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // SELL signal: Price at or above upper band (overbought)
        if (priceNow.compareTo(bbNow.getUpper()) >= 0) {
            String reason = String.format("Price %.2f at/above upper band %.2f (overbought)",
                    priceNow, bbNow.getUpper());
            log.info("SELL signal generated: {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // HOLD: Price within bands
        String reason = String.format("Price %.2f within bands [%.2f, %.2f]",
                priceNow, bbNow.getLower(), bbNow.getUpper());
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
            throw new IllegalArgumentException("Bollinger Bands strategy requires parameters");
        }

        // Validate required parameters
        if (!params.containsKey("period")) {
            throw new IllegalArgumentException("Missing required parameter: period");
        }
        if (!params.containsKey("stdDevMultiplier")) {
            throw new IllegalArgumentException("Missing required parameter: stdDevMultiplier");
        }

        // Validate parameter values
        Object periodObj = params.get("period");
        Object stdDevMultiplierObj = params.get("stdDevMultiplier");

        if (!(periodObj instanceof Number)) {
            throw new IllegalArgumentException("period must be a number");
        }
        if (!(stdDevMultiplierObj instanceof Number)) {
            throw new IllegalArgumentException("stdDevMultiplier must be a number");
        }

        int period = ((Number) periodObj).intValue();
        double stdDevMultiplier = ((Number) stdDevMultiplierObj).doubleValue();

        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive: " + period);
        }
        if (stdDevMultiplier <= 0) {
            throw new IllegalArgumentException("Standard deviation multiplier must be positive: " + stdDevMultiplier);
        }

        log.info("Bollinger Bands strategy params validated: period={}, stdDevMultiplier={}",
                period, stdDevMultiplier);
    }
}
