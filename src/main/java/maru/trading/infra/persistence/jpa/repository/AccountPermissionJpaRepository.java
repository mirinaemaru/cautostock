package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.AccountPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Account Permission JPA Repository
 */
@Repository
public interface AccountPermissionJpaRepository extends JpaRepository<AccountPermissionEntity, String> {
}
