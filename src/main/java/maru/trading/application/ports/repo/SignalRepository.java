package maru.trading.application.ports.repo;

import maru.trading.domain.signal.Signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for trading signals.
 */
public interface SignalRepository {

    /**
     * Save a signal.
     *
     * @param signal Signal to save
     * @return Saved signal
     */
    Signal save(Signal signal);

    /**
     * Find signal by ID.
     *
     * @param signalId Signal ID
     * @return Optional containing signal if found
     */
    Optional<Signal> findById(String signalId);

    /**
     * Find recent signals for a strategy and symbol.
     * Used for duplicate detection and cooldown checks.
     *
     * @param strategyId Strategy ID
     * @param symbol Symbol
     * @param since Cutoff time (only signals after this time)
     * @return List of recent signals
     */
    List<Signal> findRecentSignals(String strategyId, String symbol, LocalDateTime since);

    /**
     * Find unexecuted signals.
     *
     * @return List of signals that haven't been executed yet
     */
    List<Signal> findUnexecutedSignals();

    /**
     * Find signals for an account.
     *
     * @param accountId Account ID
     * @param limit Maximum number of signals
     * @return List of signals (newest first)
     */
    List<Signal> findByAccountIdOrderByCreatedAtDesc(String accountId, int limit);
}
