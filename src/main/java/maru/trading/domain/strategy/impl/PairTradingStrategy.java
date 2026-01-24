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
 * Pair Trading Strategy (Single Asset Mean Reversion).
 *
 * Uses Z-Score to identify mean reversion opportunities:
 * - Z-Score = (Current Price - Moving Average) / Standard Deviation
 *
 * Strategy Logic:
 * - BUY when Z-Score < -entryZScore (price significantly below mean, expect rise)
 * - SELL when Z-Score > +entryZScore (price significantly above mean, expect fall)
 * - HOLD when |Z-Score| <= exitZScore or between thresholds
 *
 * Parameters:
 * - lookbackPeriod: Period for mean and std dev calculation (default 20)
 * - entryZScore: Entry threshold for Z-Score (default 2.0)
 * - exitZScore: Exit threshold for Z-Score (default 0.5)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: lookbackPeriod
 */
public class PairTradingStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(PairTradingStrategy.class);
    private static final String STRATEGY_TYPE = "PAIR_TRADING";
    private static final int DEFAULT_LOOKBACK_PERIOD = 20;
    private static final double DEFAULT_ENTRY_ZSCORE = 2.0;
    private static final double DEFAULT_EXIT_ZSCORE = 0.5;

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters with defaults
        int lookbackPeriod = getParamWithDefault(context, "lookbackPeriod", DEFAULT_LOOKBACK_PERIOD);
        double entryZScore = getParamWithDefault(context, "entryZScore", DEFAULT_ENTRY_ZSCORE);
        double exitZScore = getParamWithDefault(context, "exitZScore", DEFAULT_EXIT_ZSCORE);
        int ttlSeconds = getTtlSeconds(context);

        // Validate parameters
        if (lookbackPeriod <= 0) {
            throw new IllegalArgumentException("lookbackPeriod must be positive: " + lookbackPeriod);
        }
        if (entryZScore <= 0) {
            throw new IllegalArgumentException("entryZScore must be positive: " + entryZScore);
        }
        if (exitZScore < 0) {
            throw new IllegalArgumentException("exitZScore cannot be negative: " + exitZScore);
        }
        if (exitZScore >= entryZScore) {
            throw new IllegalArgumentException(
                    "exitZScore must be less than entryZScore: exit=" + exitZScore + ", entry=" + entryZScore);
        }

        // Validate minimum bars
        validateMinimumBars(context, lookbackPeriod);

        // Extract close prices
        List<BigDecimal> closePrices = extractClosePrices(context.getBars());

        // Calculate spread and Z-Score
        IndicatorLibrary.SpreadResult spreadResult = IndicatorLibrary.calculateSimpleSpread(closePrices, lookbackPeriod);

        BigDecimal currentPrice = closePrices.get(closePrices.size() - 1);
        BigDecimal zScore = spreadResult.getZScore();
        BigDecimal mean = spreadResult.getMeanSpread();
        BigDecimal stdDev = spreadResult.getStdDev();

        log.debug("Pair Trading evaluation: symbol={}, price={}, mean={}, stdDev={}, zScore={}, " +
                        "entryThreshold={}, exitThreshold={}",
                context.getSymbol(), currentPrice, mean, stdDev, zScore, entryZScore, exitZScore);

        // BUY signal: Z-Score below negative entry threshold (price too low, expect mean reversion up)
        if (zScore.doubleValue() < -entryZScore) {
            String reason = String.format("Mean reversion BUY: Z-Score=%.4f < -%.2f (price=%.2f, mean=%.2f, stdDev=%.4f)",
                    zScore, entryZScore, currentPrice, mean, stdDev);
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // SELL signal: Z-Score above positive entry threshold (price too high, expect mean reversion down)
        if (zScore.doubleValue() > entryZScore) {
            String reason = String.format("Mean reversion SELL: Z-Score=%.4f > +%.2f (price=%.2f, mean=%.2f, stdDev=%.4f)",
                    zScore, entryZScore, currentPrice, mean, stdDev);
            log.info("SELL signal generated: {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // HOLD: Z-Score within thresholds
        String reason = String.format("No mean reversion signal: Z-Score=%.4f (threshold: +/-%.2f)",
                zScore, entryZScore);
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
            throw new IllegalArgumentException("Pair Trading strategy params cannot be null");
        }

        // lookbackPeriod is optional, validate if present
        if (params.containsKey("lookbackPeriod")) {
            Object period = params.get("lookbackPeriod");
            if (!(period instanceof Number)) {
                throw new IllegalArgumentException("lookbackPeriod must be a number");
            }
            int periodValue = ((Number) period).intValue();
            if (periodValue <= 0) {
                throw new IllegalArgumentException("lookbackPeriod must be positive: " + periodValue);
            }
        }

        // entryZScore is optional, validate if present
        if (params.containsKey("entryZScore")) {
            Object entry = params.get("entryZScore");
            if (!(entry instanceof Number)) {
                throw new IllegalArgumentException("entryZScore must be a number");
            }
            double entryValue = ((Number) entry).doubleValue();
            if (entryValue <= 0) {
                throw new IllegalArgumentException("entryZScore must be positive: " + entryValue);
            }
        }

        // exitZScore is optional, validate if present
        if (params.containsKey("exitZScore")) {
            Object exit = params.get("exitZScore");
            if (!(exit instanceof Number)) {
                throw new IllegalArgumentException("exitZScore must be a number");
            }
            double exitValue = ((Number) exit).doubleValue();
            if (exitValue < 0) {
                throw new IllegalArgumentException("exitZScore cannot be negative: " + exitValue);
            }
        }

        // Validate entry > exit if both present
        if (params.containsKey("entryZScore") && params.containsKey("exitZScore")) {
            double entryValue = ((Number) params.get("entryZScore")).doubleValue();
            double exitValue = ((Number) params.get("exitZScore")).doubleValue();
            if (exitValue >= entryValue) {
                throw new IllegalArgumentException(
                        "exitZScore must be less than entryZScore: exit=" + exitValue + ", entry=" + entryValue);
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

        log.info("Pair Trading strategy params validated: lookbackPeriod={}, entryZScore={}, exitZScore={}, ttlSeconds={}",
                params.getOrDefault("lookbackPeriod", DEFAULT_LOOKBACK_PERIOD),
                params.getOrDefault("entryZScore", DEFAULT_ENTRY_ZSCORE),
                params.getOrDefault("exitZScore", DEFAULT_EXIT_ZSCORE),
                params.getOrDefault("ttlSeconds", DEFAULT_TTL_SECONDS));
    }
}
