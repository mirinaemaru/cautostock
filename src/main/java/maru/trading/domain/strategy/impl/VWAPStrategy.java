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
 * VWAP (Volume Weighted Average Price) Crossover Strategy.
 *
 * Strategy Logic:
 * - BUY when price crosses above VWAP (upward crossover)
 * - SELL when price crosses below VWAP (downward crossover)
 * - HOLD otherwise
 *
 * Parameters:
 * - crossoverThreshold: Crossover sensitivity percentage (default 0.0)
 * - ttlSeconds: Signal TTL (optional, default 300)
 *
 * Minimum bars required: 2 (to detect crossover)
 */
public class VWAPStrategy extends BaseStrategy {

    private static final Logger log = LoggerFactory.getLogger(VWAPStrategy.class);
    private static final String STRATEGY_TYPE = "VWAP";
    private static final int MIN_BARS = 2;

    @Override
    public SignalDecision evaluate(StrategyContext context) {
        // Validate context
        context.validate();

        // Extract parameters with defaults
        double crossoverThreshold = getParamWithDefault(context, "crossoverThreshold", 0.0);
        int ttlSeconds = getTtlSeconds(context);

        // Validate minimum bars
        validateMinimumBars(context, MIN_BARS);

        // Extract OHLCV data
        List<MarketBar> bars = context.getBars();
        List<BigDecimal> highs = extractHighPrices(bars);
        List<BigDecimal> lows = extractLowPrices(bars);
        List<BigDecimal> closes = extractClosePrices(bars);
        List<Long> volumes = extractVolumes(bars);

        // Calculate VWAP
        List<IndicatorLibrary.VWAPResult> vwapResults = IndicatorLibrary.calculateVWAP(highs, lows, closes, volumes);

        // Get current and previous values
        BigDecimal priceNow = closes.get(closes.size() - 1);
        BigDecimal pricePrev = closes.get(closes.size() - 2);
        BigDecimal vwapNow = vwapResults.get(vwapResults.size() - 1).getVwap();
        BigDecimal vwapPrev = vwapResults.get(vwapResults.size() - 2).getVwap();

        // Apply threshold for crossover detection
        BigDecimal thresholdMultiplier = BigDecimal.ONE.add(BigDecimal.valueOf(crossoverThreshold / 100.0));
        BigDecimal vwapNowWithThreshold = vwapNow.multiply(thresholdMultiplier);
        BigDecimal vwapPrevWithThreshold = vwapPrev.multiply(thresholdMultiplier);
        BigDecimal vwapNowBelowThreshold = vwapNow.divide(thresholdMultiplier, 8, java.math.RoundingMode.HALF_UP);
        BigDecimal vwapPrevBelowThreshold = vwapPrev.divide(thresholdMultiplier, 8, java.math.RoundingMode.HALF_UP);

        log.debug("VWAP evaluation: symbol={}, price(now)={}, VWAP(now)={}, price(prev)={}, VWAP(prev)={}, threshold={}%",
                context.getSymbol(), priceNow, vwapNow, pricePrev, vwapPrev, crossoverThreshold);

        // Detect upward crossover (price crosses above VWAP)
        boolean upwardCross = pricePrev.compareTo(vwapPrevWithThreshold) <= 0 && priceNow.compareTo(vwapNowWithThreshold) > 0;

        // Detect downward crossover (price crosses below VWAP)
        boolean downwardCross = pricePrev.compareTo(vwapPrevBelowThreshold) >= 0 && priceNow.compareTo(vwapNowBelowThreshold) < 0;

        if (upwardCross) {
            String reason = String.format("Price crossed above VWAP: price=%.2f > VWAP=%.2f",
                    priceNow, vwapNow);
            log.info("BUY signal generated: {}", reason);
            return SignalDecision.buy(getDefaultQuantity(context), reason, ttlSeconds);
        }

        if (downwardCross) {
            String reason = String.format("Price crossed below VWAP: price=%.2f < VWAP=%.2f",
                    priceNow, vwapNow);
            log.info("SELL signal generated: {}", reason);
            return SignalDecision.sell(getDefaultQuantity(context), reason, ttlSeconds);
        }

        // No crossover detected
        String reason = String.format("No VWAP crossover: price=%.2f, VWAP=%.2f",
                priceNow, vwapNow);
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
            throw new IllegalArgumentException("VWAP strategy params cannot be null");
        }

        // crossoverThreshold is optional, validate if present
        if (params.containsKey("crossoverThreshold")) {
            Object threshold = params.get("crossoverThreshold");
            if (!(threshold instanceof Number)) {
                throw new IllegalArgumentException("crossoverThreshold must be a number");
            }
            double thresholdValue = ((Number) threshold).doubleValue();
            if (thresholdValue < 0) {
                throw new IllegalArgumentException("crossoverThreshold cannot be negative: " + thresholdValue);
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

        log.info("VWAP strategy params validated: crossoverThreshold={}, ttlSeconds={}",
                params.getOrDefault("crossoverThreshold", 0.0),
                params.getOrDefault("ttlSeconds", DEFAULT_TTL_SECONDS));
    }
}
