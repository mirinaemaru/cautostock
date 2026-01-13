package maru.trading.application.usecase.strategy;

import maru.trading.application.ports.repo.SignalRepository;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalPolicy;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.Strategy;
import maru.trading.domain.strategy.StrategyVersion;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Use case for generating and persisting trading signals.
 *
 * Responsibilities:
 * 1. Validate signal decision from strategy
 * 2. Check for duplicate signals (cooldown)
 * 3. Persist signal to database
 * 4. Publish signal event to outbox
 */
@Service
public class GenerateSignalUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateSignalUseCase.class);
    private static final int COOLDOWN_SECONDS = 60; // 1 minute cooldown between signals
    private static final int DUPLICATE_LOOKBACK_SECONDS = 300; // Check last 5 minutes for duplicates

    private final SignalRepository signalRepository;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;
    private final SignalPolicy signalPolicy;

    public GenerateSignalUseCase(
            SignalRepository signalRepository,
            OutboxService outboxService,
            UlidGenerator ulidGenerator,
            SignalPolicy signalPolicy) {
        this.signalRepository = signalRepository;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
        this.signalPolicy = signalPolicy;
    }

    /**
     * Execute the use case.
     *
     * @param decision Signal decision from strategy evaluation
     * @param strategy Strategy that generated the signal
     * @param version Strategy version used
     * @param symbol Symbol
     * @param accountId Account ID
     * @return Generated signal (or null if HOLD or duplicate)
     */
    @Transactional
    public Signal execute(
            SignalDecision decision,
            Strategy strategy,
            StrategyVersion version,
            String symbol,
            String accountId) {

        log.debug("Generating signal: strategyId={}, symbol={}, decision={}",
                strategy.getStrategyId(), symbol, decision.getSignalType());

        // Step 1: Validate decision
        signalPolicy.validateSignal(decision);

        // Step 2: Skip if HOLD signal
        if (decision.getSignalType() == SignalType.HOLD) {
            log.debug("HOLD signal, skipping: strategyId={}, symbol={}, reason={}",
                    strategy.getStrategyId(), symbol, decision.getReason());
            return null;
        }

        // Step 3: Check for duplicates (cooldown)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownCutoff = now.minusSeconds(DUPLICATE_LOOKBACK_SECONDS);

        List<Signal> recentSignals = signalRepository.findRecentSignals(
                strategy.getStrategyId(), symbol, cooldownCutoff);

        if (signalPolicy.isDuplicate(recentSignals, decision.getSignalType(),
                DUPLICATE_LOOKBACK_SECONDS, now)) {
            log.info("Duplicate signal detected, skipping: strategyId={}, symbol={}, signalType={}",
                    strategy.getStrategyId(), symbol, decision.getSignalType());
            return null;
        }

        // Step 4: Check cooldown
        if (!recentSignals.isEmpty()) {
            // Simplified: just check if any signal was generated recently
            log.warn("Signal in cooldown period, skipping: strategyId={}, symbol={}",
                    strategy.getStrategyId(), symbol);
            return null;
        }

        // Step 5: Create signal domain model
        String signalId = ulidGenerator.generateInstance();

        Signal signal = Signal.builder()
                .signalId(signalId)
                .strategyId(strategy.getStrategyId())
                .strategyVersionId(version.getStrategyVersionId())
                .accountId(accountId)
                .symbol(symbol)
                .signalType(decision.getSignalType())
                .targetType(decision.getTargetType())
                .targetValue(decision.getTargetValue())
                .ttlSeconds(decision.getTtlSeconds())
                .reason(decision.getReason())
                .build();

        // Step 6: Persist signal
        Signal savedSignal = signalRepository.save(signal);

        log.info("Signal generated and saved: signalId={}, strategyId={}, symbol={}, type={}, reason={}",
                savedSignal.getSignalId(), strategy.getStrategyId(), symbol,
                decision.getSignalType(), decision.getReason());

        // Step 7: Publish event
        publishSignalGeneratedEvent(savedSignal, strategy);

        return savedSignal;
    }

    /**
     * Publish SignalGenerated event to outbox.
     */
    private void publishSignalGeneratedEvent(Signal signal, Strategy strategy) {
        String eventId = ulidGenerator.generateInstance();

        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("SignalGenerated")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "signalId", signal.getSignalId(),
                        "strategyId", signal.getStrategyId(),
                        "strategyName", strategy.getName(),
                        "symbol", signal.getSymbol(),
                        "signalType", signal.getSignalType().name(),
                        "targetValue", signal.getTargetValue() != null ? signal.getTargetValue().toString() : "",
                        "reason", signal.getReason() != null ? signal.getReason() : ""
                ))
                .build();

        outboxService.save(event);

        log.debug("Published SignalGenerated event: eventId={}, signalId={}",
                eventId, signal.getSignalId());
    }
}
