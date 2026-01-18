package maru.trading.infra.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import maru.trading.application.ports.repo.RiskStateRepository;
import maru.trading.domain.risk.OrderFrequencyTracker;
import maru.trading.domain.risk.RiskState;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.RiskStateEntity;
import maru.trading.infra.persistence.jpa.repository.RiskStateJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementation for RiskStateRepository.
 */
@Component
public class RiskStateRepositoryAdapter implements RiskStateRepository {

    private static final Logger log = LoggerFactory.getLogger(RiskStateRepositoryAdapter.class);

    private final RiskStateJpaRepository riskStateJpaRepository;
    private final UlidGenerator ulidGenerator;
    private final ObjectMapper objectMapper;

    public RiskStateRepositoryAdapter(
            RiskStateJpaRepository riskStateJpaRepository,
            UlidGenerator ulidGenerator,
            ObjectMapper objectMapper) {
        this.riskStateJpaRepository = riskStateJpaRepository;
        this.ulidGenerator = ulidGenerator;
        this.objectMapper = objectMapper;
    }

    @Override
    public RiskState save(RiskState state) {
        if (state == null) {
            throw new IllegalArgumentException("Risk state cannot be null");
        }

        // Generate ID if not present
        String riskStateId = state.getRiskStateId();
        if (riskStateId == null || riskStateId.isBlank()) {
            riskStateId = ulidGenerator.generateInstance();
        }

        // Serialize order timestamps to JSON
        String orderTimestampsJson = null;
        if (state.getOrderFrequencyTracker() != null) {
            try {
                List<LocalDateTime> timestamps = state.getOrderFrequencyTracker().getTimestamps();
                orderTimestampsJson = objectMapper.writeValueAsString(timestamps);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize order timestamps", e);
                throw new RuntimeException("Failed to serialize order timestamps", e);
            }
        }

        RiskStateEntity entity = RiskStateEntity.builder()
                .riskStateId(riskStateId)
                .scope(state.getScope())
                .accountId(state.getAccountId())
                .killSwitchStatus(state.getKillSwitchStatus())
                .killSwitchReason(state.getKillSwitchReason())
                .dailyPnl(state.getDailyPnl())
                .exposure(state.getExposure())
                .consecutiveOrderFailures(state.getConsecutiveOrderFailures())
                .openOrderCount(state.getOpenOrderCount())
                .orderTimestamps(orderTimestampsJson)
                .updatedAt(LocalDateTime.now())
                .build();

        RiskStateEntity savedEntity = riskStateJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<RiskState> findById(String riskStateId) {
        return riskStateJpaRepository.findById(riskStateId)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskState> findByAccountId(String accountId) {
        return riskStateJpaRepository.findByScopeAndAccountId("ACCOUNT", accountId)
                .map(this::toDomain);
    }

    @Override
    public Optional<RiskState> findGlobalState() {
        return riskStateJpaRepository.findFirstByScopeOrderByUpdatedAtDesc("GLOBAL")
                .map(this::toDomain);
    }

    /**
     * Convert RiskStateEntity to RiskState domain model.
     */
    private RiskState toDomain(RiskStateEntity entity) {
        // Deserialize order timestamps from JSON
        OrderFrequencyTracker tracker = new OrderFrequencyTracker();
        if (entity.getOrderTimestamps() != null && !entity.getOrderTimestamps().isBlank()) {
            try {
                List<LocalDateTime> timestamps = objectMapper.readValue(
                        entity.getOrderTimestamps(),
                        new TypeReference<List<LocalDateTime>>() {}
                );
                tracker = new OrderFrequencyTracker(timestamps);
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize order timestamps, using empty tracker", e);
                // Use empty tracker on deserialization failure
            }
        }

        return RiskState.builder()
                .riskStateId(entity.getRiskStateId())
                .scope(entity.getScope())
                .accountId(entity.getAccountId())
                .killSwitchStatus(entity.getKillSwitchStatus())
                .killSwitchReason(entity.getKillSwitchReason())
                .dailyPnl(entity.getDailyPnl())
                .exposure(entity.getExposure())
                .consecutiveOrderFailures(entity.getConsecutiveOrderFailures())
                .openOrderCount(entity.getOpenOrderCount())
                .orderFrequencyTracker(tracker)
                .build();
    }
}
