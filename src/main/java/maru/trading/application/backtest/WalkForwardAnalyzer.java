package maru.trading.application.backtest;

import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.BacktestEngine;
import maru.trading.domain.backtest.BacktestException;
import maru.trading.domain.backtest.BacktestResult;
import maru.trading.domain.backtest.optimization.*;
import maru.trading.domain.backtest.walkforward.WalkForwardConfig;
import maru.trading.domain.backtest.walkforward.WalkForwardResult;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Walk-Forward Analysis implementation.
 *
 * Divides data into rolling in-sample (training) and out-of-sample (testing) windows.
 * Optimizes parameters on in-sample, validates on out-of-sample.
 */
@Component
public class WalkForwardAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(WalkForwardAnalyzer.class);

    private final BacktestEngine backtestEngine;
    private final ParameterOptimizer optimizer;

    public WalkForwardAnalyzer(BacktestEngine backtestEngine, GridSearchOptimizer optimizer) {
        this.backtestEngine = backtestEngine;
        this.optimizer = optimizer;
    }

    /**
     * Run walk-forward analysis.
     *
     * @param config Walk-forward configuration
     * @return Walk-forward result
     * @throws Exception if analysis fails
     */
    public WalkForwardResult analyze(WalkForwardConfig config) throws Exception {
        log.info("========================================");
        log.info("Starting Walk-Forward Analysis");
        log.info("========================================");

        LocalDateTime startTime = LocalDateTime.now();

        // Generate windows
        List<WalkForwardWindow> windows = generateWindows(config);

        log.info("Total windows: {}", windows.size());

        if (windows.size() < config.getMinWindows()) {
            throw new IllegalArgumentException(
                    String.format("Insufficient windows (%d). Minimum required: %d",
                            windows.size(), config.getMinWindows())
            );
        }

        // Analyze each window
        List<WalkForwardResult.WalkForwardWindow> results = new ArrayList<>();
        List<BigDecimal> outOfSampleReturns = new ArrayList<>();
        List<BigDecimal> outOfSampleSharpes = new ArrayList<>();

        for (int i = 0; i < windows.size(); i++) {
            WalkForwardWindow window = windows.get(i);

            log.info("========================================");
            log.info("Window {}/{}", i + 1, windows.size());
            log.info("In-Sample: {} to {}", window.inSampleStart, window.inSampleEnd);
            log.info("Out-of-Sample: {} to {}", window.outOfSampleStart, window.outOfSampleEnd);
            log.info("========================================");

            // 1. Optimize on in-sample
            Map<String, Object> optimizedParams = optimizeInSample(config, window);

            log.info("Optimized parameters: {}", optimizedParams);

            // 2. Backtest in-sample with optimized parameters
            BacktestResult inSampleResult = runBacktest(
                    config.getBaseConfig(),
                    window.inSampleStart,
                    window.inSampleEnd,
                    optimizedParams
            );

            // 3. Validate on out-of-sample
            BacktestResult outOfSampleResult = runBacktest(
                    config.getBaseConfig(),
                    window.outOfSampleStart,
                    window.outOfSampleEnd,
                    optimizedParams
            );

            // 4. Calculate metrics
            BigDecimal inSampleMetric = extractMetric(inSampleResult, config.getOptimizationConfig().getObjective());
            BigDecimal outOfSampleMetric = extractMetric(outOfSampleResult, config.getOptimizationConfig().getObjective());
            BigDecimal degradation = inSampleMetric.subtract(outOfSampleMetric);

            outOfSampleReturns.add(outOfSampleResult.getTotalReturn());
            if (outOfSampleResult.getPerformanceMetrics() != null) {
                outOfSampleSharpes.add(outOfSampleResult.getPerformanceMetrics().getSharpeRatio());
            }

            log.info("In-Sample {}: {}", config.getOptimizationConfig().getObjective(), inSampleMetric);
            log.info("Out-of-Sample {}: {}", config.getOptimizationConfig().getObjective(), outOfSampleMetric);
            log.info("Degradation: {}", degradation);

            // 5. Record result
            WalkForwardResult.WalkForwardWindow resultWindow = WalkForwardResult.WalkForwardWindow.builder()
                    .windowNumber(i + 1)
                    .inSampleStart(window.inSampleStart)
                    .inSampleEnd(window.inSampleEnd)
                    .outOfSampleStart(window.outOfSampleStart)
                    .outOfSampleEnd(window.outOfSampleEnd)
                    .optimizedParameters(optimizedParams)
                    .inSampleResult(inSampleResult)
                    .outOfSampleResult(outOfSampleResult)
                    .inSampleMetric(inSampleMetric)
                    .outOfSampleMetric(outOfSampleMetric)
                    .performanceDegradation(degradation)
                    .build();

            results.add(resultWindow);
        }

        // Calculate combined metrics
        BigDecimal combinedReturn = calculateCombinedReturn(outOfSampleReturns);
        BigDecimal avgSharpe = calculateAverage(outOfSampleSharpes);
        BigDecimal stabilityScore = calculateStabilityScore(outOfSampleReturns);

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        log.info("========================================");
        log.info("Walk-Forward Analysis Complete");
        log.info("========================================");
        log.info("Combined Out-of-Sample Return: {}%", combinedReturn);
        log.info("Average Out-of-Sample Sharpe: {}", avgSharpe);
        log.info("Stability Score: {}", stabilityScore);
        log.info("Duration: {}ms", durationMs);

        return WalkForwardResult.builder()
                .walkForwardId(config.getWalkForwardId())
                .config(config)
                .windows(results)
                .combinedOutOfSampleReturn(combinedReturn)
                .avgOutOfSampleSharpeRatio(avgSharpe)
                .stabilityScore(stabilityScore)
                .totalWindows(results.size())
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
    }

    /**
     * Generate walk-forward windows based on mode.
     */
    private List<WalkForwardWindow> generateWindows(WalkForwardConfig config) {
        if (config.getMode() == WalkForwardConfig.WalkForwardMode.ANCHORED) {
            return generateAnchoredWindows(config);
        } else {
            return generateRollingWindows(config);
        }
    }

    /**
     * Generate rolling walk-forward windows.
     *
     * In rolling mode, both in-sample start and end move forward.
     * The in-sample window size stays constant.
     */
    private List<WalkForwardWindow> generateRollingWindows(WalkForwardConfig config) {
        List<WalkForwardWindow> windows = new ArrayList<>();

        LocalDate currentStart = config.getAnalysisStartDate();
        LocalDate analysisEnd = config.getAnalysisEndDate();

        while (true) {
            // In-sample period
            LocalDate inSampleStart = currentStart;
            LocalDate inSampleEnd = currentStart.plusDays(config.getInSampleDays() - 1);

            // Out-of-sample period
            LocalDate outOfSampleStart = inSampleEnd.plusDays(1);
            LocalDate outOfSampleEnd = outOfSampleStart.plusDays(config.getOutOfSampleDays() - 1);

            // Check if window fits within analysis period
            if (outOfSampleEnd.isAfter(analysisEnd)) {
                break;
            }

            windows.add(new WalkForwardWindow(inSampleStart, inSampleEnd, outOfSampleStart, outOfSampleEnd));

            // Move to next window
            currentStart = currentStart.plusDays(config.getStepDays());
        }

        return windows;
    }

    /**
     * Generate anchored walk-forward windows.
     *
     * In anchored mode, in-sample always starts from analysisStartDate.
     * The in-sample window grows over time.
     */
    private List<WalkForwardWindow> generateAnchoredWindows(WalkForwardConfig config) {
        List<WalkForwardWindow> windows = new ArrayList<>();

        LocalDate analysisStart = config.getAnalysisStartDate();
        LocalDate analysisEnd = config.getAnalysisEndDate();

        // Start with minimum in-sample period
        LocalDate inSampleEnd = analysisStart.plusDays(config.getInSampleDays() - 1);

        while (true) {
            // In-sample period (always starts from beginning)
            LocalDate inSampleStart = analysisStart;

            // Out-of-sample period
            LocalDate outOfSampleStart = inSampleEnd.plusDays(1);
            LocalDate outOfSampleEnd = outOfSampleStart.plusDays(config.getOutOfSampleDays() - 1);

            // Check if window fits within analysis period
            if (outOfSampleEnd.isAfter(analysisEnd)) {
                break;
            }

            windows.add(new WalkForwardWindow(inSampleStart, inSampleEnd, outOfSampleStart, outOfSampleEnd));

            // Move in-sample end forward (in-sample grows)
            inSampleEnd = inSampleEnd.plusDays(config.getStepDays());
        }

        return windows;
    }

    /**
     * Optimize parameters on in-sample data.
     */
    private Map<String, Object> optimizeInSample(WalkForwardConfig config, WalkForwardWindow window)
            throws OptimizationException {

        // Create optimization config for in-sample period
        BacktestConfig inSampleBaseConfig = BacktestConfig.builder()
                .backtestId(UlidGenerator.generate())
                .strategyId(config.getBaseConfig().getStrategyId())
                .symbols(config.getBaseConfig().getSymbols())
                .startDate(window.inSampleStart)
                .endDate(window.inSampleEnd)
                .timeframe(config.getBaseConfig().getTimeframe())
                .initialCapital(config.getBaseConfig().getInitialCapital())
                .commission(config.getBaseConfig().getCommission())
                .slippage(config.getBaseConfig().getSlippage())
                .build();

        OptimizationConfig optConfig = OptimizationConfig.builder()
                .optimizationId(UlidGenerator.generate())
                .baseConfig(inSampleBaseConfig)
                .parameterRanges(config.getOptimizationConfig().getParameterRanges())
                .method(config.getOptimizationConfig().getMethod())
                .objective(config.getOptimizationConfig().getObjective())
                .maxRuns(config.getOptimizationConfig().getMaxRuns())
                .build();

        // Run optimization
        OptimizationResult optResult = optimizer.optimize(optConfig);

        return optResult.getBestParameters();
    }

    /**
     * Run backtest with given parameters.
     */
    private BacktestResult runBacktest(
            BacktestConfig baseConfig,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, Object> parameters) throws BacktestException {

        BacktestConfig config = BacktestConfig.builder()
                .backtestId(UlidGenerator.generate())
                .strategyId(baseConfig.getStrategyId())
                .symbols(baseConfig.getSymbols())
                .startDate(startDate)
                .endDate(endDate)
                .timeframe(baseConfig.getTimeframe())
                .initialCapital(baseConfig.getInitialCapital())
                .commission(baseConfig.getCommission())
                .slippage(baseConfig.getSlippage())
                .strategyParams(parameters)
                .build();

        return backtestEngine.run(config);
    }

    /**
     * Extract metric from backtest result.
     */
    private BigDecimal extractMetric(BacktestResult result, OptimizationConfig.OptimizationObjective objective) {
        switch (objective) {
            case TOTAL_RETURN:
                return result.getTotalReturn();
            case SHARPE_RATIO:
                return result.getPerformanceMetrics() != null
                        ? result.getPerformanceMetrics().getSharpeRatio()
                        : BigDecimal.ZERO;
            case SORTINO_RATIO:
                return result.getPerformanceMetrics() != null
                        ? result.getPerformanceMetrics().getSortinoRatio()
                        : BigDecimal.ZERO;
            case PROFIT_FACTOR:
                return result.getPerformanceMetrics() != null
                        ? result.getPerformanceMetrics().getProfitFactor()
                        : BigDecimal.ZERO;
            case CALMAR_RATIO:
                return result.getRiskMetrics() != null
                        ? result.getRiskMetrics().getCalmarRatio()
                        : BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate combined return from multiple periods.
     */
    private BigDecimal calculateCombinedReturn(List<BigDecimal> returns) {
        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Compound returns: (1 + r1) * (1 + r2) * ... - 1
        BigDecimal compound = BigDecimal.ONE;
        for (BigDecimal ret : returns) {
            BigDecimal factor = BigDecimal.ONE.add(ret.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP));
            compound = compound.multiply(factor);
        }

        return compound.subtract(BigDecimal.ONE).multiply(BigDecimal.valueOf(100));
    }

    /**
     * Calculate average of values.
     */
    private BigDecimal calculateAverage(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    /**
     * Calculate stability score (0-1).
     *
     * Measures consistency of returns across windows.
     * Lower variance = higher stability.
     */
    private BigDecimal calculateStabilityScore(List<BigDecimal> returns) {
        if (returns.size() < 2) {
            return BigDecimal.ONE;
        }

        // Calculate standard deviation
        BigDecimal mean = calculateAverage(returns);
        BigDecimal variance = returns.stream()
                .map(r -> r.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, RoundingMode.HALF_UP);

        double stdDev = Math.sqrt(variance.doubleValue());

        // Stability = 1 / (1 + stdDev/100)
        // Higher stdDev = lower stability
        double stability = 1.0 / (1.0 + stdDev / 100.0);

        return BigDecimal.valueOf(stability).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Helper class for window definition.
     */
    private static class WalkForwardWindow {
        final LocalDate inSampleStart;
        final LocalDate inSampleEnd;
        final LocalDate outOfSampleStart;
        final LocalDate outOfSampleEnd;

        WalkForwardWindow(LocalDate inSampleStart, LocalDate inSampleEnd,
                         LocalDate outOfSampleStart, LocalDate outOfSampleEnd) {
            this.inSampleStart = inSampleStart;
            this.inSampleEnd = inSampleEnd;
            this.outOfSampleStart = outOfSampleStart;
            this.outOfSampleEnd = outOfSampleEnd;
        }
    }
}
