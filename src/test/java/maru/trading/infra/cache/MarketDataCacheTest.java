package maru.trading.infra.cache;

import maru.trading.domain.market.MarketTick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MarketDataCache Test")
class MarketDataCacheTest {

    private MarketDataCache cache;

    @BeforeEach
    void setUp() {
        cache = new MarketDataCache();
    }

    @Nested
    @DisplayName("put() Tests")
    class PutTests {

        @Test
        @DisplayName("Should store tick in cache")
        void shouldStoreTickInCache() {
            // Given
            MarketTick tick = createTick("005930", 70000, 1000);

            // When
            cache.put(tick);

            // Then
            assertThat(cache.contains("005930")).isTrue();
            assertThat(cache.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should overwrite existing tick for same symbol")
        void shouldOverwriteExistingTickForSameSymbol() {
            // Given
            MarketTick tick1 = createTick("005930", 70000, 1000);
            MarketTick tick2 = createTick("005930", 71000, 2000);

            // When
            cache.put(tick1);
            cache.put(tick2);

            // Then
            assertThat(cache.size()).isEqualTo(1);
            assertThat(cache.get("005930").getPrice()).isEqualTo(BigDecimal.valueOf(71000));
        }

        @Test
        @DisplayName("Should handle null tick gracefully")
        void shouldHandleNullTickGracefully() {
            // When
            cache.put(null);

            // Then
            assertThat(cache.size()).isZero();
        }

        @Test
        @DisplayName("Should store multiple symbols")
        void shouldStoreMultipleSymbols() {
            // Given
            cache.put(createTick("005930", 70000, 1000));
            cache.put(createTick("000660", 100000, 500));
            cache.put(createTick("035720", 50000, 2000));

            // Then
            assertThat(cache.size()).isEqualTo(3);
            assertThat(cache.contains("005930")).isTrue();
            assertThat(cache.contains("000660")).isTrue();
            assertThat(cache.contains("035720")).isTrue();
        }
    }

    @Nested
    @DisplayName("get() Tests")
    class GetTests {

        @Test
        @DisplayName("Should return tick for existing symbol")
        void shouldReturnTickForExistingSymbol() {
            // Given
            MarketTick tick = createTick("005930", 70000, 1000);
            cache.put(tick);

            // When
            MarketTick result = cache.get("005930");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSymbol()).isEqualTo("005930");
            assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(70000));
        }

        @Test
        @DisplayName("Should return null for non-existent symbol")
        void shouldReturnNullForNonExistentSymbol() {
            // When
            MarketTick result = cache.get("NONEXISTENT");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getPrice() Tests")
    class GetPriceTests {

        @Test
        @DisplayName("Should return price for fresh tick")
        void shouldReturnPriceForFreshTick() {
            // Given
            MarketTick tick = createTick("005930", 70000, 1000);
            cache.put(tick);

            // When
            BigDecimal price = cache.getPrice("005930");

            // Then
            assertThat(price).isEqualTo(BigDecimal.valueOf(70000));
        }

        @Test
        @DisplayName("Should return null for non-existent symbol")
        void shouldReturnNullForNonExistentSymbol() {
            // When
            BigDecimal price = cache.getPrice("NONEXISTENT");

            // Then
            assertThat(price).isNull();
        }

        @Test
        @DisplayName("Should return null for stale tick")
        void shouldReturnNullForStaleTick() {
            // Given - tick from 2 hours ago (stale threshold is 1 hour)
            MarketTick staleTick = new MarketTick(
                    "005930",
                    BigDecimal.valueOf(70000),
                    1000L,
                    LocalDateTime.now().minusHours(2),
                    "NORMAL"
            );
            cache.put(staleTick);

            // When
            BigDecimal price = cache.getPrice("005930");

            // Then
            assertThat(price).isNull();
        }
    }

    @Nested
    @DisplayName("remove() Tests")
    class RemoveTests {

        @Test
        @DisplayName("Should remove existing tick")
        void shouldRemoveExistingTick() {
            // Given
            cache.put(createTick("005930", 70000, 1000));
            assertThat(cache.contains("005930")).isTrue();

            // When
            cache.remove("005930");

            // Then
            assertThat(cache.contains("005930")).isFalse();
            assertThat(cache.size()).isZero();
        }

        @Test
        @DisplayName("Should handle remove of non-existent symbol")
        void shouldHandleRemoveOfNonExistentSymbol() {
            // Given
            cache.put(createTick("005930", 70000, 1000));

            // When
            cache.remove("NONEXISTENT");

            // Then
            assertThat(cache.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("clear() Tests")
    class ClearTests {

        @Test
        @DisplayName("Should clear all ticks")
        void shouldClearAllTicks() {
            // Given
            cache.put(createTick("005930", 70000, 1000));
            cache.put(createTick("000660", 100000, 500));
            assertThat(cache.size()).isEqualTo(2);

            // When
            cache.clear();

            // Then
            assertThat(cache.size()).isZero();
            assertThat(cache.contains("005930")).isFalse();
            assertThat(cache.contains("000660")).isFalse();
        }

        @Test
        @DisplayName("Should handle clear on empty cache")
        void shouldHandleClearOnEmptyCache() {
            // When
            cache.clear();

            // Then
            assertThat(cache.size()).isZero();
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
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // When - each thread puts a different symbol
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        String symbol = "SYMBOL_" + index;
                        cache.put(createTick(symbol, 70000 + index, 1000));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then
            assertThat(cache.size()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("Should be thread-safe for concurrent reads and writes")
        void shouldBeThreadSafeForConcurrentReadsAndWrites() throws InterruptedException {
            // Given
            cache.put(createTick("005930", 70000, 1000));
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // When - mix of reads and writes
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        if (index % 2 == 0) {
                            cache.put(createTick("005930", 70000 + index, 1000 + index));
                        } else {
                            cache.get("005930");
                            cache.getPrice("005930");
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then - no exceptions, cache still has the symbol
            assertThat(cache.contains("005930")).isTrue();
        }
    }

    // ==================== Helper Methods ====================

    private MarketTick createTick(String symbol, int price, long volume) {
        return new MarketTick(
                symbol,
                BigDecimal.valueOf(price),
                volume,
                LocalDateTime.now(),
                "NORMAL"
        );
    }
}
