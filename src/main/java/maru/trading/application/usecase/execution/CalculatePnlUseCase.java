package maru.trading.application.usecase.execution;

import maru.trading.application.ports.repo.AccountRepository;
import maru.trading.application.ports.repo.PortfolioSnapshotRepository;
import maru.trading.application.ports.repo.PositionRepository;
import maru.trading.broker.kis.api.KisBalanceApiClient;
import maru.trading.broker.kis.dto.KisBalanceResponse;
import maru.trading.domain.account.Account;
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
import java.util.Optional;

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
    private final AccountRepository accountRepository;
    private final MarketDataCache marketDataCache;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;
    private final KisBalanceApiClient balanceApiClient;

    public CalculatePnlUseCase(
            PositionRepository positionRepository,
            PortfolioSnapshotRepository snapshotRepository,
            AccountRepository accountRepository,
            MarketDataCache marketDataCache,
            OutboxService outboxService,
            UlidGenerator ulidGenerator,
            KisBalanceApiClient balanceApiClient) {
        this.positionRepository = positionRepository;
        this.snapshotRepository = snapshotRepository;
        this.accountRepository = accountRepository;
        this.marketDataCache = marketDataCache;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
        this.balanceApiClient = balanceApiClient;
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
     * Execute without cash parameter - fetches cash balance from KIS API.
     * Falls back to 0 if API call fails or account not found.
     */
    @Transactional
    public PortfolioSnapshot execute(String accountId) {
        BigDecimal cashBalance = fetchCashBalance(accountId);
        return execute(accountId, cashBalance);
    }

    /**
     * Fetch cash balance from KIS Balance API.
     *
     * @param accountId Account ID
     * @return Cash balance or BigDecimal.ZERO if unable to fetch
     */
    private BigDecimal fetchCashBalance(String accountId) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (accountOpt.isEmpty()) {
                log.warn("Account not found for cash balance lookup: {}", accountId);
                return BigDecimal.ZERO;
            }

            Account account = accountOpt.get();
            KisBalanceResponse balanceResponse = balanceApiClient.getBalance(
                    account.getCano(),
                    account.getAcntPrdtCd(),
                    account.getEnvironment()
            );

            if (balanceResponse.isSuccess()) {
                BigDecimal cash = balanceResponse.getCashBalance();
                log.info("Fetched cash balance from KIS API: accountId={}, cash={}", accountId, cash);
                return cash;
            } else {
                log.warn("KIS Balance API returned error: {}", balanceResponse.getMsg1());
                return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch cash balance from KIS API, using 0: accountId={}, error={}",
                    accountId, e.getMessage());
            return BigDecimal.ZERO;
        }
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
