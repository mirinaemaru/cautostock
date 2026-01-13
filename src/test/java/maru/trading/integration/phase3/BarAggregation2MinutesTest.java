package maru.trading.integration.phase3;

import maru.trading.application.orchestration.BarAggregator;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.market.MarketTick;
import maru.trading.infra.cache.BarCache;
import maru.trading.infra.cache.MarketDataCache;
import maru.trading.infra.persistence.jpa.entity.BarEntity;
import maru.trading.infra.persistence.jpa.repository.BarJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3.4 - Bar Aggregation 2 Minutes Test.
 *
 * Tests bar aggregation over 2-minute period:
 * - Inject 30 ticks across 2 minutes (15 per minute)
 * - Verify 2 bars are created (timeframe=1m)
 * - Verify OHLCV values are correct
 * - Verify first bar is closed
 * - Verify bars are persisted in DB and cached
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Phase 3.4 - Bar Aggregation 2 Minutes Test")
class BarAggregation2MinutesTest {

    @Autowired
    private MarketDataCache marketDataCache;

    @Autowired
    private BarAggregator barAggregator;

    @Autowired
    private BarCache barCache;

    @Autowired
    private BarJpaRepository barRepository;

    private String symbol;

    @BeforeEach
    void setUp() {
        symbol = "005930";
    }

    @Test
    @DisplayName("Should create 2 bars from 30 ticks across 2 minutes")
    void testBarAggregation_TwoMinutes() {
        // Given - Base time at 9:30:00
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 9, 30, 0);

        // Phase 1: Inject 15 ticks for Minute 1 (9:30:00 - 9:30:59)
        // Prices: 70000 → 70140 (increasing by 10 each tick)
        for (int i = 0; i < 15; i++) {
            MarketTick tick = createTick(
                    symbol,
                    BigDecimal.valueOf(70000 + i * 10),
                    100 + i * 5,
                    baseTime.plusSeconds(i * 4) // Every 4 seconds
            );
            marketDataCache.put(tick);
            barAggregator.onTick(tick);
        }

        // Phase 2: Inject 15 ticks for Minute 2 (9:31:00 - 9:31:59)
        // Prices: 70200 → 70340 (continuing increase)
        for (int i = 0; i < 15; i++) {
            MarketTick tick = createTick(
                    symbol,
                    BigDecimal.valueOf(70200 + i * 10),
                    150 + i * 5,
                    baseTime.plusMinutes(1).plusSeconds(i * 4)
            );
            marketDataCache.put(tick);
            barAggregator.onTick(tick);
        }

        // Phase 3: Close all bars
        barAggregator.closeAllBars();

        // Phase 4: Verify 2 bars created in DB
        List<BarEntity> savedBars = barRepository.findAll();
        assertThat(savedBars).hasSizeGreaterThanOrEqualTo(2);

        // Find the two specific bars for this symbol
        List<BarEntity> symbolBars = savedBars.stream()
                .filter(b -> b.getSymbol().equals(symbol))
                .sorted((a, b) -> a.getBarTimestamp().compareTo(b.getBarTimestamp()))
                .toList();
        assertThat(symbolBars).hasSizeGreaterThanOrEqualTo(2);

        // Phase 5: Verify First Bar (9:30:00)
        BarEntity firstBar = symbolBars.get(0);
        assertThat(firstBar.getSymbol()).isEqualTo(symbol);
        assertThat(firstBar.getTimeframe()).isEqualTo("1m");
        assertThat(firstBar.getBarTimestamp()).isEqualTo(baseTime.withSecond(0).withNano(0));
        assertThat(firstBar.getOpenPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(firstBar.getHighPrice()).isEqualByComparingTo(BigDecimal.valueOf(70140));
        assertThat(firstBar.getLowPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(firstBar.getClosePrice()).isEqualByComparingTo(BigDecimal.valueOf(70140));
        assertThat(firstBar.getClosed()).isTrue();

        // Phase 6: Verify Second Bar (9:31:00)
        BarEntity secondBar = symbolBars.get(1);
        assertThat(secondBar.getSymbol()).isEqualTo(symbol);
        assertThat(secondBar.getTimeframe()).isEqualTo("1m");
        assertThat(secondBar.getBarTimestamp()).isEqualTo(baseTime.plusMinutes(1).withSecond(0).withNano(0));
        assertThat(secondBar.getOpenPrice()).isEqualByComparingTo(BigDecimal.valueOf(70200));
        assertThat(secondBar.getHighPrice()).isEqualByComparingTo(BigDecimal.valueOf(70340));
        assertThat(secondBar.getLowPrice()).isEqualByComparingTo(BigDecimal.valueOf(70200));
        assertThat(secondBar.getClosePrice()).isEqualByComparingTo(BigDecimal.valueOf(70340));
        assertThat(secondBar.getClosed()).isTrue();

        // Phase 7: Verify bars are cached
        List<MarketBar> cachedBars = barCache.getRecentBars(symbol, "1m", 10);
        assertThat(cachedBars).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should correctly calculate OHLCV for volatile price movements")
    void testBarAggregation_VolatilePrices() {
        // Given - Base time
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 10, 0, 0);

        // Inject ticks with volatile price pattern:
        // Open: 70000, High: 71000, Low: 69000, Close: 70500
        BigDecimal[] prices = {
                BigDecimal.valueOf(70000), // Open
                BigDecimal.valueOf(70500),
                BigDecimal.valueOf(71000), // High
                BigDecimal.valueOf(70800),
                BigDecimal.valueOf(70200),
                BigDecimal.valueOf(69000), // Low
                BigDecimal.valueOf(69500),
                BigDecimal.valueOf(70000),
                BigDecimal.valueOf(70500)  // Close
        };

        for (int i = 0; i < prices.length; i++) {
            MarketTick tick = createTick(
                    symbol,
                    prices[i],
                    100,
                    baseTime.plusSeconds(i * 6)
            );
            marketDataCache.put(tick);
            barAggregator.onTick(tick);
        }

        // When - Close bars
        barAggregator.closeAllBars();

        // Then - Verify OHLCV
        LocalDateTime expectedTimestamp = baseTime.withSecond(0).withNano(0);
        List<BarEntity> bars = barRepository.findAll().stream()
                .filter(b -> b.getSymbol().equals(symbol) &&
                            b.getBarTimestamp().equals(expectedTimestamp))
                .toList();
        assertThat(bars).isNotEmpty();

        BarEntity bar = bars.get(0);
        assertThat(bar.getOpenPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(bar.getHighPrice()).isEqualByComparingTo(BigDecimal.valueOf(71000));
        assertThat(bar.getLowPrice()).isEqualByComparingTo(BigDecimal.valueOf(69000));
        assertThat(bar.getClosePrice()).isEqualByComparingTo(BigDecimal.valueOf(70500));
    }

    @Test
    @DisplayName("Should aggregate volume correctly across multiple ticks")
    void testBarAggregation_VolumeAccumulation() {
        // Given - Base time
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 11, 0, 0);

        // Inject 10 ticks with different volumes
        long[] volumes = {100, 200, 150, 300, 250, 180, 220, 190, 280, 210};
        long expectedTotalVolume = 0;
        for (long vol : volumes) {
            expectedTotalVolume += vol;
        }

        for (int i = 0; i < volumes.length; i++) {
            MarketTick tick = createTick(
                    symbol,
                    BigDecimal.valueOf(70000 + i * 5),
                    volumes[i],
                    baseTime.plusSeconds(i * 5)
            );
            marketDataCache.put(tick);
            barAggregator.onTick(tick);
        }

        // When - Close bars
        barAggregator.closeAllBars();

        // Then - Verify total volume
        List<BarEntity> bars = barRepository.findAll().stream()
                .filter(b -> b.getSymbol().equals(symbol))
                .toList();
        assertThat(bars).isNotEmpty();

        BarEntity bar = bars.get(0);
        assertThat(bar.getVolume()).isEqualTo(expectedTotalVolume);
    }

    @Test
    @DisplayName("Should create separate bars for different symbols")
    void testBarAggregation_MultipleSymbols() {
        // Given - Two different symbols
        String symbol1 = "005930"; // Samsung
        String symbol2 = "035420"; // NAVER

        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 13, 0, 0);

        // Inject ticks for symbol1
        for (int i = 0; i < 10; i++) {
            MarketTick tick = createTick(
                    symbol1,
                    BigDecimal.valueOf(70000 + i * 10),
                    100,
                    baseTime.plusSeconds(i * 5)
            );
            marketDataCache.put(tick);
            barAggregator.onTick(tick);
        }

        // Inject ticks for symbol2
        for (int i = 0; i < 10; i++) {
            MarketTick tick = createTick(
                    symbol2,
                    BigDecimal.valueOf(40000 + i * 5),
                    50,
                    baseTime.plusSeconds(i * 5)
            );
            marketDataCache.put(tick);
            barAggregator.onTick(tick);
        }

        // When - Close all bars
        barAggregator.closeAllBars();

        // Then - Verify 2 separate bars (one for each symbol)
        List<BarEntity> allBars = barRepository.findAll();
        List<BarEntity> symbol1Bars = allBars.stream()
                .filter(b -> b.getSymbol().equals(symbol1))
                .toList();
        List<BarEntity> symbol2Bars = allBars.stream()
                .filter(b -> b.getSymbol().equals(symbol2))
                .toList();

        assertThat(symbol1Bars).isNotEmpty();
        assertThat(symbol2Bars).isNotEmpty();

        // Verify distinct OHLC values
        BarEntity bar1 = symbol1Bars.get(0);
        BarEntity bar2 = symbol2Bars.get(0);

        assertThat(bar1.getOpenPrice()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(bar2.getOpenPrice()).isEqualByComparingTo(BigDecimal.valueOf(40000));
    }

    @Test
    @DisplayName("Should handle single tick creating minimal bar")
    void testBarAggregation_SingleTick() {
        // Given - Single tick
        LocalDateTime baseTime = LocalDateTime.of(2026, 1, 1, 14, 30, 0);
        BigDecimal price = BigDecimal.valueOf(70000);
        long volume = 500;

        MarketTick tick = createTick(symbol, price, volume, baseTime);
        marketDataCache.put(tick);
        barAggregator.onTick(tick);

        // When - Close bar
        barAggregator.closeAllBars();

        // Then - Bar should have OHLC all equal to single price
        List<BarEntity> bars = barRepository.findAll().stream()
                .filter(b -> b.getSymbol().equals(symbol))
                .toList();
        assertThat(bars).isNotEmpty();

        BarEntity bar = bars.get(0);
        assertThat(bar.getOpenPrice()).isEqualByComparingTo(price);
        assertThat(bar.getHighPrice()).isEqualByComparingTo(price);
        assertThat(bar.getLowPrice()).isEqualByComparingTo(price);
        assertThat(bar.getClosePrice()).isEqualByComparingTo(price);
        assertThat(bar.getVolume()).isEqualTo(volume);
    }

    // ==================== Helper Methods ====================

    private MarketTick createTick(String tickSymbol, BigDecimal price, long volume, LocalDateTime timestamp) {
        return new MarketTick(tickSymbol, price, volume, timestamp, "NORMAL");
    }
}
