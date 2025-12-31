package maru.trading.infra.persistence.jpa.repository.impl;

import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.domain.execution.Position;
import maru.trading.infra.persistence.jpa.entity.PositionEntity;
import maru.trading.infra.persistence.jpa.repository.PositionJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of PositionRepository port.
 * Maps between Position domain model and PositionEntity.
 */
@Component
public class PositionRepositoryImpl implements PositionRepository {

    private final PositionJpaRepository jpaRepository;

    public PositionRepositoryImpl(PositionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Position save(Position position) {
        PositionEntity entity = toEntity(position);
        PositionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Position> findById(String positionId) {
        return jpaRepository.findById(positionId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Position> findByAccountAndSymbol(String accountId, String symbol) {
        return jpaRepository.findByAccountIdAndSymbol(accountId, symbol)
                .map(this::toDomain);
    }

    @Override
    public List<Position> findAllByAccount(String accountId) {
        return jpaRepository.findByAccountId(accountId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Position> findActiveByAccount(String accountId) {
        return jpaRepository.findByAccountId(accountId).stream()
                .filter(e -> e.getQty().compareTo(BigDecimal.ZERO) != 0) // Filter out zero-quantity positions
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Position upsert(Position position) {
        // Check if position already exists by (accountId, symbol)
        Optional<PositionEntity> existing = jpaRepository.findByAccountIdAndSymbol(
                position.getAccountId(),
                position.getSymbol()
        );

        PositionEntity entity;
        if (existing.isPresent()) {
            // Update existing position by creating new entity with same ID
            entity = PositionEntity.builder()
                    .positionId(existing.get().getPositionId())
                    .accountId(position.getAccountId())
                    .symbol(position.getSymbol())
                    .qty(BigDecimal.valueOf(position.getQty()))
                    .avgPrice(position.getAvgPrice())
                    .realizedPnl(position.getRealizedPnl())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            // Insert new position
            entity = toEntity(position);
        }

        PositionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    // Mapping methods
    private PositionEntity toEntity(Position domain) {
        return PositionEntity.builder()
                .positionId(domain.getPositionId())
                .accountId(domain.getAccountId())
                .symbol(domain.getSymbol())
                .qty(BigDecimal.valueOf(domain.getQty()))
                .avgPrice(domain.getAvgPrice())
                .realizedPnl(domain.getRealizedPnl())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Position toDomain(PositionEntity entity) {
        return new Position(
                entity.getPositionId(),
                entity.getAccountId(),
                entity.getSymbol(),
                entity.getQty().intValue(),
                entity.getAvgPrice(),
                entity.getRealizedPnl()
        );
    }
}
