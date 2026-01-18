package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.DailyPerformanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyPerformanceJpaRepository extends JpaRepository<DailyPerformanceEntity, String> {

    Optional<DailyPerformanceEntity> findByAccountIdAndStrategyIdAndTradeDate(
            String accountId, String strategyId, LocalDate tradeDate);

    List<DailyPerformanceEntity> findByAccountIdAndTradeDateBetweenOrderByTradeDateAsc(
            String accountId, LocalDate from, LocalDate to);

    List<DailyPerformanceEntity> findByStrategyIdAndTradeDateBetweenOrderByTradeDateAsc(
            String strategyId, LocalDate from, LocalDate to);

    List<DailyPerformanceEntity> findByAccountIdAndStrategyIdAndTradeDateBetweenOrderByTradeDateAsc(
            String accountId, String strategyId, LocalDate from, LocalDate to);

    @Query("SELECT SUM(d.totalPnl) FROM DailyPerformanceEntity d " +
           "WHERE d.accountId = :accountId AND d.tradeDate BETWEEN :from AND :to")
    BigDecimal sumTotalPnlByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT SUM(d.totalTrades) FROM DailyPerformanceEntity d " +
           "WHERE d.accountId = :accountId AND d.tradeDate BETWEEN :from AND :to")
    Integer sumTotalTradesByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT SUM(d.winningTrades) FROM DailyPerformanceEntity d " +
           "WHERE d.accountId = :accountId AND d.tradeDate BETWEEN :from AND :to")
    Integer sumWinningTradesByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("SELECT d FROM DailyPerformanceEntity d WHERE d.strategyId = :strategyId " +
           "ORDER BY d.tradeDate DESC")
    List<DailyPerformanceEntity> findByStrategyIdOrderByTradeDateDesc(@Param("strategyId") String strategyId);

    @Query("SELECT DISTINCT d.strategyId FROM DailyPerformanceEntity d WHERE d.accountId = :accountId")
    List<String> findDistinctStrategyIdsByAccountId(@Param("accountId") String accountId);
}
