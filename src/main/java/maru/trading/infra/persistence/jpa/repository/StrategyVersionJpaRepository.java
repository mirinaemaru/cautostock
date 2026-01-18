package maru.trading.infra.persistence.jpa.repository;

import maru.trading.infra.persistence.jpa.entity.StrategyVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface StrategyVersionJpaRepository extends JpaRepository<StrategyVersionEntity, String> {

	List<StrategyVersionEntity> findByStrategyIdOrderByVersionNoDesc(String strategyId);

	Optional<StrategyVersionEntity> findByStrategyIdAndVersionNo(String strategyId, Integer versionNo);

	@Query("SELECT MAX(v.versionNo) FROM StrategyVersionEntity v WHERE v.strategyId = :strategyId")
	Optional<Integer> findMaxVersionNoByStrategyId(@Param("strategyId") String strategyId);

	@Modifying
	@Transactional
	void deleteByStrategyId(String strategyId);
}
