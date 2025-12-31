package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiskStateJpaRepository extends JpaRepository<RiskStateEntity, String> {

	Optional<RiskStateEntity> findByScopeAndAccountId(String scope, String accountId);

	Optional<RiskStateEntity> findByScope(String scope);
}
