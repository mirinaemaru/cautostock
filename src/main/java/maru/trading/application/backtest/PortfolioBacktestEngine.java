package maru.trading.application.backtest;

import maru.trading.domain.backtest.*;
import maru.trading.domain.backtest.portfolio.PortfolioBacktestConfig;
import maru.trading.domain.backtest.portfolio.PortfolioBacktestResult;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Portfolio backtest engine.
 *
 * Backtests multiple symbols simultaneously with portfolio-level risk management.
 */
@Component
public class PortfolioBacktestEngine {

    private static final Logger log = LoggerFactory.getLogger(PortfolioBacktestEngine.class);

    private final BacktestEngine backtestEngine;
    private final PerformanceAnalyzer performanceAnalyzer;

    public PortfolioBacktestEngine(BacktestEngine backtestEngine, PerformanceAnalyzer performanceAnalyzer) {
        this.backtestEngine = backtestEngine;
        this.performanceAnalyzer = performanceAnalyzer;
    }

    /**
     * Run portfolio backtest.
     *
     * @param config Portfolio backtest configuration
     * @return Portfolio backtest result
     * @throws BacktestException if backtest fails
     */
    public PortfolioBacktestResult run(PortfolioBacktestConfig config) throws BacktestException {
        log.info("========================================");
        log.info("Starting Portfolio Backtest");
        log.info("========================================");
        log.info("Portfolio: {}", config.getPortfolioName());
        log.info("Symbols: {}", config.getSymbolWeights().keySet());

        LocalDateTime startTime = LocalDateTime.now();

        // Validate weights sum to 1.0
        BigDecimal totalWeight = config.getSymbolWeights().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalWeight.compareTo(BigDecimal.ONE) != 0) {
            throw new BacktestException(
                    String.format("Symbol weights must sum to 1.0, got: %s", totalWeight)
            );
        }

        // Run individual backtests for each symbol in parallel
        Map<String, BacktestResult> symbolResults = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(config.getSymbolWeights().size(), Runtime.getRuntime().availableProcessors())
        );

        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Map.Entry<String, BigDecimal> entry : config.getSymbolWeights().entrySet()) {
                String symbol = entry.getKey();
                BigDecimal weight = entry.getValue();

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    log.info("Running backtest for {} (weight: {})", symbol, weight);

                    // Calculate capital allocation for this symbol
                    BigDecimal symbolCapital = config.getInitialCapital()
                            .multiply(weight)
                            .setScale(0, RoundingMode.HALF_UP);

                    // Create backtest config for this symbol
                    BacktestConfig symbolConfig = BacktestConfig.builder()
                            .backtestId(UlidGenerator.generate())
                            .strategyId(config.getStrategyId())
                            .strategyType(config.getStrategyType())
                            .symbols(List.of(symbol))
                            .startDate(config.getStartDate())
                            .endDate(config.getEndDate())
                            .timeframe(config.getTimeframe())
                            .initialCapital(symbolCapital)
                            .commission(config.getCommission())
                            .slippage(config.getSlippage())
                            .strategyParams(config.getStrategyParams())
                            .build();

                    // Run backtest
                    try {
                        BacktestResult result = backtestEngine.run(symbolConfig);
                        symbolResults.put(symbol, result);

                        log.info("{} - Return: {}%, Trades: {}",
                                symbol,
                                result.getTotalReturn(),
                                result.getTrades().size());
                    } catch (BacktestException e) {
                        log.error("Backtest failed for symbol {}: {}", symbol, e.getMessage());
                        throw new RuntimeException(e);
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all backtests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.shutdown();
        }

        // Calculate portfolio-level metrics
        BigDecimal finalCapital = symbolResults.values().stream()
                .map(BacktestResult::getFinalCapital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReturn = finalCapital.subtract(config.getInitialCapital())
                .divide(config.getInitialCapital(), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Generate portfolio equity curve
        List<PortfolioBacktestResult.PortfolioEquityPoint> equityCurve =
                generatePortfolioEquityCurve(symbolResults, config);

        // Calculate portfolio performance metrics
        PerformanceMetrics portfolioMetrics = calculatePortfolioMetrics(
                symbolResults,
                config.getInitialCapital(),
                finalCapital,
                startTime
        );

        // Calculate correlation matrix
        Map<String, Map<String, BigDecimal>> correlationMatrix =
                calculateCorrelationMatrix(symbolResults);

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        log.info("========================================");
        log.info("Portfolio Backtest Complete");
        log.info("========================================");
        log.info("Total Return: {}%", totalReturn);
        log.info("Final Capital: {}", finalCapital);
        log.info("Portfolio Sharpe: {}", portfolioMetrics.getSharpeRatio());
        log.info("Duration: {}ms", durationMs);

        return PortfolioBacktestResult.builder()
                .portfolioBacktestId(config.getPortfolioBacktestId())
                .config(config)
                .symbolResults(symbolResults)
                .portfolioMetrics(portfolioMetrics)
                .finalCapital(finalCapital)
                .totalReturn(totalReturn)
                .equityCurve(equityCurve)
                .correlationMatrix(correlationMatrix)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
    }

    /**
     * Generate portfolio equity curve by combining individual symbol curves.
     */
    private List<PortfolioBacktestResult.PortfolioEquityPoint> generatePortfolioEquityCurve(
            Map<String, BacktestResult> symbolResults,
            PortfolioBacktestConfig config) {

        // Get all unique timestamps from all equity curves
        Set<LocalDateTime> allTimestamps = new TreeSet<>();

        Map<String, EquityCurve> symbolCurves = new HashMap<>();
        for (Map.Entry<String, BacktestResult> entry : symbolResults.entrySet()) {
            EquityCurve curve = performanceAnalyzer.generateEquityCurve(entry.getValue());
            symbolCurves.put(entry.getKey(), curve);

            curve.getPoints().forEach(point -> allTimestamps.add(point.getTimestamp()));
        }

        // Build combined equity curve
        List<PortfolioBacktestResult.PortfolioEquityPoint> portfolioPoints = new ArrayList<>();

        for (LocalDateTime timestamp : allTimestamps) {
            Map<String, BigDecimal> symbolEquities = new HashMap<>();
            BigDecimal totalEquity = BigDecimal.ZERO;

            for (Map.Entry<String, EquityCurve> entry : symbolCurves.entrySet()) {
                String symbol = entry.getKey();
                EquityCurve curve = entry.getValue();

                // Find equity at this timestamp (or closest previous)
                BigDecimal equity = getEquityAtTimestamp(curve, timestamp);
                symbolEquities.put(symbol, equity);
                totalEquity = totalEquity.add(equity);
            }

            PortfolioBacktestResult.PortfolioEquityPoint point =
                    PortfolioBacktestResult.PortfolioEquityPoint.builder()
                            .timestamp(timestamp)
                            .totalEquity(totalEquity)
                            .symbolEquities(symbolEquities)
                            .build();

            portfolioPoints.add(point);
        }

        return portfolioPoints;
    }

    /**
     * Get equity value at specific timestamp (or closest previous).
     */
    private BigDecimal getEquityAtTimestamp(EquityCurve curve, LocalDateTime timestamp) {
        List<EquityCurve.EquityPoint> points = curve.getPoints();

        if (points.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal latestEquity = points.get(0).getEquity();

        for (EquityCurve.EquityPoint point : points) {
            if (point.getTimestamp().isAfter(timestamp)) {
                break;
            }
            latestEquity = point.getEquity();
        }

        return latestEquity;
    }

    /**
     * Calculate portfolio-level performance metrics.
     */
    private PerformanceMetrics calculatePortfolioMetrics(
            Map<String, BacktestResult> symbolResults,
            BigDecimal initialCapital,
            BigDecimal finalCapital,
            LocalDateTime startTime) {

        // Combine all trades from all symbols
        List<BacktestTrade> allTrades = new ArrayList<>();
        symbolResults.values().forEach(result -> allTrades.addAll(result.getTrades()));

        // Create synthetic portfolio result for analysis
        BacktestConfig dummyConfig = BacktestConfig.builder()
                .backtestId("PORTFOLIO")
                .strategyId("PORTFOLIO")
                .initialCapital(initialCapital)
                .build();

        BacktestResult portfolioResult = BacktestResult.builder()
                .backtestId("PORTFOLIO")
                .config(dummyConfig)
                .startTime(startTime)
                .endTime(LocalDateTime.now())
                .finalCapital(finalCapital)
                .totalReturn(finalCapital.subtract(initialCapital)
                        .divide(initialCapital, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .trades(allTrades)
                .build();

        return performanceAnalyzer.analyze(portfolioResult);
    }

    /**
     * Calculate correlation matrix between symbols using Pearson correlation.
     *
     * The correlation is calculated based on daily returns of each symbol.
     */
    private Map<String, Map<String, BigDecimal>> calculateCorrelationMatrix(
            Map<String, BacktestResult> symbolResults) {

        Map<String, Map<String, BigDecimal>> matrix = new HashMap<>();
        List<String> symbols = new ArrayList<>(symbolResults.keySet());

        // Extract daily returns for each symbol
        Map<String, List<BigDecimal>> symbolReturns = new HashMap<>();
        for (Map.Entry<String, BacktestResult> entry : symbolResults.entrySet()) {
            symbolReturns.put(entry.getKey(), extractDailyReturns(entry.getValue()));
        }

        // Calculate pairwise correlations
        for (String symbol1 : symbols) {
            Map<String, BigDecimal> row = new HashMap<>();
            List<BigDecimal> returns1 = symbolReturns.get(symbol1);

            for (String symbol2 : symbols) {
                if (symbol1.equals(symbol2)) {
                    row.put(symbol2, BigDecimal.ONE);
                } else {
                    List<BigDecimal> returns2 = symbolReturns.get(symbol2);
                    BigDecimal correlation = calculatePearsonCorrelation(returns1, returns2);
                    row.put(symbol2, correlation);
                }
            }

            matrix.put(symbol1, row);
        }

        return matrix;
    }

    /**
     * Extract daily returns from backtest result.
     */
    private List<BigDecimal> extractDailyReturns(BacktestResult result) {
        List<BigDecimal> returns = new ArrayList<>();

        EquityCurve curve = performanceAnalyzer.generateEquityCurve(result);
        List<EquityCurve.EquityPoint> points = curve.getPoints();

        if (points.size() < 2) {
            return returns;
        }

        // Group equity points by date and calculate daily returns
        Map<java.time.LocalDate, BigDecimal> dailyEquity = new LinkedHashMap<>();
        for (EquityCurve.EquityPoint point : points) {
            java.time.LocalDate date = point.getTimestamp().toLocalDate();
            // Keep the last equity value for each day
            dailyEquity.put(date, point.getEquity());
        }

        // Calculate returns between consecutive days
        List<BigDecimal> equities = new ArrayList<>(dailyEquity.values());
        for (int i = 1; i < equities.size(); i++) {
            BigDecimal prevEquity = equities.get(i - 1);
            BigDecimal currEquity = equities.get(i);

            if (prevEquity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyReturn = currEquity.subtract(prevEquity)
                        .divide(prevEquity, 10, RoundingMode.HALF_UP);
                returns.add(dailyReturn);
            }
        }

        return returns;
    }

    /**
     * Calculate Pearson correlation coefficient between two return series.
     *
     * Formula: r = Σ((xi - x̄)(yi - ȳ)) / √(Σ(xi - x̄)² × Σ(yi - ȳ)²)
     */
    private BigDecimal calculatePearsonCorrelation(List<BigDecimal> returns1, List<BigDecimal> returns2) {
        // Align series by taking minimum length
        int n = Math.min(returns1.size(), returns2.size());

        if (n < 2) {
            return BigDecimal.ZERO;
        }

        MathContext mc = new MathContext(15);

        // Calculate means
        BigDecimal sum1 = BigDecimal.ZERO;
        BigDecimal sum2 = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            sum1 = sum1.add(returns1.get(i));
            sum2 = sum2.add(returns2.get(i));
        }

        BigDecimal mean1 = sum1.divide(BigDecimal.valueOf(n), mc);
        BigDecimal mean2 = sum2.divide(BigDecimal.valueOf(n), mc);

        // Calculate covariance and standard deviations
        BigDecimal covariance = BigDecimal.ZERO;
        BigDecimal variance1 = BigDecimal.ZERO;
        BigDecimal variance2 = BigDecimal.ZERO;

        for (int i = 0; i < n; i++) {
            BigDecimal diff1 = returns1.get(i).subtract(mean1);
            BigDecimal diff2 = returns2.get(i).subtract(mean2);

            covariance = covariance.add(diff1.multiply(diff2));
            variance1 = variance1.add(diff1.multiply(diff1));
            variance2 = variance2.add(diff2.multiply(diff2));
        }

        // Check for zero variance (no correlation can be computed)
        if (variance1.compareTo(BigDecimal.ZERO) == 0 || variance2.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate correlation: covariance / (stdDev1 * stdDev2)
        double stdDev1 = Math.sqrt(variance1.doubleValue());
        double stdDev2 = Math.sqrt(variance2.doubleValue());
        double denominator = stdDev1 * stdDev2;

        if (denominator == 0) {
            return BigDecimal.ZERO;
        }

        double correlation = covariance.doubleValue() / denominator;

        // Clamp to [-1, 1] range to handle floating point errors
        correlation = Math.max(-1.0, Math.min(1.0, correlation));

        return BigDecimal.valueOf(correlation).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Perform portfolio rebalancing simulation.
     *
     * This method simulates periodic rebalancing of the portfolio
     * to maintain target weights.
     *
     * @param config Portfolio configuration
     * @param symbolResults Initial backtest results
     * @return Rebalanced equity curve
     */
    public List<PortfolioBacktestResult.PortfolioEquityPoint> simulateRebalancing(
            PortfolioBacktestConfig config,
            Map<String, BacktestResult> symbolResults) {

        if (config.getRebalancingFrequencyDays() <= 0) {
            // No rebalancing - return original curve
            return generatePortfolioEquityCurve(symbolResults, config);
        }

        log.info("Simulating rebalancing every {} days", config.getRebalancingFrequencyDays());

        List<PortfolioBacktestResult.PortfolioEquityPoint> equityCurve = new ArrayList<>();

        // Get combined equity curve with daily points
        List<PortfolioBacktestResult.PortfolioEquityPoint> originalCurve =
                generatePortfolioEquityCurve(symbolResults, config);

        if (originalCurve.isEmpty()) {
            return equityCurve;
        }

        // Track current holdings
        Map<String, BigDecimal> currentHoldings = new HashMap<>();
        BigDecimal totalEquity = config.getInitialCapital();

        // Initialize holdings based on weights
        for (Map.Entry<String, BigDecimal> entry : config.getSymbolWeights().entrySet()) {
            currentHoldings.put(entry.getKey(), totalEquity.multiply(entry.getValue()));
        }

        LocalDateTime lastRebalanceDate = originalCurve.get(0).getTimestamp();
        int daysSinceRebalance = 0;

        for (PortfolioBacktestResult.PortfolioEquityPoint point : originalCurve) {
            // Calculate days since last rebalance
            long daysBetween = java.time.Duration.between(lastRebalanceDate, point.getTimestamp()).toDays();

            if (daysBetween >= config.getRebalancingFrequencyDays()) {
                // Time to rebalance
                totalEquity = point.getTotalEquity();

                // Rebalance to target weights
                Map<String, BigDecimal> newHoldings = new HashMap<>();
                for (Map.Entry<String, BigDecimal> entry : config.getSymbolWeights().entrySet()) {
                    newHoldings.put(entry.getKey(), totalEquity.multiply(entry.getValue()));
                }
                currentHoldings = newHoldings;
                lastRebalanceDate = point.getTimestamp();

                log.debug("Rebalanced portfolio at {} - Total Equity: {}",
                        point.getTimestamp(), totalEquity);
            }

            equityCurve.add(PortfolioBacktestResult.PortfolioEquityPoint.builder()
                    .timestamp(point.getTimestamp())
                    .totalEquity(point.getTotalEquity())
                    .symbolEquities(new HashMap<>(currentHoldings))
                    .build());
        }

        return equityCurve;
    }
}
