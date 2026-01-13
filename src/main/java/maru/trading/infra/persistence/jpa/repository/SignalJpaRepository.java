package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.SignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for trading signals.
 */
@Repository
public interface SignalJpaRepository extends JpaRepository<SignalEntity, String> {

    /**
     * Find recent signals for a strategy and symbol.
     * Used for duplicate detection and cooldown checks.
     *
     * @param strategyId Strategy ID
     * @param symbol Symbol
     * @param since Cutoff time (only signals after this time)
     * @return List of recent signals
     */
    @Query("SELECT s FROM SignalEntity s " +
            "WHERE s.strategyId = :strategyId AND s.symbol = :symbol " +
            "AND s.createdAt >= :since " +
            "ORDER BY s.createdAt DESC")
    List<SignalEntity> findRecentSignals(
            @Param("strategyId") String strategyId,
            @Param("symbol") String symbol,
            @Param("since") LocalDateTime since);

    /**
     * Find unexecuted signals.
     * Used for workflow processing.
     *
     * @return List of signals that haven't been executed yet
     */
    @Query("SELECT s FROM SignalEntity s " +
            "WHERE s.executedAt IS NULL AND s.expired = false " +
            "ORDER BY s.createdAt ASC")
    List<SignalEntity> findUnexecutedSignals();

    /**
     * Find signals for an account.
     *
     * @param accountId Account ID
     * @param limit Maximum number of signals
     * @return List of signals
     */
    @Query("SELECT s FROM SignalEntity s " +
            "WHERE s.accountId = :accountId " +
            "ORDER BY s.createdAt DESC " +
            "LIMIT :limit")
    List<SignalEntity> findByAccountIdOrderByCreatedAtDesc(
            @Param("accountId") String accountId,
            @Param("limit") int limit);
}
