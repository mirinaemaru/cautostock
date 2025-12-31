package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionJpaRepository extends JpaRepository<PositionEntity, String> {

	Optional<PositionEntity> findByAccountIdAndSymbol(String accountId, String symbol);

	List<PositionEntity> findByAccountId(String accountId);
}
