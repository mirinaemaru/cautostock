package maru.trading.infra.persistence.jpa.repository.impl;

import maru.trading.application.ports.repo.BrokerTokenRepository;
import maru.trading.domain.account.BrokerToken;
import maru.trading.infra.persistence.jpa.entity.BrokerTokenEntity;
import maru.trading.infra.persistence.jpa.repository.BrokerTokenJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of BrokerTokenRepository port.
 * Maps between BrokerToken domain model and BrokerTokenEntity.
 */
@Component
public class BrokerTokenRepositoryImpl implements BrokerTokenRepository {

    private final BrokerTokenJpaRepository jpaRepository;

    public BrokerTokenRepositoryImpl(BrokerTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public BrokerToken save(BrokerToken token) {
        BrokerTokenEntity entity = toEntity(token);
        BrokerTokenEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<BrokerToken> findById(String tokenId) {
        return jpaRepository.findById(tokenId)
                .map(this::toDomain);
    }

    @Override
    public Optional<BrokerToken> findValidToken(String broker, String environment) {
        return jpaRepository.findTopByBrokerAndEnvironmentAndExpiresAtAfterOrderByExpiresAtDesc(
                        broker,
                        environment,
                        LocalDateTime.now())
                .map(this::toDomain);
    }

    @Override
    public List<BrokerToken> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BrokerToken> findByBrokerAndEnvironment(String broker, String environment) {
        return jpaRepository.findByBrokerAndEnvironment(broker, environment).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<BrokerToken> findTokensNeedingRefresh(LocalDateTime now, LocalDateTime expiresBeforeThreshold) {
        return jpaRepository.findByExpiresAtBetween(now, expiresBeforeThreshold).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // Mapping methods
    private BrokerTokenEntity toEntity(BrokerToken domain) {
        BrokerTokenEntity entity = new BrokerTokenEntity();
        entity.setTokenId(domain.getTokenId());
        entity.setBroker(domain.getBroker());
        entity.setEnvironment(domain.getEnvironment());
        entity.setAccessToken(domain.getAccessToken());
        entity.setIssuedAt(domain.getIssuedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        return entity;
    }

    private BrokerToken toDomain(BrokerTokenEntity entity) {
        return new BrokerToken(
                entity.getTokenId(),
                entity.getBroker(),
                entity.getEnvironment(),
                entity.getAccessToken(),
                entity.getIssuedAt(),
                entity.getExpiresAt()
        );
    }
}
