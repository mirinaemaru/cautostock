package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.PnlLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for PnlLedgerEntity.
 */
@Repository
public interface PnlLedgerJpaRepository extends JpaRepository<PnlLedgerEntity, String> {

    /**
     * Find ledger entries by account and time range.
     */
    List<PnlLedgerEntity> findByAccountIdAndEventTsBetween(
            String accountId,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Find ledger entries by account, symbol, and time range.
     */
    List<PnlLedgerEntity> findByAccountIdAndSymbolAndEventTsBetween(
            String accountId,
            String symbol,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Calculate total P&L for an account within a time range.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PnlLedgerEntity p " +
           "WHERE p.accountId = :accountId " +
           "AND p.eventTs BETWEEN :from AND :to")
    BigDecimal sumAmountByAccountAndDateRange(
            @Param("accountId") String accountId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Calculate total P&L for an account and symbol within a time range.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PnlLedgerEntity p " +
           "WHERE p.accountId = :accountId " +
           "AND p.symbol = :symbol " +
           "AND p.eventTs BETWEEN :from AND :to")
    BigDecimal sumAmountByAccountSymbolAndDateRange(
            @Param("accountId") String accountId,
            @Param("symbol") String symbol,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
