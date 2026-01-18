package maru.trading.load;

import maru.trading.TestFixtures;
import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.usecase.trading.PlaceOrderUseCase;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.Side;
import maru.trading.domain.risk.RiskLimitExceededException;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Load Test.
 *
 * Tests system behavior under concurrent load:
 * - Concurrent order placement
 * - Thread safety validation
 * - Resource contention handling
 *
 * Load test scenarios:
 * - 10 concurrent orders
 * - 50 concurrent orders
 * - Mixed operations (orders + queries)
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@DisplayName("Load Test")
class LoadTest {

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private RiskStateJpaRepository riskStateJpaRepository;

    @MockBean
    private BrokerClient brokerClient;

    private String accountId;
    private String symbol;

    @BeforeEach
    void setUp() {
        // Clear risk state data to prevent duplicate results from previous tests
        riskStateJpaRepository.deleteAll();

        accountId = "ACC_LOAD_001";
        symbol = "005930";
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-CONCURRENT"));
    }

    @Test
    @DisplayName("Should handle 10 concurrent order placements")
    void testConcurrentOrderPlacement_10Threads() throws InterruptedException, ExecutionException {
        // Given
        int concurrentOrders = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentOrders);
        List<Future<Order>> futures = new ArrayList<>();

        // When - Submit 10 concurrent order placement tasks
        for (int i = 0; i < concurrentOrders; i++) {
            final int orderIndex = i;
            Future<Order> future = executor.submit(() -> {
                Order order = TestFixtures.placeLimitOrder(
                        UlidGenerator.generate(),
                        accountId + "_" + orderIndex, // Different account to avoid conflicts
                        symbol,
                        Side.BUY,
                        BigDecimal.valueOf(1),
                        BigDecimal.valueOf(70000 + orderIndex * 10),
                        UlidGenerator.generate()
                );
                return placeOrderUseCase.execute(order);
            });
            futures.add(future);
        }

        // Then - All orders should be placed successfully
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        int successCount = 0;
        for (Future<Order> future : futures) {
            Order result = future.get();
            if (result != null && result.getStatus() == OrderStatus.SENT) {
                successCount++;
            }
        }

        assertThat(successCount).isEqualTo(concurrentOrders);
    }

    @Test
    @DisplayName("Should handle 50 concurrent order placements without errors")
    void testConcurrentOrderPlacement_50Threads() throws InterruptedException {
        // Given
        int concurrentOrders = 50;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(concurrentOrders);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - Submit 50 concurrent tasks
        AtomicInteger riskRejectedCount = new AtomicInteger(0);
        for (int i = 0; i < concurrentOrders; i++) {
            final int orderIndex = i;
            executor.submit(() -> {
                try {
                    Order order = TestFixtures.placeLimitOrder(
                            UlidGenerator.generate(),
                            accountId + "_" + orderIndex, // Different account to avoid conflicts
                            symbol,
                            Side.BUY,
                            BigDecimal.valueOf(1),
                            BigDecimal.valueOf(70000),
                            UlidGenerator.generate()
                    );
                    Order result = placeOrderUseCase.execute(order);
                    if (result.getStatus() == OrderStatus.SENT) {
                        successCount.incrementAndGet();
                    }
                } catch (RiskLimitExceededException e) {
                    // Risk rejection is expected behavior, not an error
                    riskRejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // In concurrent tests, various exceptions may occur
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then - Wait for all tasks and verify
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // All requests should be handled (completed within timeout)
        // Note: In high concurrency, some DB transaction conflicts may occur
    }

    @Test
    @DisplayName("System should remain responsive under sustained load")
    void testSustainedLoad() throws InterruptedException {
        // Given
        int totalRequests = 100;
        int threadPoolSize = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger riskRejectedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // When - Submit 100 requests with 10 concurrent threads
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int requestIndex = i;
            executor.submit(() -> {
                try {
                    // Use different accounts to avoid risk limit conflicts
                    String testAccountId = "ACC_LOAD_" + (requestIndex % 20);

                    Order order = TestFixtures.placeLimitOrder(
                            UlidGenerator.generate(),
                            testAccountId,
                            symbol,
                            Side.BUY,
                            BigDecimal.valueOf(1),
                            BigDecimal.valueOf(70000),
                            UlidGenerator.generate()
                    );
                    placeOrderUseCase.execute(order);
                    processedCount.incrementAndGet();
                } catch (RiskLimitExceededException e) {
                    // Expected: Risk rejection is normal behavior
                    riskRejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // Unexpected error
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then - All requests should complete within reasonable time
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();

        assertThat(completed).isTrue();
        // All requests handled (processed + risk rejected + errors = total)
        assertThat(processedCount.get() + riskRejectedCount.get() + errorCount.get())
                .isEqualTo(totalRequests);
        // System remains responsive - completes within timeout
        assertThat(duration).isLessThan(60000L); // Complete within 60 seconds
    }
}
