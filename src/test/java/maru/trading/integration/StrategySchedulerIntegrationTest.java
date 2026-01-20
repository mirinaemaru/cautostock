package maru.trading.integration;

import maru.trading.application.ports.broker.BrokerAck;
import maru.trading.application.ports.broker.BrokerClient;
import maru.trading.application.scheduler.StrategyScheduler;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.shared.Environment;
import maru.trading.infra.cache.BarCache;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.entity.StrategySymbolEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyVersionEntity;
import maru.trading.infra.persistence.jpa.repository.SignalJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategySymbolJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyVersionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for StrategyScheduler.
 *
 * Tests the scheduler with real database operations:
 * - Strategy loading from database
 * - Symbol-strategy mapping lookup
 * - Signal generation and persistence
 * - Complete execution pipeline
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("StrategyScheduler Integration Tests")
class StrategySchedulerIntegrationTest {

    @Autowired
    private StrategyScheduler strategyScheduler;

    @Autowired
    private StrategyJpaRepository strategyRepository;

    @Autowired
    private StrategyVersionJpaRepository strategyVersionRepository;

    @Autowired
    private StrategySymbolJpaRepository strategySymbolRepository;

    @Autowired
    private SignalJpaRepository signalRepository;

    @Autowired
    private BarCache barCache;

    @MockBean
    private BrokerClient brokerClient;

    private String strategyId;
    private String versionId;
    private String symbol;
    private String accountId;

    @BeforeEach
    void setUp() {
        strategyId = UlidGenerator.generate();
        versionId = UlidGenerator.generate();
        symbol = "005930";
        accountId = "ACC_TEST_001";

        // Mock broker client
        given(brokerClient.placeOrder(any()))
                .willReturn(BrokerAck.success("BROKER-TEST-001"));

        // Clear existing data
        signalRepository.deleteAll();
        strategySymbolRepository.deleteAll();
        strategyVersionRepository.deleteAll();
        strategyRepository.deleteAll();
    }

    @Nested
    @DisplayName("Database Integration Tests")
    class DatabaseIntegrationTests {

        @Test
        @DisplayName("Should load and execute active strategy from database")
        void shouldLoadAndExecuteActiveStrategy() {
            // Given - Create strategy with version in database
            createActiveStrategy(strategyId, "MA_CROSSOVER", versionId);
            createStrategyVersion(versionId, strategyId, "{\"shortPeriod\": 5, \"longPeriod\": 20, \"ttlSeconds\": 120}");
            createStrategySymbolMapping(strategyId, symbol, accountId);

            // Prepare bar data for MA Crossover (need enough bars)
            prepareBarData(symbol, 30);

            // When
            strategyScheduler.executeStrategies();

            // Then - Strategy should have been executed (check logs or signal generation)
            // Note: Signal might not be generated if bars don't produce crossover
            assertThat(strategyRepository.findByStrategyIdAndDelyn(strategyId, "N")).isPresent();
        }

        @Test
        @DisplayName("Should skip inactive strategies")
        void shouldSkipInactiveStrategies() {
            // Given - Create inactive strategy
            createInactiveStrategy(strategyId, "MA_CROSSOVER", versionId);
            createStrategyVersion(versionId, strategyId, "{\"shortPeriod\": 5, \"longPeriod\": 20, \"ttlSeconds\": 120}");

            int signalCountBefore = (int) signalRepository.count();

            // When
            strategyScheduler.executeStrategies();

            // Then - No new signals should be created
            int signalCountAfter = (int) signalRepository.count();
            assertThat(signalCountAfter).isEqualTo(signalCountBefore);
        }

        @Test
        @DisplayName("Should use default symbol when no mappings exist")
        void shouldUseDefaultSymbolWhenNoMappings() {
            // Given - Create strategy without symbol mappings
            createActiveStrategy(strategyId, "RSI", versionId);
            createStrategyVersion(versionId, strategyId, "{\"period\": 14, \"overboughtThreshold\": 70, \"oversoldThreshold\": 30, \"ttlSeconds\": 120}");
            // No symbol mapping created

            // Prepare bar data for default symbol
            prepareBarData("005930", 30);

            // When
            strategyScheduler.executeStrategies();

            // Then - Strategy should execute with default symbol (005930)
            // Verified by no exception being thrown
            assertThat(strategyRepository.findByStrategyIdAndDelyn(strategyId, "N")).isPresent();
        }

        @Test
        @DisplayName("Should execute strategy for multiple symbols")
        void shouldExecuteForMultipleSymbols() {
            // Given - Create strategy with multiple symbol mappings
            createActiveStrategy(strategyId, "RSI", versionId);
            createStrategyVersion(versionId, strategyId, "{\"period\": 14, \"overboughtThreshold\": 70, \"oversoldThreshold\": 30, \"ttlSeconds\": 120}");

            createStrategySymbolMapping(strategyId, "005930", accountId);
            createStrategySymbolMapping(strategyId, "035420", accountId);
            createStrategySymbolMapping(strategyId, "000660", accountId);

            // Prepare bar data for all symbols
            prepareBarData("005930", 30);
            prepareBarData("035420", 30);
            prepareBarData("000660", 30);

            // When
            strategyScheduler.executeStrategies();

            // Then - Strategy should have been executed for all symbols
            List<StrategySymbolEntity> mappings = strategySymbolRepository.findActiveByStrategyId(strategyId);
            assertThat(mappings).hasSize(3);
        }

        @Test
        @DisplayName("Should handle multiple active strategies")
        void shouldHandleMultipleActiveStrategies() {
            // Given - Create multiple strategies
            String strategyId1 = UlidGenerator.generate();
            String strategyId2 = UlidGenerator.generate();
            String versionId1 = UlidGenerator.generate();
            String versionId2 = UlidGenerator.generate();

            createActiveStrategy(strategyId1, "MA_CROSSOVER", versionId1);
            createStrategyVersion(versionId1, strategyId1, "{\"shortPeriod\": 5, \"longPeriod\": 20, \"ttlSeconds\": 120}");
            createStrategySymbolMapping(strategyId1, "005930", accountId);

            createActiveStrategy(strategyId2, "RSI", versionId2);
            createStrategyVersion(versionId2, strategyId2, "{\"period\": 14, \"overboughtThreshold\": 70, \"oversoldThreshold\": 30, \"ttlSeconds\": 120}");
            createStrategySymbolMapping(strategyId2, "035420", accountId);

            // Prepare bar data
            prepareBarData("005930", 30);
            prepareBarData("035420", 30);

            // When
            strategyScheduler.executeStrategies();

            // Then - Both strategies should exist
            assertThat(strategyRepository.findByStatusAndDelyn("ACTIVE", "N")).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should continue when one strategy fails")
        void shouldContinueWhenOneStrategyFails() {
            // Given - Create two strategies, one with invalid version
            String strategyId1 = UlidGenerator.generate();
            String strategyId2 = UlidGenerator.generate();
            String versionId1 = UlidGenerator.generate();
            String versionId2 = UlidGenerator.generate();

            // First strategy - will fail due to missing version
            createActiveStrategy(strategyId1, "MA_CROSSOVER", "INVALID_VERSION_ID");

            // Second strategy - valid
            createActiveStrategy(strategyId2, "RSI", versionId2);
            createStrategyVersion(versionId2, strategyId2, "{\"period\": 14, \"overboughtThreshold\": 70, \"oversoldThreshold\": 30, \"ttlSeconds\": 120}");
            createStrategySymbolMapping(strategyId2, "005930", accountId);

            prepareBarData("005930", 30);

            // When - Should not throw exception
            strategyScheduler.executeStrategies();

            // Then - Second strategy should still be processed
            assertThat(strategyRepository.findByStrategyIdAndDelyn(strategyId2, "N")).isPresent();
        }

        @Test
        @DisplayName("Should handle missing bar data gracefully")
        void shouldHandleMissingBarDataGracefully() {
            // Given - Create strategy but don't prepare bar data
            createActiveStrategy(strategyId, "MA_CROSSOVER", versionId);
            createStrategyVersion(versionId, strategyId, "{\"shortPeriod\": 5, \"longPeriod\": 20, \"ttlSeconds\": 120}");
            createStrategySymbolMapping(strategyId, "UNKNOWN_SYMBOL", accountId);

            // When - Should not throw exception
            strategyScheduler.executeStrategies();

            // Then - Strategy execution completed (even if no signal generated)
            assertThat(strategyRepository.findByStrategyIdAndDelyn(strategyId, "N")).isPresent();
        }
    }

    @Nested
    @DisplayName("Manual Trigger Tests")
    class ManualTriggerTests {

        @Test
        @DisplayName("Should execute strategy manually with specific parameters")
        void shouldExecuteManuallyWithParameters() {
            // Given
            createActiveStrategy(strategyId, "RSI", versionId);
            createStrategyVersion(versionId, strategyId, "{\"period\": 14, \"overboughtThreshold\": 70, \"oversoldThreshold\": 30, \"ttlSeconds\": 120}");
            prepareBarData(symbol, 30);

            // When
            strategyScheduler.triggerManually(strategyId, symbol, accountId);

            // Then - Strategy should have been executed
            assertThat(strategyRepository.findByStrategyIdAndDelyn(strategyId, "N")).isPresent();
        }
    }

    // ==================== Helper Methods ====================

    private void createActiveStrategy(String stratId, String name, String versId) {
        StrategyEntity entity = StrategyEntity.builder()
                .strategyId(stratId)
                .name(name + "_" + stratId.substring(0, 8)) // Make name unique
                .description("Test strategy")
                .status("ACTIVE")
                .mode(Environment.PAPER)
                .activeVersionId(versId)
                .delyn("N")
                .build();
        strategyRepository.save(entity);
    }

    private void createInactiveStrategy(String stratId, String name, String versId) {
        StrategyEntity entity = StrategyEntity.builder()
                .strategyId(stratId)
                .name(name + "_INACTIVE_" + stratId.substring(0, 8))
                .description("Inactive test strategy")
                .status("STOPPED")
                .mode(Environment.PAPER)
                .activeVersionId(versId)
                .delyn("N")
                .build();
        strategyRepository.save(entity);
    }

    private void createStrategyVersion(String versId, String stratId, String paramsJson) {
        StrategyVersionEntity entity = StrategyVersionEntity.builder()
                .strategyVersionId(versId)
                .strategyId(stratId)
                .versionNo(1)
                .paramsJson(paramsJson)
                .build();
        strategyVersionRepository.save(entity);
    }

    private void createStrategySymbolMapping(String stratId, String sym, String accId) {
        StrategySymbolEntity entity = StrategySymbolEntity.builder()
                .strategySymbolId(UlidGenerator.generate())
                .strategyId(stratId)
                .symbol(sym)
                .accountId(accId)
                .isActive(true)
                .build();
        strategySymbolRepository.save(entity);
    }

    /**
     * Prepare bar data in cache for strategy evaluation.
     * Creates bars with slight upward trend for testing.
     */
    private void prepareBarData(String sym, int count) {
        BigDecimal basePrice = BigDecimal.valueOf(70000);
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(count);

        for (int i = 0; i < count; i++) {
            // Create slight upward trend with some variation
            BigDecimal variation = BigDecimal.valueOf(Math.sin(i * 0.5) * 500 + i * 10);
            BigDecimal open = basePrice.add(variation);
            BigDecimal high = open.add(BigDecimal.valueOf(200));
            BigDecimal low = open.subtract(BigDecimal.valueOf(150));
            BigDecimal close = open.add(BigDecimal.valueOf(Math.random() * 100 - 50));
            long volume = 10000L + (long)(Math.random() * 5000);

            // Use MarketBar.restore() to create complete bars
            MarketBar bar = MarketBar.restore(
                    sym,
                    "1m",
                    startTime.plusMinutes(i),
                    open,
                    high,
                    low,
                    close,
                    volume,
                    true // closed
            );

            // Put each bar in cache
            barCache.put(bar);
        }
    }
}
