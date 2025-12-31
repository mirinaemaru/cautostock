package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.BrokerTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for BrokerTokenEntity.
 */
@Repository
public interface BrokerTokenJpaRepository extends JpaRepository<BrokerTokenEntity, String> {

    /**
     * Find a valid (non-expired) token for a broker and environment.
     * Returns the most recently issued token that has not expired.
     */
    Optional<BrokerTokenEntity> findTopByBrokerAndEnvironmentAndExpiresAtAfterOrderByExpiresAtDesc(
            String broker,
            String environment,
            LocalDateTime now);

    /**
     * Find all tokens for a specific broker and environment.
     */
    List<BrokerTokenEntity> findByBrokerAndEnvironment(String broker, String environment);
}
