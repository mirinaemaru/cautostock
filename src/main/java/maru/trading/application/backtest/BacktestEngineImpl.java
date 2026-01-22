package maru.trading.application.backtest;

import maru.trading.domain.backtest.*;
import maru.trading.domain.execution.Fill;
import maru.trading.domain.market.MarketBar;
import maru.trading.domain.order.Order;
import maru.trading.domain.order.OrderStatus;
import maru.trading.domain.order.OrderType;
import maru.trading.domain.order.Side;
import maru.trading.domain.signal.Signal;
import maru.trading.domain.signal.SignalDecision;
import maru.trading.domain.signal.SignalType;
import maru.trading.domain.strategy.StrategyContext;
import maru.trading.domain.strategy.StrategyEngine;
import maru.trading.domain.strategy.StrategyFactory;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.persistence.jpa.entity.BacktestRunEntity;
import maru.trading.infra.persistence.jpa.entity.BacktestTradeEntity;
import maru.trading.infra.persistence.jpa.entity.HistoricalBarEntity;
import maru.trading.infra.persistence.jpa.repository.BacktestRunJpaRepository;
import maru.trading.infra.persistence.jpa.repository.BacktestTradeJpaRepository;
import maru.trading.infra.persistence.jpa.repository.HistoricalBarJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import maru.trading.infra.async.BacktestJobExecutor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backtest Engine implementation.
 *
 * Main orchestrator for running backtest simulations.
 * Coordinates data replay, strategy execution, order simulation, and performance analysis.
 */
@Service
public class BacktestEngineImpl implements BacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(BacktestEngineImpl.class);

    private final DataReplayEngine dataReplayEngine;
    private final VirtualBroker virtualBroker;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final HistoricalBarJpaRepository historicalBarRepository;
    private final BacktestRunJpaRepository backtestRunRepository;
    private final BacktestTradeJpaRepository backtestTradeRepository;
    private final BacktestJobExecutor jobExecutor;

    // Track running backtests (for async support)
    private final Map<String, String> runningBacktests = new ConcurrentHashMap<>();

    public BacktestEngineImpl(
            DataReplayEngine dataReplayEngine,
            VirtualBroker virtualBroker,
            PerformanceAnalyzer performanceAnalyzer,
            HistoricalBarJpaRepository historicalBarRepository,
            BacktestRunJpaRepository backtestRunRepository,
            BacktestTradeJpaRepository backtestTradeRepository,
            BacktestJobExecutor jobExecutor) {
        this.dataReplayEngine = dataReplayEngine;
        this.virtualBroker = virtualBroker;
        this.performanceAnalyzer = performanceAnalyzer;
        this.historicalBarRepository = historicalBarRepository;
        this.backtestRunRepository = backtestRunRepository;
        this.backtestTradeRepository = backtestTradeRepository;
        this.jobExecutor = jobExecutor;
    }

    @Override
    public BacktestResult run(BacktestConfig config) throws BacktestException {
        log.info("========================================");
        log.info("Starting Backtest");
        log.info("========================================");
        log.info("Backtest ID: {}", config.getBacktestId());
        log.info("Strategy: {}", config.getStrategyId());
        log.info("Period: {} to {}", config.getStartDate(), config.getEndDate());
        log.info("Symbols: {}", config.getSymbols());
        log.info("Initial Capital: {}", config.getInitialCapital());

        // Validate configuration
        validateConfig(config);

        // Save backtest run to DB
        BacktestRunEntity runEntity = createBacktestRun(config);
        runningBacktests.put(config.getBacktestId(), "RUNNING");

        try {
            // Execute backtest
            BacktestResult result = executeBacktest(config);

            // Update run entity with results
            updateBacktestRun(runEntity, result);

            // Save trades to DB
            saveTrades(config.getBacktestId(), result.getTrades());

            runningBacktests.put(config.getBacktestId(), "COMPLETED");
            log.info("Backtest completed successfully");
            log.info("Final Capital: {}", result.getFinalCapital());
            log.info("Total Return: {}%", result.getTotalReturn());
            log.info("Total Trades: {}", result.getTrades().size());

            return result;

        } catch (Exception e) {
            log.error("Backtest execution failed", e);
            runEntity.fail(e.getMessage());
            backtestRunRepository.save(runEntity);
            runningBacktests.put(config.getBacktestId(), "FAILED");
            throw new BacktestException("Backtest execution failed: " + e.getMessage(), e);
        }
    }

    private BacktestResult executeBacktest(BacktestConfig config) {
        LocalDateTime startTime = LocalDateTime.now();

        // Initialize components
        dataReplayEngine.loadData(config);
        virtualBroker.reset(config.getInitialCapital());
        virtualBroker.setCommission(config.getCommission());
        virtualBroker.setSlippage(config.getSlippage());

        // Create strategy using factory (dynamic strategy selection)
        String strategyType = config.getStrategyType();
        if (strategyType == null || strategyType.isBlank()) {
            strategyType = "MA_CROSSOVER"; // Default strategy
        }
        log.info("Creating strategy: {}", strategyType);
        StrategyEngine strategy = StrategyFactory.createStrategy(strategyType);

        // Result collectors
        List<Signal> allSignals = new ArrayList<>();
        List<Order> allOrders = new ArrayList<>();
        List<Fill> allFills = new ArrayList<>();
        List<BacktestTrade> allTrades = new ArrayList<>();

        // Track open positions for trade matching
        Map<String, BacktestTrade> openPositions = new HashMap<>();

        // Buffer bars for strategy context
        List<MarketBar> barBuffer = new ArrayList<>();

        // Replay data and execute strategy
        int barCount = 0;
        while (dataReplayEngine.hasNext()) {
            HistoricalBarEntity barEntity = dataReplayEngine.next();
            barCount++;

            // Convert to MarketBar and add to buffer
            MarketBar bar = convertToMarketBar(barEntity);
            barBuffer.add(bar);

            // Execute strategy when we have enough bars
            if (barBuffer.size() >= 21) { // Minimum bars for MA(20) strategy
                StrategyContext context = StrategyContext.builder()
                        .strategyId(config.getStrategyId())
                        .symbol(barEntity.getSymbol())
                        .accountId("BACKTEST_ACCOUNT")
                        .bars(new ArrayList<>(barBuffer))
                        .params(config.getStrategyParams())
                        .timeframe(config.getTimeframe())
                        .build();

                SignalDecision decision = strategy.evaluate(context);
                if (decision != null && decision.getSignalType() != SignalType.HOLD) {
                    // Create signal from decision
                    Signal signal = Signal.builder()
                            .signalId(UlidGenerator.generate())
                            .strategyId(config.getStrategyId())
                            .accountId("BACKTEST_ACCOUNT")
                            .symbol(barEntity.getSymbol())
                            .signalType(decision.getSignalType())
                            .targetType("QTY")
                            .targetValue(decision.getTargetValue())
                            .reason(decision.getReason())
                            .ttlSeconds(decision.getTtlSeconds())
                            .build();

                    allSignals.add(signal);

                    // Convert signal to order
                    Order order = convertSignalToOrder(signal, bar, config);
                    allOrders.add(order);

                    // Submit order to virtual broker
                    virtualBroker.submitOrder(order);
                }

                // Keep only recent bars (sliding window)
                if (barBuffer.size() > 100) {
                    barBuffer.remove(0);
                }
            }

            // Process fills
            List<Fill> fills = virtualBroker.processBar(barEntity);
            allFills.addAll(fills);

            // Match fills to trades
            for (Fill fill : fills) {
                processFill(fill, openPositions, allTrades, config);
            }
        }

        LocalDateTime endTime = LocalDateTime.now();

        log.info("Processed {} bars", barCount);
        log.info("Generated {} signals", allSignals.size());
        log.info("Placed {} orders", allOrders.size());
        log.info("Executed {} fills", allFills.size());
        log.info("Completed {} trades", allTrades.size());

        // Build result
        BigDecimal finalCapital = virtualBroker.getCashBalance();
        BigDecimal totalReturn = calculateTotalReturn(config.getInitialCapital(), finalCapital);

        BacktestResult result = BacktestResult.builder()
                .backtestId(config.getBacktestId())
                .config(config)
                .startTime(startTime)
                .endTime(endTime)
                .signals(allSignals)
                .orders(allOrders)
                .fills(allFills)
                .trades(allTrades)
                .finalCapital(finalCapital)
                .totalReturn(totalReturn)
                .build();

        // Calculate performance metrics
        PerformanceMetrics performanceMetrics = performanceAnalyzer.analyze(result);
        RiskMetrics riskMetrics = performanceAnalyzer.analyzeRisk(result);
        EquityCurve equityCurve = performanceAnalyzer.generateEquityCurve(result);

        result = BacktestResult.builder()
                .backtestId(result.getBacktestId())
                .config(result.getConfig())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .signals(result.getSignals())
                .orders(result.getOrders())
                .fills(result.getFills())
                .trades(result.getTrades())
                .finalCapital(result.getFinalCapital())
                .totalReturn(result.getTotalReturn())
                .performanceMetrics(performanceMetrics)
                .riskMetrics(riskMetrics)
                .equityCurve(equityCurve)
                .build();

        return result;
    }

    private void processFill(Fill fill, Map<String, BacktestTrade> openPositions,
                             List<BacktestTrade> allTrades, BacktestConfig config) {
        String symbol = fill.getSymbol();

        if (fill.getSide() == Side.BUY) {
            // Open new position
            BacktestTrade trade = BacktestTrade.builder()
                    .tradeId(UlidGenerator.generate())
                    .backtestId(config.getBacktestId())
                    .symbol(symbol)
                    .side(fill.getSide())
                    .entryTime(fill.getFillTimestamp())
                    .entryPrice(fill.getFillPrice())
                    .entryQty(BigDecimal.valueOf(fill.getFillQty()))
                    .status("OPEN")
                    .build();

            openPositions.put(symbol, trade);

        } else { // SELL
            // Close existing position
            BacktestTrade openTrade = openPositions.remove(symbol);
            if (openTrade != null) {
                // Calculate P&L
                BacktestTrade closedTrade = closeTradeWithPnL(
                        openTrade,
                        fill.getFillTimestamp(),
                        fill.getFillPrice(),
                        BigDecimal.valueOf(fill.getFillQty()),
                        config.getCommission(),
                        config.getSlippage()
                );

                allTrades.add(closedTrade);
            }
        }
    }

    private BacktestTrade closeTradeWithPnL(BacktestTrade trade, LocalDateTime exitTime,
                                            BigDecimal exitPrice, BigDecimal exitQty,
                                            BigDecimal commissionRate, BigDecimal slippageRate) {
        BigDecimal entryValue = trade.getEntryPrice().multiply(trade.getEntryQty());
        BigDecimal exitValue = exitPrice.multiply(exitQty);

        // Gross P&L
        BigDecimal grossPnl = exitValue.subtract(entryValue);

        // Commission (entry + exit)
        BigDecimal entryCommission = entryValue.multiply(commissionRate);
        BigDecimal exitCommission = exitValue.multiply(commissionRate);
        BigDecimal totalCommission = entryCommission.add(exitCommission);

        // Slippage (entry + exit)
        BigDecimal entrySlippage = entryValue.multiply(slippageRate);
        BigDecimal exitSlippage = exitValue.multiply(slippageRate);
        BigDecimal totalSlippage = entrySlippage.add(exitSlippage);

        // Net P&L
        BigDecimal netPnl = grossPnl.subtract(totalCommission).subtract(totalSlippage);

        // Return percentage
        BigDecimal returnPct = entryValue.compareTo(BigDecimal.ZERO) > 0
                ? netPnl.divide(entryValue, 6, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return BacktestTrade.builder()
                .tradeId(trade.getTradeId())
                .backtestId(trade.getBacktestId())
                .symbol(trade.getSymbol())
                .side(trade.getSide())
                .entryTime(trade.getEntryTime())
                .entryPrice(trade.getEntryPrice())
                .entryQty(trade.getEntryQty())
                .exitTime(exitTime)
                .exitPrice(exitPrice)
                .exitQty(exitQty)
                .grossPnl(grossPnl)
                .commissionPaid(totalCommission)
                .slippageCost(totalSlippage)
                .netPnl(netPnl)
                .returnPct(returnPct)
                .status("CLOSED")
                .build();
    }

    private MarketBar convertToMarketBar(HistoricalBarEntity entity) {
        return MarketBar.restore(
                entity.getSymbol(),
                entity.getTimeframe(),
                entity.getBarTimestamp(),
                entity.getOpenPrice(),
                entity.getHighPrice(),
                entity.getLowPrice(),
                entity.getClosePrice(),
                entity.getVolume(),
                true // Closed
        );
    }

    private Order convertSignalToOrder(Signal signal, MarketBar bar, BacktestConfig config) {
        Side side = signal.getSignalType() == SignalType.BUY ? Side.BUY : Side.SELL;

        return Order.builder()
                .orderId(UlidGenerator.generate())
                .accountId("BACKTEST_ACCOUNT")
                .strategyId(config.getStrategyId())
                .signalId(signal.getSignalId())
                .symbol(signal.getSymbol())
                .side(side)
                .orderType(OrderType.MARKET)
                .qty(signal.getTargetValue() != null ? signal.getTargetValue() : BigDecimal.TEN)
                .price(bar.getClose())
                .status(OrderStatus.SENT)
                .idempotencyKey(UlidGenerator.generate())
                .build();
    }

    private BigDecimal calculateTotalReturn(BigDecimal initialCapital, BigDecimal finalCapital) {
        if (initialCapital.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, 6, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BacktestRunEntity createBacktestRun(BacktestConfig config) {
        BacktestRunEntity entity = BacktestRunEntity.builder()
                .backtestId(config.getBacktestId())
                .strategyId(config.getStrategyId())
                .startDate(config.getStartDate().atStartOfDay())
                .endDate(config.getEndDate().atTime(23, 59, 59))
                .symbols(String.join(",", config.getSymbols()))
                .timeframe(config.getTimeframe())
                .initialCapital(config.getInitialCapital())
                .commission(config.getCommission())
                .slippage(config.getSlippage())
                .status("RUNNING")
                .build();

        return backtestRunRepository.save(entity);
    }

    private void updateBacktestRun(BacktestRunEntity entity, BacktestResult result) {
        entity.complete(result.getFinalCapital(), result.getTotalReturn());

        if (result.getPerformanceMetrics() != null) {
            PerformanceMetrics metrics = result.getPerformanceMetrics();
            // null 값은 0으로 처리하여 NPE 방지
            int totalTrades = metrics.getTotalTrades() != null ? metrics.getTotalTrades() : 0;
            int winningTrades = metrics.getWinningTrades() != null ? metrics.getWinningTrades() : 0;
            int losingTrades = metrics.getLosingTrades() != null ? metrics.getLosingTrades() : 0;
            entity.updateTradeStats(totalTrades, winningTrades, losingTrades);
        }

        backtestRunRepository.save(entity);
    }

    private void saveTrades(String backtestId, List<BacktestTrade> trades) {
        for (BacktestTrade trade : trades) {
            BacktestTradeEntity entity = BacktestTradeEntity.builder()
                    .tradeId(trade.getTradeId())
                    .backtestId(backtestId)
                    .symbol(trade.getSymbol())
                    .entryTime(trade.getEntryTime())
                    .entryPrice(trade.getEntryPrice())
                    .entryQty(trade.getEntryQty())
                    .side(trade.getSide().name())
                    .exitTime(trade.getExitTime())
                    .exitPrice(trade.getExitPrice())
                    .exitQty(trade.getExitQty())
                    .grossPnl(trade.getGrossPnl())
                    .commissionPaid(trade.getCommissionPaid())
                    .slippageCost(trade.getSlippageCost())
                    .netPnl(trade.getNetPnl())
                    .returnPct(trade.getReturnPct())
                    .status(trade.getStatus())
                    .build();

            backtestTradeRepository.save(entity);
        }
    }

    @Override
    public void validateConfig(BacktestConfig config) {
        if (config.getBacktestId() == null) {
            throw new IllegalArgumentException("Backtest ID is required");
        }
        if (config.getStrategyId() == null) {
            throw new IllegalArgumentException("Strategy ID is required");
        }
        if (config.getStartDate() == null || config.getEndDate() == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (config.getStartDate().isAfter(config.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        if (config.getSymbols() == null || config.getSymbols().isEmpty()) {
            throw new IllegalArgumentException("At least one symbol is required");
        }
        if (config.getInitialCapital().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial capital must be positive");
        }
    }

    @Override
    public String getStatus(String backtestId) {
        return runningBacktests.getOrDefault(backtestId, "NOT_FOUND");
    }

    @Override
    public void cancel(String backtestId) {
        // Try to cancel via job executor first
        if (jobExecutor != null && jobExecutor.cancel(backtestId)) {
            log.info("Backtest job {} cancelled via executor", backtestId);
        }
        runningBacktests.put(backtestId, "CANCELLED");
        log.info("Backtest {} cancelled", backtestId);
    }

    @Override
    public String runAsync(BacktestConfig config) {
        log.info("Submitting async backtest for strategy: {}", config.getStrategyId());

        return jobExecutor.submit(config, (cfg, progressCallback) -> {
            return executeBacktestWithProgress(cfg, progressCallback);
        });
    }

    @Override
    public CompletableFuture<BacktestResult> runAsyncWithFuture(BacktestConfig config) {
        String jobId = runAsync(config);

        return CompletableFuture.supplyAsync(() -> {
            // Poll until completion
            while (true) {
                BacktestProgress progress = jobExecutor.getProgress(jobId);
                if (progress != null && progress.isDone()) {
                    if (progress.isSuccess()) {
                        return jobExecutor.getResult(jobId);
                    } else {
                        throw new RuntimeException("Backtest failed: " + progress.getErrorMessage());
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for backtest", e);
                }
            }
        });
    }

    @Override
    public BacktestProgress getProgress(String jobId) {
        return jobExecutor.getProgress(jobId);
    }

    @Override
    public BacktestResult getResult(String jobId) {
        return jobExecutor.getResult(jobId);
    }

    /**
     * Execute backtest with progress callback for async execution.
     */
    private BacktestResult executeBacktestWithProgress(BacktestConfig config,
                                                        BacktestJobExecutor.ProgressCallback progressCallback) {
        LocalDateTime startTime = LocalDateTime.now();

        // Initialize components
        progressCallback.onProgress(5, "Loading data", 0, 0);
        dataReplayEngine.loadData(config);
        int totalBars = dataReplayEngine.getTotalBars();

        progressCallback.onProgress(10, "Initializing broker", totalBars, 0);
        virtualBroker.reset(config.getInitialCapital());
        virtualBroker.setCommission(config.getCommission());
        virtualBroker.setSlippage(config.getSlippage());

        // Create strategy
        String strategyType = config.getStrategyType();
        if (strategyType == null || strategyType.isBlank()) {
            strategyType = "MA_CROSSOVER";
        }
        StrategyEngine strategy = StrategyFactory.createStrategy(strategyType);

        // Result collectors
        List<Signal> allSignals = new ArrayList<>();
        List<Order> allOrders = new ArrayList<>();
        List<Fill> allFills = new ArrayList<>();
        List<BacktestTrade> allTrades = new ArrayList<>();
        Map<String, BacktestTrade> openPositions = new HashMap<>();
        List<MarketBar> barBuffer = new ArrayList<>();

        // Replay data
        int barCount = 0;
        int lastProgressPercent = 10;

        while (dataReplayEngine.hasNext()) {
            HistoricalBarEntity barEntity = dataReplayEngine.next();
            barCount++;

            // Update progress periodically
            if (totalBars > 0 && barCount % 100 == 0) {
                int progressPercent = 10 + (int) ((barCount * 80.0) / totalBars);
                if (progressPercent != lastProgressPercent) {
                    progressCallback.onProgress(progressPercent, "Processing bars", totalBars, barCount);
                    lastProgressPercent = progressPercent;
                }
            }

            MarketBar bar = convertToMarketBar(barEntity);
            barBuffer.add(bar);

            if (barBuffer.size() >= 21) {
                StrategyContext context = StrategyContext.builder()
                        .strategyId(config.getStrategyId())
                        .symbol(barEntity.getSymbol())
                        .accountId("BACKTEST_ACCOUNT")
                        .bars(new ArrayList<>(barBuffer))
                        .params(config.getStrategyParams())
                        .timeframe(config.getTimeframe())
                        .build();

                SignalDecision decision = strategy.evaluate(context);
                if (decision != null && decision.getSignalType() != SignalType.HOLD) {
                    Signal signal = Signal.builder()
                            .signalId(UlidGenerator.generate())
                            .strategyId(config.getStrategyId())
                            .accountId("BACKTEST_ACCOUNT")
                            .symbol(barEntity.getSymbol())
                            .signalType(decision.getSignalType())
                            .targetType("QTY")
                            .targetValue(decision.getTargetValue())
                            .reason(decision.getReason())
                            .ttlSeconds(decision.getTtlSeconds())
                            .build();

                    allSignals.add(signal);
                    Order order = convertSignalToOrder(signal, bar, config);
                    allOrders.add(order);
                    virtualBroker.submitOrder(order);
                }

                if (barBuffer.size() > 100) {
                    barBuffer.remove(0);
                }
            }

            List<Fill> fills = virtualBroker.processBar(barEntity);
            allFills.addAll(fills);

            for (Fill fill : fills) {
                processFill(fill, openPositions, allTrades, config);
            }
        }

        progressCallback.onProgress(90, "Calculating metrics", totalBars, barCount);

        LocalDateTime endTime = LocalDateTime.now();
        BigDecimal finalCapital = virtualBroker.getCashBalance();
        BigDecimal totalReturn = calculateTotalReturn(config.getInitialCapital(), finalCapital);

        BacktestResult result = BacktestResult.builder()
                .backtestId(config.getBacktestId())
                .config(config)
                .startTime(startTime)
                .endTime(endTime)
                .signals(allSignals)
                .orders(allOrders)
                .fills(allFills)
                .trades(allTrades)
                .finalCapital(finalCapital)
                .totalReturn(totalReturn)
                .build();

        // Calculate metrics
        PerformanceMetrics performanceMetrics = performanceAnalyzer.analyze(result);
        RiskMetrics riskMetrics = performanceAnalyzer.analyzeRisk(result);
        EquityCurve equityCurve = performanceAnalyzer.generateEquityCurve(result);

        progressCallback.onProgress(95, "Saving results", totalBars, barCount);

        result = BacktestResult.builder()
                .backtestId(result.getBacktestId())
                .config(result.getConfig())
                .startTime(result.getStartTime())
                .endTime(result.getEndTime())
                .signals(result.getSignals())
                .orders(result.getOrders())
                .fills(result.getFills())
                .trades(result.getTrades())
                .finalCapital(result.getFinalCapital())
                .totalReturn(result.getTotalReturn())
                .performanceMetrics(performanceMetrics)
                .riskMetrics(riskMetrics)
                .equityCurve(equityCurve)
                .build();

        // Save to DB
        BacktestRunEntity runEntity = createBacktestRun(config);
        updateBacktestRun(runEntity, result);
        saveTrades(config.getBacktestId(), result.getTrades());

        progressCallback.onProgress(100, "Completed", totalBars, barCount);

        return result;
    }
}
