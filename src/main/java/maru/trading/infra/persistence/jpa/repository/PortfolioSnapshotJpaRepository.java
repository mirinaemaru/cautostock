package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.PortfolioSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PortfolioSnapshotEntity.
 */
@Repository
public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotEntity, String> {

    /**
     * Find the latest snapshot for an account.
     */
    Optional<PortfolioSnapshotEntity> findTopByAccountIdOrderBySnapshotTsDesc(String accountId);

    /**
     * Find all snapshots for an account within a date range.
     */
    List<PortfolioSnapshotEntity> findByAccountIdAndSnapshotTsBetween(
            String accountId,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Find all snapshots for an account.
     */
    List<PortfolioSnapshotEntity> findByAccountIdOrderBySnapshotTsDesc(String accountId);
}
