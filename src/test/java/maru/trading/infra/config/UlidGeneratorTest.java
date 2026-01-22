package maru.trading.infra.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UlidGenerator Test")
class UlidGeneratorTest {

    private UlidGenerator ulidGenerator;

    @BeforeEach
    void setUp() {
        ulidGenerator = new UlidGenerator();
    }

    @Nested
    @DisplayName("Instance Method Tests")
    class InstanceMethodTests {

        @Test
        @DisplayName("Should generate valid ULID")
        void shouldGenerateValidUlid() {
            // When
            String ulid = ulidGenerator.generateInstance();

            // Then
            assertThat(ulid).isNotNull();
            assertThat(ulid).hasSize(26); // ULID is 26 characters
            assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}"); // ULID character set (Crockford Base32)
        }

        @Test
        @DisplayName("Should generate unique ULIDs")
        void shouldGenerateUniqueUlids() {
            // Given
            Set<String> ulids = new HashSet<>();

            // When
            for (int i = 0; i < 1000; i++) {
                ulids.add(ulidGenerator.generateInstance());
            }

            // Then
            assertThat(ulids).hasSize(1000);
        }

        @Test
        @DisplayName("Should generate ULID with prefix")
        void shouldGenerateUlidWithPrefix() {
            // Given
            String prefix = "ORD";

            // When
            String ulid = ulidGenerator.generateWithPrefixInstance(prefix);

            // Then
            assertThat(ulid).isNotNull();
            assertThat(ulid).startsWith("ORD_");
            assertThat(ulid).hasSize(30); // "ORD_" (4) + ULID (26)
        }

        @Test
        @DisplayName("Should handle empty prefix")
        void shouldHandleEmptyPrefix() {
            // When
            String ulid = ulidGenerator.generateWithPrefixInstance("");

            // Then
            assertThat(ulid).isNotNull();
            assertThat(ulid).startsWith("_");
        }
    }

    @Nested
    @DisplayName("Static Method Tests")
    class StaticMethodTests {

        @Test
        @DisplayName("Should generate valid ULID statically")
        void shouldGenerateValidUlidStatically() {
            // When
            String ulid = UlidGenerator.generate();

            // Then
            assertThat(ulid).isNotNull();
            assertThat(ulid).hasSize(26);
            assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}");
        }

        @Test
        @DisplayName("Should generate ULID with prefix statically")
        void shouldGenerateUlidWithPrefixStatically() {
            // Given
            String prefix = "FILL";

            // When
            String ulid = UlidGenerator.generateWithPrefix(prefix);

            // Then
            assertThat(ulid).isNotNull();
            assertThat(ulid).startsWith("FILL_");
            assertThat(ulid).hasSize(31); // "FILL_" (5) + ULID (26)
        }

        @Test
        @DisplayName("Should generate unique ULIDs statically")
        void shouldGenerateUniqueUlidsStatically() {
            // Given
            Set<String> ulids = new HashSet<>();

            // When
            for (int i = 0; i < 1000; i++) {
                ulids.add(UlidGenerator.generate());
            }

            // Then
            assertThat(ulids).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should be thread-safe for concurrent generation")
        void shouldBeThreadSafeForConcurrentGeneration() throws InterruptedException {
            // Given
            int threadCount = 10;
            int generationsPerThread = 100;
            Set<String> ulids = java.util.Collections.synchronizedSet(new HashSet<>());
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < generationsPerThread; j++) {
                            ulids.add(ulidGenerator.generateInstance());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then
            assertThat(ulids).hasSize(threadCount * generationsPerThread);
        }
    }

    @Nested
    @DisplayName("ULID Order Tests")
    class UlidOrderTests {

        @Test
        @DisplayName("Should generate lexicographically sortable ULIDs")
        void shouldGenerateLexicographicallySortableUlids() throws InterruptedException {
            // Given
            String ulid1 = ulidGenerator.generateInstance();
            Thread.sleep(2); // Small delay to ensure different timestamp
            String ulid2 = ulidGenerator.generateInstance();

            // Then
            assertThat(ulid1.compareTo(ulid2)).isLessThan(0);
        }
    }
}
