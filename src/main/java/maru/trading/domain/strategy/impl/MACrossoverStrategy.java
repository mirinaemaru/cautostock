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
 * Moving Average Crossover Strategy.
 *
 * Strategy Logic:
 * - BUY when short MA crosses above long MA (golden cross)
 * - SELL when short MA crosses below long MA (death cross)
 * - HOLD otherwise
 *
 * Parameters:
 * - shortPeriod: Period for short MA (e.g., 5)
 * - longPeriod: Period for long MA (e.g., 20)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: longPeriod + 1 (to detect crossover)
 */
public class MACrossoverStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(MACrossoverStrategy.class);
    private static final String STRATEGY_TYPE = "MA_CROSSOVER";

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters
        int shortPeriod = context.getParamAsInt("shortPeriod");
        int longPeriod = context.getParamAsInt("longPeriod");
        int ttlSeconds = getTtlSeconds(context);

        // Validate parameters
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException(
                    "Short period must be less than long period: short=" + shortPeriod + ", long=" + longPeriod);
        }

        // Validate minimum bars
        int minBars = longPeriod + 1; // Need extra bar to detect crossover
        validateMinimumBars(context, minBars);

        // Extract close prices
        List<BigDecimal> closePrices = extractClosePrices(context.getBars());

        // Calculate MAs
        List<BigDecimal> shortMA = IndicatorLibrary.calculateMA(closePrices, shortPeriod);
        List<BigDecimal> longMA = IndicatorLibrary.calculateMA(closePrices, longPeriod);

        // Get latest and previous MA values
        BigDecimal shortMANow = shortMA.get(shortMA.size() - 1);
        BigDecimal longMANow = longMA.get(longMA.size() - 1);
        BigDecimal shortMAPrev = shortMA.get(shortMA.size() - 2);
        BigDecimal longMAPrev = longMA.get(longMA.size() - 2);

        log.debug("MA Crossover evaluation: symbol={}, shortMA(now)={}, longMA(now)={}, shortMA(prev)={}, longMA(prev)={}",
                context.getSymbol(), shortMANow, longMANow, shortMAPrev, longMAPrev);

        // Detect crossover
        boolean goldenCross = shortMAPrev.compareTo(longMAPrev) <= 0 && shortMANow.compareTo(longMANow) > 0;
        boolean deathCross = shortMAPrev.compareTo(longMAPrev) >= 0 && shortMANow.compareTo(longMANow) < 0;

        if (goldenCross) {
            String reason = String.format("Golden Cross: MA(%d)=%.2f crossed above MA(%d)=%.2f",
                    shortPeriod, shortMANow, longPeriod, longMANow);
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        if (deathCross) {
            String reason = String.format("Death Cross: MA(%d)=%.2f crossed below MA(%d)=%.2f",
                    shortPeriod, shortMANow, longPeriod, longMANow);
            log.info("SELL signal generated: {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // No crossover detected
        String reason = String.format("No crossover: MA(%d)=%.2f, MA(%d)=%.2f",
                shortPeriod, shortMANow, longPeriod, longMANow);
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
            throw new IllegalArgumentException("MA Crossover strategy requires parameters");
        }

        // Validate required parameters
        if (!params.containsKey("shortPeriod")) {
            throw new IllegalArgumentException("Missing required parameter: shortPeriod");
        }
        if (!params.containsKey("longPeriod")) {
            throw new IllegalArgumentException("Missing required parameter: longPeriod");
        }

        // Validate parameter values
        Object shortPeriodObj = params.get("shortPeriod");
        Object longPeriodObj = params.get("longPeriod");

        if (!(shortPeriodObj instanceof Number)) {
            throw new IllegalArgumentException("shortPeriod must be a number");
        }
        if (!(longPeriodObj instanceof Number)) {
            throw new IllegalArgumentException("longPeriod must be a number");
        }

        int shortPeriod = ((Number) shortPeriodObj).intValue();
        int longPeriod = ((Number) longPeriodObj).intValue();

        if (shortPeriod <= 0 || longPeriod <= 0) {
            throw new IllegalArgumentException("MA periods must be positive");
        }
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException(
                    "shortPeriod must be less than longPeriod: short=" + shortPeriod + ", long=" + longPeriod);
        }

        log.info("MA Crossover strategy params validated: shortPeriod={}, longPeriod={}", shortPeriod, longPeriod);
    }
}
