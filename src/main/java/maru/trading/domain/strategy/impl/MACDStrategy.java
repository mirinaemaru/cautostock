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
 * MACD (Moving Average Convergence Divergence) Strategy.
 *
 * Strategy Logic:
 * - BUY when MACD line crosses above signal line (bullish crossover)
 * - SELL when MACD line crosses below signal line (bearish crossover)
 * - HOLD otherwise
 *
 * Parameters:
 * - fastPeriod: Fast EMA period (typically 12)
 * - slowPeriod: Slow EMA period (typically 26)
 * - signalPeriod: Signal line EMA period (typically 9)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: slowPeriod + signalPeriod (to detect crossover)
 */
public class MACDStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(MACDStrategy.class);
    private static final String STRATEGY_TYPE = "MACD";

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters
        int fastPeriod = context.getParamAsInt("fastPeriod");
        int slowPeriod = context.getParamAsInt("slowPeriod");
        int signalPeriod = context.getParamAsInt("signalPeriod");
        int ttlSeconds = getTtlSeconds(context);

        // Validate parameters
        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException("All periods must be positive");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException(
                    "Fast period must be less than slow period: fast=" + fastPeriod + ", slow=" + slowPeriod);
        }

        // Validate minimum bars (need extra bar to detect crossover)
        int minBars = slowPeriod + signalPeriod + 1;
        validateMinimumBars(context, minBars);

        // Extract close prices
        List<BigDecimal> closePrices = extractClosePrices(context.getBars());

        // Calculate MACD
        List<IndicatorLibrary.MACD> macdList =
                IndicatorLibrary.calculateMACD(closePrices, fastPeriod, slowPeriod, signalPeriod);

        // Get latest and previous MACD values
        IndicatorLibrary.MACD macdNow = macdList.get(macdList.size() - 1);
        IndicatorLibrary.MACD macdPrev = macdList.get(macdList.size() - 2);

        log.debug("MACD evaluation: symbol={}, macd(now)={}, signal(now)={}, macd(prev)={}, signal(prev)={}",
                context.getSymbol(),
                macdNow.getMacdLine(), macdNow.getSignalLine(),
                macdPrev.getMacdLine(), macdPrev.getSignalLine());

        // Detect crossover
        boolean bullishCrossover =
                macdPrev.getMacdLine().compareTo(macdPrev.getSignalLine()) <= 0 &&
                macdNow.getMacdLine().compareTo(macdNow.getSignalLine()) > 0;

        boolean bearishCrossover =
                macdPrev.getMacdLine().compareTo(macdPrev.getSignalLine()) >= 0 &&
                macdNow.getMacdLine().compareTo(macdNow.getSignalLine()) < 0;

        if (bullishCrossover) {
            String reason = String.format("MACD bullish crossover: MACD=%.4f crossed above Signal=%.4f",
                    macdNow.getMacdLine(), macdNow.getSignalLine());
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        if (bearishCrossover) {
            String reason = String.format("MACD bearish crossover: MACD=%.4f crossed below Signal=%.4f",
                    macdNow.getMacdLine(), macdNow.getSignalLine());
            log.info("SELL signal generated: {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // No crossover detected
        String reason = String.format("No MACD crossover: MACD=%.4f, Signal=%.4f, Histogram=%.4f",
                macdNow.getMacdLine(), macdNow.getSignalLine(), macdNow.getHistogram());
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
            throw new IllegalArgumentException("MACD strategy requires parameters");
        }

        // Validate required parameters
        if (!params.containsKey("fastPeriod")) {
            throw new IllegalArgumentException("Missing required parameter: fastPeriod");
        }
        if (!params.containsKey("slowPeriod")) {
            throw new IllegalArgumentException("Missing required parameter: slowPeriod");
        }
        if (!params.containsKey("signalPeriod")) {
            throw new IllegalArgumentException("Missing required parameter: signalPeriod");
        }

        // Validate parameter values
        Object fastPeriodObj = params.get("fastPeriod");
        Object slowPeriodObj = params.get("slowPeriod");
        Object signalPeriodObj = params.get("signalPeriod");

        if (!(fastPeriodObj instanceof Number)) {
            throw new IllegalArgumentException("fastPeriod must be a number");
        }
        if (!(slowPeriodObj instanceof Number)) {
            throw new IllegalArgumentException("slowPeriod must be a number");
        }
        if (!(signalPeriodObj instanceof Number)) {
            throw new IllegalArgumentException("signalPeriod must be a number");
        }

        int fastPeriod = ((Number) fastPeriodObj).intValue();
        int slowPeriod = ((Number) slowPeriodObj).intValue();
        int signalPeriod = ((Number) signalPeriodObj).intValue();

        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException("All periods must be positive");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException(
                    "fastPeriod must be less than slowPeriod: fast=" + fastPeriod + ", slow=" + slowPeriod);
        }

        log.info("MACD strategy params validated: fastPeriod={}, slowPeriod={}, signalPeriod={}",
                fastPeriod, slowPeriod, signalPeriod);
    }
}
