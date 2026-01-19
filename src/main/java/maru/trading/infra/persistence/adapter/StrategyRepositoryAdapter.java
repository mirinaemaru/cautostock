package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyVersion;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyVersionEntity;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyVersionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for StrategyRepository.
 */
@Component
public class StrategyRepositoryAdapter implements StrategyRepository {

    private final StrategyJpaRepository strategyJpaRepository;
    private final StrategyVersionJpaRepository strategyVersionJpaRepository;

    public StrategyRepositoryAdapter(
            StrategyJpaRepository strategyJpaRepository,
            StrategyVersionJpaRepository strategyVersionJpaRepository) {
        this.strategyJpaRepository = strategyJpaRepository;
        this.strategyVersionJpaRepository = strategyVersionJpaRepository;
    }

    @Override
    public Optional<Strategy> findById(String strategyId) {
        return strategyJpaRepository.findByStrategyIdAndDelyn(strategyId, "N")
                .map(this::toDomain);
    }

    @Override
    public Optional<Strategy> findByName(String name) {
        return strategyJpaRepository.findByNameAndDelyn(name, "N")
                .map(this::toDomain);
    }

    @Override
    public List<Strategy> findActiveStrategies() {
        return strategyJpaRepository.findByStatusAndDelyn("ACTIVE", "N").stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<StrategyVersion> findVersionById(String versionId) {
        return strategyVersionJpaRepository.findById(versionId)
                .map(this::toVersionDomain);
    }

    @Override
    public Strategy save(Strategy strategy) {
        // Note: This is a simplified implementation
        // In a real system, you'd handle creation timestamps, etc.
        throw new UnsupportedOperationException("Strategy save not implemented yet");
    }

    private Strategy toDomain(StrategyEntity entity) {
        return Strategy.builder()
                .strategyId(entity.getStrategyId())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .mode(entity.getMode())
                .activeVersionId(entity.getActiveVersionId())
                .build();
    }

    private StrategyVersion toVersionDomain(StrategyVersionEntity entity) {
        return StrategyVersion.builder()
                .strategyVersionId(entity.getStrategyVersionId())
                .strategyId(entity.getStrategyId())
                .versionNo(entity.getVersionNo())
                .paramsJson(entity.getParamsJson())
                .build();
    }
}
