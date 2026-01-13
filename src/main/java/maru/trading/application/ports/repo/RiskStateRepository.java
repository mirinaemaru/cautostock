package maru.trading.application.ports.repo;

import maru.trading.domain.risk.RiskState;

import java.util.Optional;

/**
 * Repository port for RiskState domain model.
 *
 * Implementation: RiskStateRepositoryAdapter (infrastructure layer)
 */
public interface RiskStateRepository {

    /**
     * Save or update a risk state.
     */
    RiskState save(RiskState state);

    /**
     * Find risk state by ID.
     */
    Optional<RiskState> findById(String riskStateId);

    /**
     * Find risk state for a specific account.
     * Returns empty if no state exists.
     */
    Optional<RiskState> findByAccountId(String accountId);

    /**
     * Find global risk state.
     * Returns empty if no global state exists.
     */
    Optional<RiskState> findGlobalState();
}
