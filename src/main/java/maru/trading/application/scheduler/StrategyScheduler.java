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

import java.util.List;

/**
 * Strategy scheduler.
 *
 * Automatically executes active strategies on a schedule.
 * Runs every minute to evaluate strategies and generate signals.
 *
 * Can be disabled via application property:
 * trading.scheduler.strategy.enabled=false
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

    public StrategyScheduler(
            StrategyRepository strategyRepository,
            StrategySymbolJpaRepository strategySymbolRepository,
            ExecuteStrategyUseCase executeStrategyUseCase) {
        this.strategyRepository = strategyRepository;
        this.strategySymbolRepository = strategySymbolRepository;
        this.executeStrategyUseCase = executeStrategyUseCase;
    }

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
        try {
            log.info("StrategyScheduler: Starting scheduled execution");

            // Find all active strategies
            List<Strategy> activeStrategies = strategyRepository.findActiveStrategies();

            if (activeStrategies.isEmpty()) {
                log.debug("No active strategies found, skipping execution");
                return;
            }

            log.info("Found {} active strategies to execute", activeStrategies.size());

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

                            } catch (Exception e) {
                                log.error("Error executing strategy {} for symbol {}: {}",
                                        strategy.getStrategyId(), mapping.getSymbol(), e.getMessage());
                                // Continue with next symbol even if one fails
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("Error executing strategy: id={}, name={}",
                            strategy.getStrategyId(), strategy.getName(), e);
                    // Continue with next strategy even if one fails
                }
            }

            log.info("StrategyScheduler: Completed scheduled execution");

        } catch (Exception e) {
            log.error("Error in strategy scheduler", e);
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
