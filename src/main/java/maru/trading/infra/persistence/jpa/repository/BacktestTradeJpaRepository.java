package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.BacktestTradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * JPA Repository for BacktestTradeEntity.
 */
@Repository
public interface BacktestTradeJpaRepository extends JpaRepository<BacktestTradeEntity, String> {

    /**
     * Find all trades for a backtest run.
     */
    List<BacktestTradeEntity> findByBacktestIdOrderByEntryTimeAsc(String backtestId);

    /**
     * Find open trades for a backtest.
     */
    List<BacktestTradeEntity> findByBacktestIdAndStatus(String backtestId, String status);

    /**
     * Find winning trades for a backtest.
     */
    @Query("SELECT t FROM BacktestTradeEntity t " +
            "WHERE t.backtestId = :backtestId " +
            "AND t.netPnl > 0 " +
            "ORDER BY t.entryTime ASC")
    List<BacktestTradeEntity> findWinningTrades(@Param("backtestId") String backtestId);

    /**
     * Find losing trades for a backtest.
     */
    @Query("SELECT t FROM BacktestTradeEntity t " +
            "WHERE t.backtestId = :backtestId " +
            "AND t.netPnl < 0 " +
            "ORDER BY t.entryTime ASC")
    List<BacktestTradeEntity> findLosingTrades(@Param("backtestId") String backtestId);

    /**
     * Calculate total P&L for backtest.
     */
    @Query("SELECT COALESCE(SUM(t.netPnl), 0) FROM BacktestTradeEntity t " +
            "WHERE t.backtestId = :backtestId")
    BigDecimal calculateTotalPnl(@Param("backtestId") String backtestId);

    /**
     * Count trades by status.
     */
    long countByBacktestIdAndStatus(String backtestId, String status);

    /**
     * Delete all trades for a backtest run.
     */
    void deleteByBacktestId(String backtestId);
}
