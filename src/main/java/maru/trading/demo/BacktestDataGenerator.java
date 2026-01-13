package maru.trading.demo;

import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import maru.trading.infra.persistence.jpa.repository.HistoricalBarJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Backtest Demo Data Generator.
 *
 * Generates synthetic historical bar data for backtesting demonstrations.
 */
@Component
public class BacktestDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(BacktestDataGenerator.class);
    private final Random random = new Random(42); // Fixed seed for reproducibility

    private final HistoricalBarJpaRepository historicalBarRepository;

    public BacktestDataGenerator(HistoricalBarJpaRepository historicalBarRepository) {
        this.historicalBarRepository = historicalBarRepository;
    }

    /**
     * Generate sample data for a trending market (uptrend then downtrend).
     *
     * This creates data suitable for testing MA Crossover strategy.
     */
    public void generateTrendingMarketData(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Generating trending market data for {}: {} to {}", symbol, startDate, endDate);

        List<HistoricalBarEntity> bars = new ArrayList<>();
        LocalDateTime currentDateTime = startDate.atTime(9, 0);
        LocalDateTime endDateTime = endDate.atTime(16, 0);

        BigDecimal price = BigDecimal.valueOf(70_000); // Starting price
        int trend = 1; // 1 = uptrend, -1 = downtrend
        int daysInTrend = 0;
        int trendDuration = 30; // Change trend every 30 days

        while (currentDateTime.isBefore(endDateTime)) {
            // Skip weekends
            if (currentDateTime.getDayOfWeek().getValue() >= 6) {
                currentDateTime = currentDateTime.plusDays(1);
                continue;
            }

            // Change trend direction periodically
            daysInTrend++;
            if (daysInTrend > trendDuration) {
                trend = -trend;
                daysInTrend = 0;
                log.debug("Trend change at {}: {}", currentDateTime.toLocalDate(),
                    trend == 1 ? "UPTREND" : "DOWNTREND");
            }

            // Generate daily bar
            BigDecimal open = price;
            BigDecimal dailyChange = BigDecimal.valueOf(random.nextInt(1000) + 500); // 500-1500 won change
            BigDecimal direction = BigDecimal.valueOf(trend);

            price = price.add(dailyChange.multiply(direction)); // Trend movement
            BigDecimal close = price;

            // Add some randomness
            BigDecimal high = close.max(open).add(BigDecimal.valueOf(random.nextInt(500)));
            BigDecimal low = close.min(open).subtract(BigDecimal.valueOf(random.nextInt(500)));

            long volume = 1_000_000L + random.nextInt(500_000);

            HistoricalBarEntity bar = HistoricalBarEntity.builder()
                    .barId(UlidGenerator.generate())
                    .symbol(symbol)
                    .timeframe("1d")
                    .barTimestamp(currentDateTime)
                    .openPrice(open)
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice(close)
                    .volume(volume)
                    .createdAt(LocalDateTime.now())
                    .build();

            bars.add(bar);

            // Move to next day
            currentDateTime = currentDateTime.plusDays(1);
        }

        // Save to database
        historicalBarRepository.saveAll(bars);
        log.info("Generated and saved {} bars for {}", bars.size(), symbol);
    }

    /**
     * Generate sample data for a ranging market (oscillating).
     *
     * This creates data suitable for testing RSI strategy.
     */
    public void generateRangingMarketData(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Generating ranging market data for {}: {} to {}", symbol, startDate, endDate);

        List<HistoricalBarEntity> bars = new ArrayList<>();
        LocalDateTime currentDateTime = startDate.atTime(9, 0);
        LocalDateTime endDateTime = endDate.atTime(16, 0);

        BigDecimal basePrice = BigDecimal.valueOf(50_000);
        BigDecimal rangeTop = BigDecimal.valueOf(55_000);
        BigDecimal rangeBottom = BigDecimal.valueOf(45_000);

        BigDecimal price = basePrice;
        int direction = 1; // 1 = up, -1 = down

        while (currentDateTime.isBefore(endDateTime)) {
            // Skip weekends
            if (currentDateTime.getDayOfWeek().getValue() >= 6) {
                currentDateTime = currentDateTime.plusDays(1);
                continue;
            }

            // Oscillate between range top and bottom
            BigDecimal dailyChange = BigDecimal.valueOf(random.nextInt(800) + 400);
            price = price.add(dailyChange.multiply(BigDecimal.valueOf(direction)));

            // Bounce at range boundaries
            if (price.compareTo(rangeTop) >= 0) {
                direction = -1;
                price = rangeTop;
            } else if (price.compareTo(rangeBottom) <= 0) {
                direction = 1;
                price = rangeBottom;
            }

            BigDecimal open = price.subtract(BigDecimal.valueOf(random.nextInt(200) - 100));
            BigDecimal close = price;
            BigDecimal high = close.max(open).add(BigDecimal.valueOf(random.nextInt(300)));
            BigDecimal low = close.min(open).subtract(BigDecimal.valueOf(random.nextInt(300)));

            long volume = 500_000L + random.nextInt(300_000);

            HistoricalBarEntity bar = HistoricalBarEntity.builder()
                    .barId(UlidGenerator.generate())
                    .symbol(symbol)
                    .timeframe("1d")
                    .barTimestamp(currentDateTime)
                    .openPrice(open)
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice(close)
                    .volume(volume)
                    .createdAt(LocalDateTime.now())
                    .build();

            bars.add(bar);

            currentDateTime = currentDateTime.plusDays(1);
        }

        historicalBarRepository.saveAll(bars);
        log.info("Generated and saved {} bars for {}", bars.size(), symbol);
    }

    /**
     * Generate multiple symbols with different market patterns.
     */
    public void generateDemoDataset() {
        log.info("========================================");
        log.info("Generating Demo Backtest Dataset");
        log.info("========================================");

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        // Clear existing data
        log.info("Clearing existing historical data...");
        historicalBarRepository.deleteAll();

        // Generate trending market for Samsung Electronics (005930)
        generateTrendingMarketData("005930", startDate, endDate);

        // Generate ranging market for SK Hynix (000660)
        generateRangingMarketData("000660", startDate, endDate);

        log.info("========================================");
        log.info("Demo Dataset Generation Complete");
        log.info("========================================");
        log.info("Symbols: 005930 (trending), 000660 (ranging)");
        log.info("Period: {} to {}", startDate, endDate);
        log.info("Total bars: {}", historicalBarRepository.count());
    }

    /**
     * Clear all historical data.
     */
    public void clearHistoricalData() {
        log.info("Clearing all historical data...");
        historicalBarRepository.deleteAll();
        log.info("Historical data cleared");
    }
}
