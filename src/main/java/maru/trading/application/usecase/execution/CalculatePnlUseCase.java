package maru.trading.application.usecase.execution;

import maru.trading.application.ports.repo.PortfolioSnapshotRepository;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.domain.execution.PortfolioSnapshot;
import maru.trading.domain.execution.Position;
import maru.trading.infra.cache.MarketDataCache;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use case for calculating P&L and creating portfolio snapshots.
 *
 * This use case:
 * 1. Loads all positions for an account
 * 2. Fetches current prices from market data cache
 * 3. Calculates unrealized P&L
 * 4. Creates and saves portfolio snapshot
 * 5. Publishes PnlUpdated event
 */
@Service
public class CalculatePnlUseCase {

    private static final Logger log = LoggerFactory.getLogger(CalculatePnlUseCase.class);

    private final PositionRepository positionRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final MarketDataCache marketDataCache;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;

    public CalculatePnlUseCase(
            PositionRepository positionRepository,
            PortfolioSnapshotRepository snapshotRepository,
            MarketDataCache marketDataCache,
            OutboxService outboxService,
            UlidGenerator ulidGenerator) {
        this.positionRepository = positionRepository;
        this.snapshotRepository = snapshotRepository;
        this.marketDataCache = marketDataCache;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Execute the use case to calculate P&L and create snapshot.
     *
     * @param accountId Account ID
     * @param cash Current cash balance
     * @return Created portfolio snapshot
     */
    @Transactional
    public PortfolioSnapshot execute(String accountId, BigDecimal cash) {
        log.info("Calculating P&L for account: {}", accountId);

        // Step 1: Load all positions for the account
        List<Position> positions = positionRepository.findAllByAccount(accountId);
        log.debug("Loaded {} positions for account {}", positions.size(), accountId);

        // Step 2: Fetch current prices from market data cache
        Map<String, BigDecimal> currentPrices = new HashMap<>();
        for (Position position : positions) {
            if (!position.isFlat()) {
                BigDecimal currentPrice = marketDataCache.getPrice(position.getSymbol());
                if (currentPrice != null) {
                    currentPrices.put(position.getSymbol(), currentPrice);
                } else {
                    log.warn("No current price available for symbol: {}, using avgPrice as fallback",
                            position.getSymbol());
                    currentPrices.put(position.getSymbol(), position.getAvgPrice());
                }
            }
        }

        // Step 3: Calculate portfolio snapshot
        String snapshotId = ulidGenerator.generateInstance();
        PortfolioSnapshot snapshot = PortfolioSnapshot.calculate(
                snapshotId,
                accountId,
                positions,
                currentPrices,
                cash
        );

        log.info("Portfolio snapshot calculated: totalValue={}, realizedPnl={}, unrealizedPnl={}",
                snapshot.getTotalValue(),
                snapshot.getRealizedPnl(),
                snapshot.getUnrealizedPnl());

        // Step 4: Save snapshot
        PortfolioSnapshot savedSnapshot = snapshotRepository.save(snapshot);
        log.debug("Portfolio snapshot saved: {}", savedSnapshot.getSnapshotId());

        // Step 5: Publish PnlUpdated event
        publishPnlUpdatedEvent(savedSnapshot);

        return savedSnapshot;
    }

    /**
     * Execute without cash parameter (fetch from account or use 0).
     * For MVP, we use 0 as default cash.
     */
    @Transactional
    public PortfolioSnapshot execute(String accountId) {
        // TODO: In production, fetch cash balance from account service
        return execute(accountId, BigDecimal.ZERO);
    }

    private void publishPnlUpdatedEvent(PortfolioSnapshot snapshot) {
        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("PnlUpdated")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "accountId", snapshot.getAccountId(),
                        "snapshotId", snapshot.getSnapshotId(),
                        "totalValue", snapshot.getTotalValue(),
                        "realizedPnl", snapshot.getRealizedPnl(),
                        "unrealizedPnl", snapshot.getUnrealizedPnl(),
                        "snapshotTs", snapshot.getSnapshotTimestamp()
                ))
                .build();
        outboxService.save(event);
        log.debug("Published PnlUpdated event: {}", eventId);
    }
}
