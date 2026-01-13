package maru.trading.application.usecase.strategy;

import maru.trading.application.ports.repo.BarRepository;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyContext;
import maru.trading.domain.strategy.StrategyVersion;
import maru.trading.infra.cache.BarCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Use case for loading strategy execution context.
 *
 * Loads:
 * 1. Strategy parameters from active version
 * 2. Recent market bars for the symbol
 * 3. Constructs StrategyContext for strategy evaluation
 */
@Service
public class LoadStrategyContextUseCase {

    private static final Logger log = LoggerFactory.getLogger(LoadStrategyContextUseCase.class);
    private static final String DEFAULT_TIMEFRAME = "1m";
    private static final int DEFAULT_BAR_LOOKBACK = 100; // Load last 100 bars

    private final BarRepository barRepository;
    private final BarCache barCache;

    public LoadStrategyContextUseCase(BarRepository barRepository, BarCache barCache) {
        this.barRepository = barRepository;
        this.barCache = barCache;
    }

    /**
     * Execute the use case.
     *
     * @param strategy Strategy configuration
     * @param version Strategy version with parameters
     * @param symbol Symbol to evaluate
     * @param accountId Account ID
     * @return StrategyContext for strategy evaluation
     */
    public StrategyContext execute(
            Strategy strategy,
            StrategyVersion version,
            String symbol,
            String accountId) {

        log.debug("Loading strategy context: strategyId={}, symbol={}, versionNo={}",
                strategy.getStrategyId(), symbol, version.getVersionNo());

        // Step 1: Parse strategy parameters
        Map<String, Object> params = version.getParamsAsMap();

        // Step 2: Determine required bar count from parameters
        int requiredBars = calculateRequiredBars(params);

        // Step 3: Load bars (try cache first, then database)
        List<MarketBar> bars = loadBars(symbol, DEFAULT_TIMEFRAME, requiredBars);

        if (bars.isEmpty()) {
            log.warn("No bars available for strategy evaluation: symbol={}, timeframe={}",
                    symbol, DEFAULT_TIMEFRAME);
        }

        // Step 4: Build context
        StrategyContext context = StrategyContext.builder()
                .strategyId(strategy.getStrategyId())
                .symbol(symbol)
                .accountId(accountId)
                .bars(bars)
                .params(params)
                .timeframe(DEFAULT_TIMEFRAME)
                .build();

        log.info("Loaded strategy context: strategyId={}, symbol={}, bars={}, params={}",
                strategy.getStrategyId(), symbol, bars.size(), params.keySet());

        return context;
    }

    /**
     * Load bars for symbol and timeframe.
     * Tries cache first, falls back to database.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @param count Number of bars to load
     * @return List of bars (oldest first)
     */
    private List<MarketBar> loadBars(String symbol, String timeframe, int count) {
        // Try cache first (faster)
        List<MarketBar> cachedBars = barCache.getRecentBars(symbol, timeframe, count);

        if (!cachedBars.isEmpty() && cachedBars.size() >= count) {
            log.debug("Loaded {} bars from cache for symbol={}", cachedBars.size(), symbol);
            return cachedBars;
        }

        // Fall back to database
        try {
            List<MarketBar> dbBars = barRepository.findRecentClosedBars(symbol, timeframe, count);
            log.debug("Loaded {} bars from database for symbol={}", dbBars.size(), symbol);
            return dbBars;
        } catch (Exception e) {
            log.error("Error loading bars from database: symbol={}, timeframe={}", symbol, timeframe, e);
            return Collections.emptyList();
        }
    }

    /**
     * Calculate required bar count based on strategy parameters.
     * Different strategies need different amounts of historical data.
     *
     * @param params Strategy parameters
     * @return Required bar count
     */
    private int calculateRequiredBars(Map<String, Object> params) {
        int maxPeriod = 0;

        // Check for MA parameters
        if (params.containsKey("shortPeriod")) {
            maxPeriod = Math.max(maxPeriod, getIntParam(params, "shortPeriod"));
        }
        if (params.containsKey("longPeriod")) {
            maxPeriod = Math.max(maxPeriod, getIntParam(params, "longPeriod"));
        }

        // Check for RSI period
        if (params.containsKey("period")) {
            maxPeriod = Math.max(maxPeriod, getIntParam(params, "period"));
        }

        // Add buffer for indicator calculation (need extra bars for warmup)
        int bufferBars = 10;

        // Need +1 for crossover detection
        int required = maxPeriod + bufferBars + 1;

        // Cap at maximum to avoid excessive memory usage
        return Math.min(required, DEFAULT_BAR_LOOKBACK);
    }

    private int getIntParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
