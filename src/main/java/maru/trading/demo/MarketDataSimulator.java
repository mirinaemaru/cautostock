package maru.trading.demo;

import maru.trading.application.orchestration.BarAggregator;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.MarketDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Market Data Simulator for Demo Purposes.
 *
 * Generates simulated market ticks for testing strategies without real market data.
 * Supports multiple scenarios:
 * - GOLDEN_CROSS: Price pattern that triggers MA golden cross
 * - DEATH_CROSS: Price pattern that triggers MA death cross
 * - RSI_OVERSOLD: Price pattern that triggers RSI oversold signal
 * - RSI_OVERBOUGHT: Price pattern that triggers RSI overbought signal
 * - VOLATILE: Random volatile price movements
 * - STABLE: Stable price with small fluctuations
 */
@Component
public class MarketDataSimulator {

    private static final Logger log = LoggerFactory.getLogger(MarketDataSimulator.class);

    private final MarketDataCache marketDataCache;
    private final BarAggregator barAggregator;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running = false;

    public MarketDataSimulator(MarketDataCache marketDataCache, BarAggregator barAggregator) {
        this.marketDataCache = marketDataCache;
        this.barAggregator = barAggregator;
    }

    /**
     * Start simulating market data.
     *
     * @param scenario Simulation scenario
     * @param symbol Symbol to simulate
     * @param tickIntervalMs Interval between ticks in milliseconds
     */
    public void start(SimulationScenario scenario, String symbol, long tickIntervalMs) {
        if (running) {
            log.warn("Simulator already running");
            return;
        }

        running = true;
        log.info("Starting market data simulation: scenario={}, symbol={}, interval={}ms",
                scenario, symbol, tickIntervalMs);

        List<BigDecimal> prices = generatePricePattern(scenario);
        LocalDateTime baseTime = LocalDateTime.now();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                simulateTicks(symbol, prices, baseTime);
            } catch (Exception e) {
                log.error("Error in simulation", e);
                stop();
            }
        }, 0, tickIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop simulation.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        log.info("Market data simulation stopped");
    }

    /**
     * Run a complete simulation synchronously (for testing).
     *
     * @param scenario Simulation scenario
     * @param symbol Symbol to simulate
     */
    public void runSync(SimulationScenario scenario, String symbol) {
        log.info("Running synchronous simulation: scenario={}, symbol={}", scenario, symbol);

        List<BigDecimal> prices = generatePricePattern(scenario);
        LocalDateTime baseTime = LocalDateTime.now();

        simulateTicks(symbol, prices, baseTime);

        log.info("Synchronous simulation complete: {} ticks generated", prices.size());
    }

    private void simulateTicks(String symbol, List<BigDecimal> prices, LocalDateTime baseTime) {
        for (int i = 0; i < prices.size(); i++) {
            BigDecimal price = prices.get(i);
            LocalDateTime tickTime = baseTime.plusMinutes(i).plusSeconds(i % 60);

            // Generate multiple ticks per minute to create realistic bars
            for (int j = 0; j < 10; j++) {
                MarketTick tick = new MarketTick(
                        symbol,
                        price.add(BigDecimal.valueOf((j - 5) * 10)), // Small variations
                        100L,
                        tickTime.plusSeconds(j * 5),
                        "NORMAL"
                );

                marketDataCache.put(tick);
                barAggregator.onTick(tick);
            }

            // Close bar at minute boundary
            if ((i + 1) % 1 == 0) {
                barAggregator.closeAllBars();
            }
        }

        // Final bar close
        barAggregator.closeAllBars();
    }

    /**
     * Generate price pattern based on scenario.
     */
    private List<BigDecimal> generatePricePattern(SimulationScenario scenario) {
        return switch (scenario) {
            case GOLDEN_CROSS -> generateGoldenCrossPattern();
            case DEATH_CROSS -> generateDeathCrossPattern();
            case RSI_OVERSOLD -> generateRSIOversoldPattern();
            case RSI_OVERBOUGHT -> generateRSIOverboughtPattern();
            case VOLATILE -> generateVolatilePattern();
            case STABLE -> generateStablePattern();
        };
    }

    /**
     * Generate Golden Cross pattern (MA5 crosses above MA20).
     *
     * Pattern:
     * - Bars 1-20: Stable at 70000
     * - Bars 21-27: Downtrend (creates MA5 < MA20)
     * - Bars 28-35: Strong uptrend (triggers golden cross)
     */
    private List<BigDecimal> generateGoldenCrossPattern() {
        List<BigDecimal> prices = new ArrayList<>();

        // Phase 1: Stable (20 bars)
        for (int i = 0; i < 20; i++) {
            prices.add(BigDecimal.valueOf(70000));
        }

        // Phase 2: Downtrend (7 bars)
        prices.add(BigDecimal.valueOf(68000));
        prices.add(BigDecimal.valueOf(66000));
        prices.add(BigDecimal.valueOf(64000));
        prices.add(BigDecimal.valueOf(62000));
        prices.add(BigDecimal.valueOf(60000));
        prices.add(BigDecimal.valueOf(58000));
        prices.add(BigDecimal.valueOf(56000));

        // Phase 3: Strong uptrend (8 bars) - Golden Cross
        prices.add(BigDecimal.valueOf(62000));
        prices.add(BigDecimal.valueOf(68000));
        prices.add(BigDecimal.valueOf(74000));
        prices.add(BigDecimal.valueOf(80000));
        prices.add(BigDecimal.valueOf(86000));
        prices.add(BigDecimal.valueOf(92000));
        prices.add(BigDecimal.valueOf(98000));
        prices.add(BigDecimal.valueOf(104000));

        return prices;
    }

    /**
     * Generate Death Cross pattern (MA5 crosses below MA20).
     */
    private List<BigDecimal> generateDeathCrossPattern() {
        List<BigDecimal> prices = new ArrayList<>();

        // Phase 1: Stable (20 bars)
        for (int i = 0; i < 20; i++) {
            prices.add(BigDecimal.valueOf(70000));
        }

        // Phase 2: Uptrend (6 bars)
        prices.add(BigDecimal.valueOf(72000));
        prices.add(BigDecimal.valueOf(74000));
        prices.add(BigDecimal.valueOf(76000));
        prices.add(BigDecimal.valueOf(78000));
        prices.add(BigDecimal.valueOf(80000));
        prices.add(BigDecimal.valueOf(82000));

        // Phase 3: Crash (8 bars) - Death Cross
        prices.add(BigDecimal.valueOf(78000));
        prices.add(BigDecimal.valueOf(72000));
        prices.add(BigDecimal.valueOf(66000));
        prices.add(BigDecimal.valueOf(60000));
        prices.add(BigDecimal.valueOf(54000));
        prices.add(BigDecimal.valueOf(48000));
        prices.add(BigDecimal.valueOf(42000));
        prices.add(BigDecimal.valueOf(36000));

        return prices;
    }

    /**
     * Generate RSI Oversold pattern.
     */
    private List<BigDecimal> generateRSIOversoldPattern() {
        List<BigDecimal> prices = new ArrayList<>();

        // Stable period
        for (int i = 0; i < 21; i++) {
            prices.add(BigDecimal.valueOf(70000));
        }

        // Decline
        for (int i = 0; i < 4; i++) {
            prices.add(BigDecimal.valueOf(70000 - (i + 1) * 2000));
        }

        // Strong bounce (oversold crossover)
        for (int i = 0; i < 7; i++) {
            prices.add(BigDecimal.valueOf(62000 + (i + 1) * 1500));
        }

        // Adjustment
        for (int i = 0; i < 3; i++) {
            prices.add(BigDecimal.valueOf(73500 + i * 500));
        }

        // Final drop to trigger oversold
        prices.add(BigDecimal.valueOf(60000));

        return prices;
    }

    /**
     * Generate RSI Overbought pattern.
     */
    private List<BigDecimal> generateRSIOverboughtPattern() {
        List<BigDecimal> prices = new ArrayList<>();

        // Stable period
        for (int i = 0; i < 21; i++) {
            prices.add(BigDecimal.valueOf(70000));
        }

        // Rise
        for (int i = 0; i < 4; i++) {
            prices.add(BigDecimal.valueOf(70000 + (i + 1) * 2000));
        }

        // Strong correction (overbought crossover)
        for (int i = 0; i < 7; i++) {
            prices.add(BigDecimal.valueOf(78000 - (i + 1) * 1500));
        }

        // Rise again
        for (int i = 0; i < 3; i++) {
            prices.add(BigDecimal.valueOf(67500 + i * 500));
        }

        // Final surge to trigger overbought
        prices.add(BigDecimal.valueOf(80000));

        return prices;
    }

    /**
     * Generate volatile pattern.
     */
    private List<BigDecimal> generateVolatilePattern() {
        List<BigDecimal> prices = new ArrayList<>();
        BigDecimal base = BigDecimal.valueOf(70000);

        for (int i = 0; i < 50; i++) {
            // Random volatility: ±5%
            double volatility = (Math.random() - 0.5) * 0.1;
            BigDecimal price = base.multiply(BigDecimal.valueOf(1 + volatility));
            prices.add(price);
        }

        return prices;
    }

    /**
     * Generate stable pattern.
     */
    private List<BigDecimal> generateStablePattern() {
        List<BigDecimal> prices = new ArrayList<>();
        BigDecimal base = BigDecimal.valueOf(70000);

        for (int i = 0; i < 50; i++) {
            // Small fluctuation: ±0.1%
            double fluctuation = (Math.random() - 0.5) * 0.002;
            BigDecimal price = base.multiply(BigDecimal.valueOf(1 + fluctuation));
            prices.add(price);
        }

        return prices;
    }
}
