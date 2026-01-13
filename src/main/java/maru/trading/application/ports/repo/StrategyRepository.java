package maru.trading.application.ports.repo;

import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyVersion;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for strategies.
 */
public interface StrategyRepository {

    /**
     * Find strategy by ID.
     *
     * @param strategyId Strategy ID
     * @return Optional containing strategy if found
     */
    Optional<Strategy> findById(String strategyId);

    /**
     * Find strategy by name.
     *
     * @param name Strategy name
     * @return Optional containing strategy if found
     */
    Optional<Strategy> findByName(String name);

    /**
     * Find all active strategies.
     * Used by scheduler to execute strategies.
     *
     * @return List of active strategies
     */
    List<Strategy> findActiveStrategies();

    /**
     * Find strategy version by ID.
     *
     * @param versionId Strategy version ID
     * @return Optional containing version if found
     */
    Optional<StrategyVersion> findVersionById(String versionId);

    /**
     * Save a strategy.
     *
     * @param strategy Strategy to save
     * @return Saved strategy
     */
    Strategy save(Strategy strategy);
}
