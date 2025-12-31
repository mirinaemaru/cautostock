package maru.trading.application.ports.repo;

import maru.trading.domain.execution.Position;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Position domain model.
 *
 * Implementation: PositionRepositoryImpl (infrastructure layer)
 */
public interface PositionRepository {

    /**
     * Save a position.
     */
    Position save(Position position);

    /**
     * Find position by ID.
     */
    Optional<Position> findById(String positionId);

    /**
     * Find position for a specific account and symbol.
     * Returns empty if no position exists.
     */
    Optional<Position> findByAccountAndSymbol(String accountId, String symbol);

    /**
     * Find all positions for an account.
     * Includes positions with qty=0 (historical closed positions).
     */
    List<Position> findAllByAccount(String accountId);

    /**
     * Find all active (non-zero qty) positions for an account.
     */
    List<Position> findActiveByAccount(String accountId);

    /**
     * Upsert a position (insert if not exists, update if exists).
     * Uses (accountId, symbol) as the unique key.
     */
    Position upsert(Position position);
}
