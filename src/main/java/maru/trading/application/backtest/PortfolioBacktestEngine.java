package maru.trading.application.backtest;

import maru.trading.domain.backtest.*;
import maru.trading.domain.backtest.portfolio.PortfolioBacktestConfig;
import maru.trading.domain.backtest.portfolio.PortfolioBacktestResult;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

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

        // Run individual backtests for each symbol
        Map<String, BacktestResult> symbolResults = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry : config.getSymbolWeights().entrySet()) {
            String symbol = entry.getKey();
            BigDecimal weight = entry.getValue();

            log.info("Running backtest for {} (weight: {})", symbol, weight);

            // Calculate capital allocation for this symbol
            BigDecimal symbolCapital = config.getInitialCapital()
                    .multiply(weight)
                    .setScale(0, RoundingMode.HALF_UP);

            // Create backtest config for this symbol
            BacktestConfig symbolConfig = BacktestConfig.builder()
                    .backtestId(UlidGenerator.generate())
                    .strategyId(config.getStrategyId())
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
            BacktestResult result = backtestEngine.run(symbolConfig);
            symbolResults.put(symbol, result);

            log.info("{} - Return: {}%, Trades: {}",
                    symbol,
                    result.getTotalReturn(),
                    result.getTrades().size());
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
     * Calculate correlation matrix between symbols.
     */
    private Map<String, Map<String, BigDecimal>> calculateCorrelationMatrix(
            Map<String, BacktestResult> symbolResults) {

        Map<String, Map<String, BigDecimal>> matrix = new HashMap<>();
        List<String> symbols = new ArrayList<>(symbolResults.keySet());

        for (String symbol1 : symbols) {
            Map<String, BigDecimal> row = new HashMap<>();

            for (String symbol2 : symbols) {
                if (symbol1.equals(symbol2)) {
                    row.put(symbol2, BigDecimal.ONE);
                } else {
                    // Simplified correlation (would need full implementation)
                    // For now, use placeholder
                    row.put(symbol2, BigDecimal.ZERO);
                }
            }

            matrix.put(symbol1, row);
        }

        return matrix;
    }
}
