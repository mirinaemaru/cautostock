package maru.trading.application.scheduler;

import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.application.usecase.strategy.ExecuteStrategyUseCase;
import maru.trading.domain.strategy.Strategy;
import maru.trading.infra.persistence.jpa.entity.StrategySymbolEntity;
import maru.trading.infra.persistence.jpa.repository.StrategySymbolJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Strategy scheduler.
 *
 * Automatically executes active strategies on a schedule.
 * Runs every minute to evaluate strategies and generate signals.
 *
 * Can be disabled via application property:
 * trading.scheduler.strategy.enabled=false
 *
 * Also supports runtime enable/disable via setEnabled() method.
 */
@Service
@ConditionalOnProperty(
        name = "trading.scheduler.strategy.enabled",
        havingValue = "true",
        matchIfMissing = true // Enabled by default
)
public class StrategyScheduler {

    private static final Logger log = LoggerFactory.getLogger(StrategyScheduler.class);

    // Fallback defaults when no StrategySymbol mappings exist
    private static final String DEFAULT_SYMBOL = "005930"; // Samsung Electronics
    private static final String DEFAULT_ACCOUNT_ID = "ACC_DEMO_001"; // Default demo account

    private final StrategyRepository strategyRepository;
    private final StrategySymbolJpaRepository strategySymbolRepository;
    private final ExecuteStrategyUseCase executeStrategyUseCase;

    // Runtime state management
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicReference<LocalDateTime> lastExecutionTime = new AtomicReference<>();
    private final AtomicInteger executionCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);

    public StrategyScheduler(
            StrategyRepository strategyRepository,
            StrategySymbolJpaRepository strategySymbolRepository,
            ExecuteStrategyUseCase executeStrategyUseCase) {
        this.strategyRepository = strategyRepository;
        this.strategySymbolRepository = strategySymbolRepository;
        this.executeStrategyUseCase = executeStrategyUseCase;
    }

    /**
     * Enable or disable the scheduler at runtime.
     *
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) {
        boolean previousState = this.enabled.getAndSet(enabled);
        if (previousState != enabled) {
            log.info("Strategy scheduler {} (was {})",
                    enabled ? "ENABLED" : "DISABLED",
                    previousState ? "enabled" : "disabled");
        }
    }

    /**
     * Check if scheduler is currently enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Get scheduler status information.
     *
     * @return SchedulerStatus with current state
     */
    public SchedulerStatus getStatus() {
        return new SchedulerStatus(
                enabled.get(),
                lastExecutionTime.get(),
                executionCount.get(),
                successCount.get(),
                errorCount.get()
        );
    }

    /**
     * Reset scheduler statistics.
     */
    public void resetStatistics() {
        executionCount.set(0);
        successCount.set(0);
        errorCount.set(0);
        log.info("Scheduler statistics reset");
    }

    /**
     * Status record for scheduler state.
     */
    public record SchedulerStatus(
            boolean enabled,
            LocalDateTime lastExecutionTime,
            int executionCount,
            int successCount,
            int errorCount
    ) {}

    /**
     * Execute strategies every minute.
     * Cron: "0 * * * * *" = every minute at 0 seconds
     *
     * Schedule:
     * - 09:00:00 - First execution
     * - 09:01:00 - Second execution
     * - 09:02:00 - Third execution
     * - ...
     */
    @Scheduled(cron = "0 * * * * *")
    public void executeStrategies() {
        // Check if scheduler is enabled at runtime
        if (!enabled.get()) {
            log.debug("StrategyScheduler: Scheduler is disabled, skipping execution");
            return;
        }

        executionCount.incrementAndGet();
        lastExecutionTime.set(LocalDateTime.now());

        try {
            log.info("StrategyScheduler: Starting scheduled execution");

            // Find all active strategies
            List<Strategy> activeStrategies = strategyRepository.findActiveStrategies();

            if (activeStrategies.isEmpty()) {
                log.debug("No active strategies found, skipping execution");
                successCount.incrementAndGet();
                return;
            }

            log.info("Found {} active strategies to execute", activeStrategies.size());

            int strategySuccessCount = 0;
            int strategyErrorCount = 0;

            // Execute each strategy
            for (Strategy strategy : activeStrategies) {
                try {
                    log.info("Executing strategy: id={}, name={}", strategy.getStrategyId(), strategy.getName());

                    // Find all active symbols configured for this strategy
                    List<StrategySymbolEntity> strategySymbols =
                            strategySymbolRepository.findActiveByStrategyId(strategy.getStrategyId());

                    if (strategySymbols.isEmpty()) {
                        // Fallback: use default symbol if no mappings exist
                        log.warn("No StrategySymbol mappings found for strategy {}, using default symbol {}",
                                strategy.getStrategyId(), DEFAULT_SYMBOL);

                        executeStrategyUseCase.execute(
                                strategy.getStrategyId(),
                                DEFAULT_SYMBOL,
                                DEFAULT_ACCOUNT_ID
                        );
                        strategySuccessCount++;
                    } else {
                        // Execute strategy for each configured symbol
                        for (StrategySymbolEntity mapping : strategySymbols) {
                            try {
                                log.debug("Executing strategy {} for symbol {} on account {}",
                                        strategy.getStrategyId(), mapping.getSymbol(), mapping.getAccountId());

                                executeStrategyUseCase.execute(
                                        strategy.getStrategyId(),
                                        mapping.getSymbol(),
                                        mapping.getAccountId()
                                );
                                strategySuccessCount++;

                            } catch (Exception e) {
                                log.error("Error executing strategy {} for symbol {}: {}",
                                        strategy.getStrategyId(), mapping.getSymbol(), e.getMessage());
                                strategyErrorCount++;
                                // Continue with next symbol even if one fails
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("Error executing strategy: id={}, name={}",
                            strategy.getStrategyId(), strategy.getName(), e);
                    strategyErrorCount++;
                    // Continue with next strategy even if one fails
                }
            }

            if (strategyErrorCount == 0) {
                successCount.incrementAndGet();
            } else {
                errorCount.incrementAndGet();
            }

            log.info("StrategyScheduler: Completed scheduled execution (success={}, errors={})",
                    strategySuccessCount, strategyErrorCount);

        } catch (Exception e) {
            log.error("Error in strategy scheduler", e);
            errorCount.incrementAndGet();
        }
    }

    /**
     * Manual trigger for testing.
     * Called by StrategyAdminController.
     *
     * @param strategyId Strategy ID to execute
     * @param symbol Symbol to evaluate
     * @param accountId Account ID
     */
    public void triggerManually(String strategyId, String symbol, String accountId) {
        log.info("Manual strategy execution triggered: strategyId={}, symbol={}, accountId={}",
                strategyId, symbol, accountId);

        executeStrategyUseCase.execute(strategyId, symbol, accountId);
    }
}
