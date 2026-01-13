package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.StrategySymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for StrategySymbol entity.
 */
@Repository
public interface StrategySymbolJpaRepository extends JpaRepository<StrategySymbolEntity, String> {

    /**
     * Find all symbols for a given strategy.
     *
     * @param strategyId Strategy ID
     * @return List of strategy-symbol mappings
     */
    List<StrategySymbolEntity> findByStrategyId(String strategyId);

    /**
     * Find all active symbols for a given strategy.
     *
     * @param strategyId Strategy ID
     * @return List of active strategy-symbol mappings
     */
    @Query("SELECT s FROM StrategySymbolEntity s WHERE s.strategyId = :strategyId AND s.isActive = true")
    List<StrategySymbolEntity> findActiveByStrategyId(@Param("strategyId") String strategyId);

    /**
     * Find all strategies for a given symbol.
     *
     * @param symbol Symbol
     * @return List of strategy-symbol mappings
     */
    List<StrategySymbolEntity> findBySymbol(String symbol);

    /**
     * Find all active strategies for a given symbol.
     *
     * @param symbol Symbol
     * @return List of active strategy-symbol mappings
     */
    @Query("SELECT s FROM StrategySymbolEntity s WHERE s.symbol = :symbol AND s.isActive = true")
    List<StrategySymbolEntity> findActiveBySymbol(@Param("symbol") String symbol);

    /**
     * Find all active strategy-symbol mappings.
     *
     * @return List of all active mappings
     */
    @Query("SELECT s FROM StrategySymbolEntity s WHERE s.isActive = true")
    List<StrategySymbolEntity> findAllActive();
}
