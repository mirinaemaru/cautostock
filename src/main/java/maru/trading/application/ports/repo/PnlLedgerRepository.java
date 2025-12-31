package maru.trading.application.ports.repo;

import maru.trading.domain.execution.PnlLedger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository port for PnlLedger domain model.
 *
 * Implementation: PnlLedgerRepositoryImpl (infrastructure layer)
 */
public interface PnlLedgerRepository {

    /**
     * Save a single P&L ledger entry.
     */
    PnlLedger save(PnlLedger ledger);

    /**
     * Save multiple P&L ledger entries (batch).
     */
    List<PnlLedger> saveAll(List<PnlLedger> ledgers);

    /**
     * Find all ledger entries for an account within a time range.
     */
    List<PnlLedger> findByAccount(String accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Find all ledger entries for a specific account and symbol.
     */
    List<PnlLedger> findByAccountAndSymbol(
            String accountId,
            String symbol,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Calculate total realized P&L for an account within a time range.
     * Sums all ledger entry amounts.
     */
    BigDecimal calculateRealizedPnl(String accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Calculate total realized P&L for an account and symbol.
     */
    BigDecimal calculateRealizedPnlBySymbol(
            String accountId,
            String symbol,
            LocalDateTime from,
            LocalDateTime to);
}
