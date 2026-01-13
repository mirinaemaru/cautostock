package maru.trading.application.backtest;

import maru.trading.domain.backtest.BacktestConfig;
import maru.trading.domain.backtest.BacktestEngine;
import maru.trading.domain.backtest.BacktestException;
import maru.trading.domain.backtest.BacktestResult;
import maru.trading.domain.backtest.optimization.*;
import maru.trading.infra.config.UlidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid Search parameter optimization.
 *
 * Tests all possible parameter combinations.
 */
@Component
public class GridSearchOptimizer implements ParameterOptimizer {

    private static final Logger log = LoggerFactory.getLogger(GridSearchOptimizer.class);

    private final BacktestEngine backtestEngine;

    public GridSearchOptimizer(BacktestEngine backtestEngine) {
        this.backtestEngine = backtestEngine;
    }

    @Override
    public OptimizationResult optimize(OptimizationConfig config) throws OptimizationException {
        log.info("========================================");
        log.info("Starting Grid Search Optimization");
        log.info("========================================");

        LocalDateTime startTime = LocalDateTime.now();

        // Generate all parameter combinations
        List<Map<String, Object>> parameterCombinations = generateParameterCombinations(
                config.getParameterRanges()
        );

        int totalCombinations = parameterCombinations.size();
        log.info("Total parameter combinations: {}", totalCombinations);

        if (totalCombinations > config.getMaxRuns()) {
            throw new OptimizationException(
                    String.format("Total combinations (%d) exceeds maxRuns (%d)",
                            totalCombinations, config.getMaxRuns())
            );
        }

        // Run backtest for each combination
        List<OptimizationResult.OptimizationRun> allRuns = new ArrayList<>();
        BigDecimal bestObjectiveValue = null;
        Map<String, Object> bestParameters = null;
        BacktestResult bestBacktestResult = null;

        for (int i = 0; i < parameterCombinations.size(); i++) {
            Map<String, Object> parameters = parameterCombinations.get(i);

            log.info("Running backtest {}/{} with parameters: {}", i + 1, totalCombinations, parameters);

            try {
                // Create backtest config with current parameters
                BacktestConfig backtestConfig = BacktestConfig.builder()
                        .backtestId(UlidGenerator.generate())
                        .strategyId(config.getBaseConfig().getStrategyId())
                        .symbols(config.getBaseConfig().getSymbols())
                        .startDate(config.getBaseConfig().getStartDate())
                        .endDate(config.getBaseConfig().getEndDate())
                        .timeframe(config.getBaseConfig().getTimeframe())
                        .initialCapital(config.getBaseConfig().getInitialCapital())
                        .commission(config.getBaseConfig().getCommission())
                        .slippage(config.getBaseConfig().getSlippage())
                        .strategyParams(parameters)
                        .build();

                // Run backtest
                BacktestResult result = backtestEngine.run(backtestConfig);

                // Extract objective value
                BigDecimal objectiveValue = extractObjectiveValue(result, config.getObjective());

                // Record run
                OptimizationResult.OptimizationRun run = OptimizationResult.OptimizationRun.builder()
                        .parameters(parameters)
                        .backtestResult(result)
                        .objectiveValue(objectiveValue)
                        .runNumber(i + 1)
                        .build();

                allRuns.add(run);

                // Update best if this is better
                if (bestObjectiveValue == null || objectiveValue.compareTo(bestObjectiveValue) > 0) {
                    bestObjectiveValue = objectiveValue;
                    bestParameters = parameters;
                    bestBacktestResult = result;

                    log.info("New best found! Objective value: {}, Parameters: {}",
                            objectiveValue, parameters);
                }

            } catch (BacktestException e) {
                log.error("Backtest failed for parameters {}: {}", parameters, e.getMessage());
                // Continue with next combination
            }
        }

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        log.info("========================================");
        log.info("Grid Search Optimization Complete");
        log.info("========================================");
        log.info("Best parameters: {}", bestParameters);
        log.info("Best objective value: {}", bestObjectiveValue);
        log.info("Total runs: {}", allRuns.size());
        log.info("Duration: {}ms", durationMs);

        return OptimizationResult.builder()
                .optimizationId(config.getOptimizationId())
                .config(config)
                .bestParameters(bestParameters)
                .bestObjectiveValue(bestObjectiveValue)
                .bestBacktestResult(bestBacktestResult)
                .allRuns(allRuns)
                .totalRuns(allRuns.size())
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
    }

    /**
     * Generate all parameter combinations from parameter ranges.
     *
     * @param parameterRanges Parameter ranges
     * @return List of all parameter combinations
     */
    private List<Map<String, Object>> generateParameterCombinations(
            Map<String, List<Object>> parameterRanges) {

        List<Map<String, Object>> combinations = new ArrayList<>();

        // Get parameter names
        List<String> paramNames = new ArrayList<>(parameterRanges.keySet());

        if (paramNames.isEmpty()) {
            combinations.add(new HashMap<>());
            return combinations;
        }

        // Generate combinations recursively
        generateCombinationsRecursive(
                paramNames,
                parameterRanges,
                0,
                new HashMap<>(),
                combinations
        );

        return combinations;
    }

    /**
     * Recursively generate parameter combinations.
     */
    private void generateCombinationsRecursive(
            List<String> paramNames,
            Map<String, List<Object>> parameterRanges,
            int depth,
            Map<String, Object> current,
            List<Map<String, Object>> combinations) {

        if (depth == paramNames.size()) {
            combinations.add(new HashMap<>(current));
            return;
        }

        String paramName = paramNames.get(depth);
        List<Object> values = parameterRanges.get(paramName);

        for (Object value : values) {
            current.put(paramName, value);
            generateCombinationsRecursive(paramNames, parameterRanges, depth + 1, current, combinations);
            current.remove(paramName);
        }
    }

    /**
     * Extract objective value from backtest result.
     *
     * @param result Backtest result
     * @param objective Optimization objective
     * @return Objective value
     */
    private BigDecimal extractObjectiveValue(
            BacktestResult result,
            OptimizationConfig.OptimizationObjective objective) {

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
}
