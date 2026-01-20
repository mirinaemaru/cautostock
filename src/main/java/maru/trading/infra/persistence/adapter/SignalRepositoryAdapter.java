package maru.trading.infra.persistence.adapter;

import maru.trading.application.ports.repo.SignalRepository;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalType;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.SignalEntity;
import maru.trading.infra.persistence.jpa.repository.SignalJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementation for SignalRepository.
 */
@Component
public class SignalRepositoryAdapter implements SignalRepository {

    private final SignalJpaRepository signalJpaRepository;
    private final UlidGenerator ulidGenerator;

    public SignalRepositoryAdapter(SignalJpaRepository signalJpaRepository, UlidGenerator ulidGenerator) {
        this.signalJpaRepository = signalJpaRepository;
        this.ulidGenerator = ulidGenerator;
    }

    @Override
    @Transactional
    public Signal save(Signal signal) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }

        // Generate ID if not present
        String signalId = signal.getSignalId();
        if (signalId == null || signalId.isBlank()) {
            signalId = ulidGenerator.generateInstance();
        }

        SignalEntity entity = SignalEntity.builder()
                .signalId(signalId)
                .strategyId(signal.getStrategyId())
                .strategyVersionId(signal.getStrategyVersionId())
                .accountId(signal.getAccountId())
                .symbol(signal.getSymbol())
                .signalType(signal.getSignalType().name())
                .targetType(signal.getTargetType())
                .targetValue(signal.getTargetValue())
                .ttlSeconds(signal.getTtlSeconds())
                .reason(signal.getReason())
                .build();

        SignalEntity savedEntity = signalJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Signal> findById(String signalId) {
        return signalJpaRepository.findById(signalId)
                .map(this::toDomain);
    }

    @Override
    public List<Signal> findRecentSignals(String strategyId, String symbol, LocalDateTime since) {
        return signalJpaRepository.findRecentSignals(strategyId, symbol, since).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Signal> findUnexecutedSignals() {
        return signalJpaRepository.findUnexecutedSignals().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Signal> findByAccountIdOrderByCreatedAtDesc(String accountId, int limit) {
        return signalJpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId, limit).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Signal toDomain(SignalEntity entity) {
        return Signal.builder()
                .signalId(entity.getSignalId())
                .strategyId(entity.getStrategyId())
                .strategyVersionId(entity.getStrategyVersionId())
                .accountId(entity.getAccountId())
                .symbol(entity.getSymbol())
                .signalType(SignalType.valueOf(entity.getSignalType()))
                .targetType(entity.getTargetType())
                .targetValue(entity.getTargetValue())
                .ttlSeconds(entity.getTtlSeconds())
                .reason(entity.getReason())
                .build();
    }
}
