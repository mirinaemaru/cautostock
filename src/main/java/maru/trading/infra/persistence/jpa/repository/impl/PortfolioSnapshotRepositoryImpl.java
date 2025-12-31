package maru.trading.infra.persistence.jpa.repository.impl;

import maru.trading.application.ports.repo.PortfolioSnapshotRepository;
import maru.trading.domain.execution.PortfolioSnapshot;
import maru.trading.infra.persistence.jpa.entity.PortfolioSnapshotEntity;
import maru.trading.infra.persistence.jpa.repository.PortfolioSnapshotJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PortfolioSnapshotRepository port.
 * Maps between PortfolioSnapshot domain model and PortfolioSnapshotEntity.
 */
@Component
public class PortfolioSnapshotRepositoryImpl implements PortfolioSnapshotRepository {

    private final PortfolioSnapshotJpaRepository jpaRepository;

    public PortfolioSnapshotRepositoryImpl(PortfolioSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PortfolioSnapshot save(PortfolioSnapshot snapshot) {
        PortfolioSnapshotEntity entity = toEntity(snapshot);
        PortfolioSnapshotEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<PortfolioSnapshot> findById(String snapshotId) {
        return jpaRepository.findById(snapshotId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PortfolioSnapshot> findLatestByAccount(String accountId) {
        return jpaRepository.findTopByAccountIdOrderBySnapshotTsDesc(accountId)
                .map(this::toDomain);
    }

    @Override
    public List<PortfolioSnapshot> findByAccountAndDateRange(String accountId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByAccountIdAndSnapshotTsBetween(accountId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PortfolioSnapshot> findAllByAccount(String accountId) {
        return jpaRepository.findByAccountIdOrderBySnapshotTsDesc(accountId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private PortfolioSnapshotEntity toEntity(PortfolioSnapshot domain) {
        PortfolioSnapshotEntity entity = new PortfolioSnapshotEntity();
        entity.setSnapshotId(domain.getSnapshotId());
        entity.setAccountId(domain.getAccountId());
        entity.setTotalValue(domain.getTotalValue());
        entity.setCash(domain.getCash());
        entity.setRealizedPnl(domain.getRealizedPnl());
        entity.setUnrealizedPnl(domain.getUnrealizedPnl());
        entity.setSnapshotTs(domain.getSnapshotTimestamp());
        return entity;
    }

    private PortfolioSnapshot toDomain(PortfolioSnapshotEntity entity) {
        return new PortfolioSnapshot(
                entity.getSnapshotId(),
                entity.getAccountId(),
                entity.getTotalValue(),
                entity.getCash(),
                entity.getRealizedPnl(),
                entity.getUnrealizedPnl(),
                entity.getSnapshotTs()
        );
    }
}
