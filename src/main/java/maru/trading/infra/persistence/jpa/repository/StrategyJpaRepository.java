package maru.trading.infra.persistence.jpa.repository;

import maru.trading.domain.shared.Environment;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyJpaRepository extends JpaRepository<StrategyEntity, String> {

	Optional<StrategyEntity> findByName(String name);

	List<StrategyEntity> findByStatusAndMode(String status, Environment mode);

	List<StrategyEntity> findByStatus(String status);

	// 소프트 삭제 지원 메서드
	Optional<StrategyEntity> findByStrategyIdAndDelyn(String strategyId, String delyn);

	Optional<StrategyEntity> findByNameAndDelyn(String name, String delyn);

	List<StrategyEntity> findByDelyn(String delyn);

	List<StrategyEntity> findByStatusAndDelyn(String status, String delyn);

	List<StrategyEntity> findByStatusAndModeAndDelyn(String status, Environment mode, String delyn);
}
