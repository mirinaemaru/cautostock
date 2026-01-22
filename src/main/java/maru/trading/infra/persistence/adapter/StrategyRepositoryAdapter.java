package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.StrategyRepository;
import maru.trading.domain.shared.Environment;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyVersion;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.StrategyEntity;
import maru.trading.infra.persistence.jpa.entity.StrategyVersionEntity;
import maru.trading.infra.persistence.jpa.repository.StrategyJpaRepository;
import maru.trading.infra.persistence.jpa.repository.StrategyVersionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public Strategy save(Strategy strategy) {
        StrategyEntity entity;

        if (strategy.getStrategyId() != null) {
            // Update existing strategy
            Optional<StrategyEntity> existingOpt = strategyJpaRepository.findById(strategy.getStrategyId());
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                entity.setName(strategy.getName());
                entity.setDescription(strategy.getDescription());
                entity.setStatus(strategy.getStatus());
                entity.setMode(strategy.getMode());
                if (strategy.getActiveVersionId() != null) {
                    entity.setActiveVersionId(strategy.getActiveVersionId());
                }
            } else {
                // ID provided but not found - create new with given ID
                entity = toEntity(strategy);
            }
        } else {
            // Create new strategy with generated ID
            String newId = UlidGenerator.generate();
            entity = toEntity(Strategy.builder()
                    .strategyId(newId)
                    .name(strategy.getName())
                    .description(strategy.getDescription())
                    .status(strategy.getStatus() != null ? strategy.getStatus() : "INACTIVE")
                    .mode(strategy.getMode() != null ? strategy.getMode() : Environment.PAPER)
                    .activeVersionId(strategy.getActiveVersionId())
                    .build());

            // If no activeVersionId, create initial version
            if (entity.getActiveVersionId() == null) {
                String versionId = UlidGenerator.generate();
                StrategyVersionEntity versionEntity = StrategyVersionEntity.builder()
                        .strategyVersionId(versionId)
                        .strategyId(newId)
                        .versionNo(1)
                        .paramsJson("{}")
                        .build();
                strategyVersionJpaRepository.save(versionEntity);
                entity.setActiveVersionId(versionId);
            }
        }

        StrategyEntity saved = strategyJpaRepository.save(entity);
        return toDomain(saved);
    }

    private StrategyEntity toEntity(Strategy domain) {
        return StrategyEntity.builder()
                .strategyId(domain.getStrategyId())
                .name(domain.getName())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .mode(domain.getMode())
                .activeVersionId(domain.getActiveVersionId())
                .delyn("N")
                .build();
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
