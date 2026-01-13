package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.BacktestRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for BacktestRunEntity.
 */
@Repository
public interface BacktestRunJpaRepository extends JpaRepository<BacktestRunEntity, String> {

    /**
     * Find all backtest runs for a strategy.
     */
    List<BacktestRunEntity> findByStrategyIdOrderByStartedAtDesc(String strategyId);

    /**
     * Find backtest runs by status.
     */
    List<BacktestRunEntity> findByStatusOrderByStartedAtDesc(String status);

    /**
     * Find completed backtest runs for strategy.
     */
    @Query("SELECT b FROM BacktestRunEntity b " +
            "WHERE b.strategyId = :strategyId " +
            "AND b.status = 'COMPLETED' " +
            "ORDER BY b.startedAt DESC")
    List<BacktestRunEntity> findCompletedByStrategy(@Param("strategyId") String strategyId);

    /**
     * Find backtest runs started after date.
     */
    List<BacktestRunEntity> findByStartedAtAfterOrderByStartedAtDesc(LocalDateTime afterDate);

    /**
     * Count running backtests.
     */
    long countByStatus(String status);
}
