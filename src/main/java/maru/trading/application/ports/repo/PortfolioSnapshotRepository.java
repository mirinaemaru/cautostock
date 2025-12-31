package maru.trading.application.ports.repo;

import maru.trading.domain.execution.PortfolioSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for PortfolioSnapshot domain model.
 *
 * Implementation: PortfolioSnapshotRepositoryImpl (infrastructure layer)
 */
public interface PortfolioSnapshotRepository {

    /**
     * Save a portfolio snapshot.
     */
    PortfolioSnapshot save(PortfolioSnapshot snapshot);

    /**
     * Find snapshot by ID.
     */
    Optional<PortfolioSnapshot> findById(String snapshotId);

    /**
     * Find the latest snapshot for an account.
     */
    Optional<PortfolioSnapshot> findLatestByAccount(String accountId);

    /**
     * Find all snapshots for an account within a date range.
     */
    List<PortfolioSnapshot> findByAccountAndDateRange(
            String accountId,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Find all snapshots for an account.
     */
    List<PortfolioSnapshot> findAllByAccount(String accountId);
}
