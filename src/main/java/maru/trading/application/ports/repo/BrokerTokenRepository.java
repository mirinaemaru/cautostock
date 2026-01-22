package maru.trading.application.ports.repo;

import maru.trading.domain.account.BrokerToken;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for BrokerToken domain model.
 *
 * Implementation: BrokerTokenRepositoryImpl (infrastructure layer)
 */
public interface BrokerTokenRepository {

    /**
     * Save a broker token.
     */
    BrokerToken save(BrokerToken token);

    /**
     * Find token by ID.
     */
    Optional<BrokerToken> findById(String tokenId);

    /**
     * Find a valid (non-expired) token for a broker and environment.
     * Returns the most recently issued token that has not expired.
     */
    Optional<BrokerToken> findValidToken(String broker, String environment);

    /**
     * Find all tokens (for refresh scheduler).
     */
    List<BrokerToken> findAll();

    /**
     * Find all tokens for a specific broker and environment.
     */
    List<BrokerToken> findByBrokerAndEnvironment(String broker, String environment);

    /**
     * Find tokens that need refresh (expire within the threshold, not yet expired).
     * Used by refresh scheduler for optimized token refresh.
     */
    List<BrokerToken> findTokensNeedingRefresh(java.time.LocalDateTime now, java.time.LocalDateTime expiresBeforeThreshold);
}
