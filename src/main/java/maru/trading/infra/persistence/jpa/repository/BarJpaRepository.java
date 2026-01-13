package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.BarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for market bars.
 */
@Repository
public interface BarJpaRepository extends JpaRepository<BarEntity, String> {

    /**
     * Find bar by symbol, timeframe, and timestamp.
     * Used to check if bar already exists.
     */
    Optional<BarEntity> findBySymbolAndTimeframeAndBarTimestamp(
            String symbol, String timeframe, LocalDateTime barTimestamp);

    /**
     * Find recent bars for a symbol and timeframe.
     * Returns bars ordered by timestamp descending (newest first).
     *
     * @param symbol Symbol
     * @param timeframe Timeframe (e.g., "1m")
     * @param limit Maximum number of bars to return
     * @return List of recent bars
     */
    @Query(value = "SELECT * FROM market_bars " +
            "WHERE symbol = :symbol AND timeframe = :timeframe AND closed = true " +
            "ORDER BY bar_timestamp DESC LIMIT :limit", nativeQuery = true)
    List<BarEntity> findRecentBars(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("limit") int limit);

    /**
     * Find bars within a time range.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @param startTime Start time (inclusive)
     * @param endTime End time (inclusive)
     * @return List of bars ordered by timestamp ascending
     */
    @Query("SELECT b FROM BarEntity b " +
            "WHERE b.symbol = :symbol AND b.timeframe = :timeframe " +
            "AND b.barTimestamp >= :startTime AND b.barTimestamp <= :endTime " +
            "AND b.closed = true " +
            "ORDER BY b.barTimestamp ASC")
    List<BarEntity> findBarsInRange(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find N most recent closed bars for a symbol.
     *
     * @param symbol Symbol
     * @param timeframe Timeframe
     * @param count Number of bars to retrieve
     * @return List of bars ordered by timestamp ascending (oldest first)
     */
    @Query(value = "SELECT * FROM market_bars " +
            "WHERE symbol = :symbol AND timeframe = :timeframe AND closed = true " +
            "ORDER BY bar_timestamp DESC LIMIT :count", nativeQuery = true)
    List<BarEntity> findRecentClosedBars(
            @Param("symbol") String symbol,
            @Param("timeframe") String timeframe,
            @Param("count") int count);
}
