package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository for HistoricalBarEntity.
 *
 * Provides queries for historical market data used in backtesting.
 */
@Repository
public interface HistoricalBarJpaRepository extends JpaRepository<HistoricalBarEntity, String> {

    /**
     * Find bars by symbol and timeframe, ordered by timestamp.
     *
     * @param symbol Symbol code
     * @param timeframe Timeframe (1m, 5m, 1h, 1d, etc.)
     * @return List of bars ordered by timestamp ASC
     */
    List<HistoricalBarEntity> findBySymbolAndTimeframeOrderByBarTimestampAsc(
            String symbol,
            String timeframe
    );

    /**
     * Find bars by symbol, timeframe, and date range.
     *
     * Critical for backtesting: loads data for specific period.
     *
     * @param symbol Symbol code
     * @param timeframe Timeframe
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of bars ordered by timestamp ASC
     */
    @Query("SELECT h FROM HistoricalBarEntity h " +
            "WHERE h.symbol = :symbol " +
            "AND h.timeframe = :timeframe " +
            "AND h.barTimestamp >= :startDate " +
            "AND h.barTimestamp <= :endDate " +
            "ORDER BY h.barTimestamp ASC")
    List<HistoricalBarEntity> findBySymbolAndTimeframeAndDateRange(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find bars for multiple symbols in date range.
     *
     * Used for multi-symbol backtesting.
     *
     * @param symbols List of symbols
     * @param timeframe Timeframe
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of bars ordered by timestamp ASC
     */
    @Query("SELECT h FROM HistoricalBarEntity h " +
            "WHERE h.symbol IN :symbols " +
            "AND h.timeframe = :timeframe " +
            "AND h.barTimestamp >= :startDate " +
            "AND h.barTimestamp <= :endDate " +
            "ORDER BY h.barTimestamp ASC")
    List<HistoricalBarEntity> findBySymbolsAndTimeframeAndDateRange(
            @Param("symbols") List<String> symbols,
            @Param("timeframe") String timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count bars for symbol and timeframe in date range.
     *
     * Useful for validation before running backtest.
     *
     * @param symbol Symbol code
     * @param timeframe Timeframe
     * @param startDate Start date
     * @param endDate End date
     * @return Count of bars
     */
    @Query("SELECT COUNT(h) FROM HistoricalBarEntity h " +
            "WHERE h.symbol = :symbol " +
            "AND h.timeframe = :timeframe " +
            "AND h.barTimestamp >= :startDate " +
            "AND h.barTimestamp <= :endDate")
    long countBySymbolAndTimeframeAndDateRange(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find latest bar for symbol and timeframe.
     *
     * @param symbol Symbol code
     * @param timeframe Timeframe
     * @return Latest bar or null if not found
     */
    @Query("SELECT h FROM HistoricalBarEntity h " +
            "WHERE h.symbol = :symbol " +
            "AND h.timeframe = :timeframe " +
            "ORDER BY h.barTimestamp DESC " +
            "LIMIT 1")
    HistoricalBarEntity findLatestBySymbolAndTimeframe(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe
    );

    /**
     * Delete bars older than specified date.
     *
     * Useful for data cleanup and maintenance.
     *
     * @param cutoffDate Cutoff date (bars before this will be deleted)
     */
    void deleteByBarTimestampBefore(LocalDateTime cutoffDate);
}
