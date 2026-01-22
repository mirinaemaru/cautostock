package maru.trading.infra.cache;

import maru.trading.domain.market.MarketBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BarCache Test")
class BarCacheTest {

    private BarCache cache;

    @BeforeEach
    void setUp() {
        cache = new BarCache();
    }

    @Nested
    @DisplayName("put() Tests")
    class PutTests {

        @Test
        @DisplayName("Should store bar in cache")
        void shouldStoreBarInCache() {
            // Given
            MarketBar bar = createBar("005930", "1m", 70000, 70100, 69900, 70050, 1000);

            // When
            cache.put(bar);

            // Then
            List<MarketBar> bars = cache.getAllBars("005930", "1m");
            assertThat(bars).hasSize(1);
            assertThat(bars.get(0).getClose()).isEqualTo(BigDecimal.valueOf(70050));
        }

        @Test
        @DisplayName("Should throw exception for null bar")
        void shouldThrowExceptionForNullBar() {
            // When/Then
            assertThatThrownBy(() -> cache.put(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Bar cannot be null");
        }

        @Test
        @DisplayName("Should maintain bar order (oldest first)")
        void shouldMaintainBarOrder() {
            // Given
            LocalDateTime baseTime = LocalDateTime.now();

            // When - add bars in chronological order
            cache.put(createBarWithTime("005930", "1m", baseTime.minusMinutes(2), 70000));
            cache.put(createBarWithTime("005930", "1m", baseTime.minusMinutes(1), 70100));
            cache.put(createBarWithTime("005930", "1m", baseTime, 70200));

            // Then
            List<MarketBar> bars = cache.getAllBars("005930", "1m");
            assertThat(bars).hasSize(3);
            assertThat(bars.get(0).getClose()).isEqualTo(BigDecimal.valueOf(70000));
            assertThat(bars.get(2).getClose()).isEqualTo(BigDecimal.valueOf(70200));
        }

        @Test
        @DisplayName("Should separate bars by timeframe")
        void shouldSeparateBarsByTimeframe() {
            // Given
            cache.put(createBar("005930", "1m", 70000, 70100, 69900, 70050, 1000));
            cache.put(createBar("005930", "5m", 70000, 70500, 69800, 70300, 5000));

            // Then
            assertThat(cache.getAllBars("005930", "1m")).hasSize(1);
            assertThat(cache.getAllBars("005930", "5m")).hasSize(1);
        }

        @Test
        @DisplayName("Should trim cache when exceeding max bars")
        void shouldTrimCacheWhenExceedingMaxBars() {
            // Given - add more than 200 bars (default max)
            for (int i = 0; i < 250; i++) {
                cache.put(createBarWithTime("005930", "1m",
                        LocalDateTime.now().minusMinutes(250 - i), 70000 + i));
            }

            // Then
            List<MarketBar> bars = cache.getAllBars("005930", "1m");
            assertThat(bars).hasSize(200);
            // Should keep most recent bars
            assertThat(bars.get(199).getClose()).isEqualTo(BigDecimal.valueOf(70249));
        }
    }

    @Nested
    @DisplayName("getRecentBars() Tests")
    class GetRecentBarsTests {

        @Test
        @DisplayName("Should return requested number of recent bars")
        void shouldReturnRequestedNumberOfRecentBars() {
            // Given
            for (int i = 0; i < 10; i++) {
                cache.put(createBarWithTime("005930", "1m",
                        LocalDateTime.now().minusMinutes(10 - i), 70000 + i));
            }

            // When
            List<MarketBar> bars = cache.getRecentBars("005930", "1m", 5);

            // Then
            assertThat(bars).hasSize(5);
            // Most recent 5 bars
            assertThat(bars.get(0).getClose()).isEqualTo(BigDecimal.valueOf(70005));
            assertThat(bars.get(4).getClose()).isEqualTo(BigDecimal.valueOf(70009));
        }

        @Test
        @DisplayName("Should return all bars if count exceeds available")
        void shouldReturnAllBarsIfCountExceedsAvailable() {
            // Given
            for (int i = 0; i < 5; i++) {
                cache.put(createBarWithTime("005930", "1m",
                        LocalDateTime.now().minusMinutes(5 - i), 70000 + i));
            }

            // When
            List<MarketBar> bars = cache.getRecentBars("005930", "1m", 100);

            // Then
            assertThat(bars).hasSize(5);
        }

        @Test
        @DisplayName("Should return empty list for non-existent symbol")
        void shouldReturnEmptyListForNonExistentSymbol() {
            // When
            List<MarketBar> bars = cache.getRecentBars("NONEXISTENT", "1m", 5);

            // Then
            assertThat(bars).isEmpty();
        }

        @Test
        @DisplayName("Should return all bars for zero or negative count")
        void shouldReturnAllBarsForZeroOrNegativeCount() {
            // Given
            for (int i = 0; i < 5; i++) {
                cache.put(createBarWithTime("005930", "1m",
                        LocalDateTime.now().minusMinutes(5 - i), 70000 + i));
            }

            // When
            List<MarketBar> zeroCountBars = cache.getRecentBars("005930", "1m", 0);
            List<MarketBar> negativeCountBars = cache.getRecentBars("005930", "1m", -1);

            // Then
            assertThat(zeroCountBars).hasSize(5);
            assertThat(negativeCountBars).hasSize(5);
        }
    }

    @Nested
    @DisplayName("getLatestBar() Tests")
    class GetLatestBarTests {

        @Test
        @DisplayName("Should return most recent bar")
        void shouldReturnMostRecentBar() {
            // Given
            for (int i = 0; i < 5; i++) {
                cache.put(createBarWithTime("005930", "1m",
                        LocalDateTime.now().minusMinutes(5 - i), 70000 + i));
            }

            // When
            MarketBar latest = cache.getLatestBar("005930", "1m");

            // Then
            assertThat(latest).isNotNull();
            assertThat(latest.getClose()).isEqualTo(BigDecimal.valueOf(70004));
        }

        @Test
        @DisplayName("Should return null for non-existent symbol")
        void shouldReturnNullForNonExistentSymbol() {
            // When
            MarketBar latest = cache.getLatestBar("NONEXISTENT", "1m");

            // Then
            assertThat(latest).isNull();
        }

        @Test
        @DisplayName("Should return null for non-existent timeframe")
        void shouldReturnNullForNonExistentTimeframe() {
            // Given
            cache.put(createBar("005930", "1m", 70000, 70100, 69900, 70050, 1000));

            // When
            MarketBar latest = cache.getLatestBar("005930", "5m");

            // Then
            assertThat(latest).isNull();
        }
    }

    @Nested
    @DisplayName("clear() Tests")
    class ClearTests {

        @Test
        @DisplayName("Should clear bars for specific symbol and timeframe")
        void shouldClearBarsForSpecificSymbolAndTimeframe() {
            // Given
            cache.put(createBar("005930", "1m", 70000, 70100, 69900, 70050, 1000));
            cache.put(createBar("005930", "5m", 70000, 70500, 69800, 70300, 5000));

            // When
            cache.clear("005930", "1m");

            // Then
            assertThat(cache.getAllBars("005930", "1m")).isEmpty();
            assertThat(cache.getAllBars("005930", "5m")).hasSize(1);
        }

        @Test
        @DisplayName("Should clear all bars")
        void shouldClearAllBars() {
            // Given
            cache.put(createBar("005930", "1m", 70000, 70100, 69900, 70050, 1000));
            cache.put(createBar("000660", "1m", 100000, 100100, 99900, 100050, 500));

            // When
            cache.clearAll();

            // Then
            assertThat(cache.getAllBars("005930", "1m")).isEmpty();
            assertThat(cache.getAllBars("000660", "1m")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStats() Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should return cache statistics")
        void shouldReturnCacheStatistics() {
            // Given
            for (int i = 0; i < 10; i++) {
                cache.put(createBarWithTime("005930", "1m",
                        LocalDateTime.now().minusMinutes(10 - i), 70000 + i));
            }
            for (int i = 0; i < 5; i++) {
                cache.put(createBarWithTime("000660", "1m",
                        LocalDateTime.now().minusMinutes(5 - i), 100000 + i));
            }

            // When
            Map<String, Integer> stats = cache.getStats();

            // Then
            assertThat(stats).containsEntry("005930:1m", 10);
            assertThat(stats).containsEntry("000660:1m", 5);
        }

        @Test
        @DisplayName("Should return empty stats for empty cache")
        void shouldReturnEmptyStatsForEmptyCache() {
            // When
            Map<String, Integer> stats = cache.getStats();

            // Then
            assertThat(stats).isEmpty();
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should be thread-safe for concurrent puts")
        void shouldBeThreadSafeForConcurrentPuts() throws InterruptedException {
            // Given
            int threadCount = 10;
            int barsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < barsPerThread; j++) {
                            cache.put(createBarWithTime("005930", "1m",
                                    LocalDateTime.now().minusMinutes(threadIndex * barsPerThread + j),
                                    70000 + threadIndex * 100 + j));
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then - should have at most 200 bars (max limit)
            List<MarketBar> bars = cache.getAllBars("005930", "1m");
            assertThat(bars.size()).isLessThanOrEqualTo(200);
        }
    }

    // ==================== Helper Methods ====================

    private MarketBar createBar(String symbol, String timeframe, int open, int high, int low, int close, long volume) {
        return MarketBar.restore(
                symbol,
                timeframe,
                LocalDateTime.now(),
                BigDecimal.valueOf(open),
                BigDecimal.valueOf(high),
                BigDecimal.valueOf(low),
                BigDecimal.valueOf(close),
                volume,
                true
        );
    }

    private MarketBar createBarWithTime(String symbol, String timeframe, LocalDateTime timestamp, int closePrice) {
        return MarketBar.restore(
                symbol,
                timeframe,
                timestamp,
                BigDecimal.valueOf(closePrice - 100),
                BigDecimal.valueOf(closePrice + 100),
                BigDecimal.valueOf(closePrice - 200),
                BigDecimal.valueOf(closePrice),
                1000L,
                true
        );
    }
}
