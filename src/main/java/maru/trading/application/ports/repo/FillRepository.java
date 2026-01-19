package maru.trading.application.ports.repo;

import maru.trading.domain.execution.Fill;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for Fill domain model.
 *
 * Implementation: FillRepositoryImpl (infrastructure layer)
 */
public interface FillRepository {

    /**
     * Save a fill.
     */
    Fill save(Fill fill);

    /**
     * Find fill by ID.
     */
    Optional<Fill> findById(String fillId);

    /**
     * Find all fills for a specific order.
     */
    List<Fill> findByOrderId(String orderId);

    /**
     * Find fills for an account and symbol within a time range.
     */
    List<Fill> findByAccountAndSymbol(
            String accountId,
            String symbol,
            LocalDateTime from,
            LocalDateTime to);

    /**
     * Find all fills for an account within a time range.
     */
    List<Fill> findByAccount(String accountId, LocalDateTime from, LocalDateTime to);

    /**
     * Check if a fill already exists (for duplicate detection).
     * Checks based on orderId, fillTimestamp, fillPrice, and fillQty.
     */
    boolean existsByOrderIdAndDetails(
            String orderId,
            LocalDateTime fillTimestamp,
            java.math.BigDecimal fillPrice,
            int fillQty);

    /**
     * Find all fills.
     */
    List<Fill> findAll();
}
