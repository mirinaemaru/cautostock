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
import java.util.*;

/**
 * Random Search parameter optimization.
 *
 * Tests random parameter combinations (faster than grid search for large spaces).
 */
@Component
public class RandomSearchOptimizer implements ParameterOptimizer {

    private static final Logger log = LoggerFactory.getLogger(RandomSearchOptimizer.class);

    private final BacktestEngine backtestEngine;
    private final Random random;

    public RandomSearchOptimizer(BacktestEngine backtestEngine) {
        this.backtestEngine = backtestEngine;
        this.random = new Random(System.currentTimeMillis());
    }

    @Override
    public OptimizationResult optimize(OptimizationConfig config) throws OptimizationException {
        log.info("========================================");
        log.info("Starting Random Search Optimization");
        log.info("========================================");

        LocalDateTime startTime = LocalDateTime.now();

        // Calculate total possible combinations
        long totalCombinations = calculateTotalCombinations(config.getParameterRanges());
        int searchSize = Math.min(config.getMaxRuns(), (int) totalCombinations);

        log.info("Total possible combinations: {}", totalCombinations);
        log.info("Random search size: {}", searchSize);

        // Generate random parameter combinations
        List<Map<String, Object>> randomCombinations = generateRandomCombinations(
                config.getParameterRanges(),
                searchSize
        );

        // Run backtest for each combination
        List<OptimizationResult.OptimizationRun> allRuns = new ArrayList<>();
        BigDecimal bestObjectiveValue = null;
        Map<String, Object> bestParameters = null;
        BacktestResult bestBacktestResult = null;

        for (int i = 0; i < randomCombinations.size(); i++) {
            Map<String, Object> parameters = randomCombinations.get(i);

            log.info("Running backtest {}/{} with parameters: {}", i + 1, searchSize, parameters);

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
        log.info("Random Search Optimization Complete");
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
     * Calculate total possible combinations.
     */
    private long calculateTotalCombinations(Map<String, List<Object>> parameterRanges) {
        long total = 1;

        for (List<Object> values : parameterRanges.values()) {
            total *= values.size();
        }

        return total;
    }

    /**
     * Generate random parameter combinations.
     *
     * @param parameterRanges Parameter ranges
     * @param count Number of random combinations to generate
     * @return List of random parameter combinations
     */
    private List<Map<String, Object>> generateRandomCombinations(
            Map<String, List<Object>> parameterRanges,
            int count) {

        List<Map<String, Object>> combinations = new ArrayList<>();
        Set<String> seen = new HashSet<>(); // To avoid duplicates

        List<String> paramNames = new ArrayList<>(parameterRanges.keySet());

        while (combinations.size() < count) {
            Map<String, Object> randomParams = new HashMap<>();

            // For each parameter, pick a random value
            for (String paramName : paramNames) {
                List<Object> values = parameterRanges.get(paramName);
                Object randomValue = values.get(random.nextInt(values.size()));
                randomParams.put(paramName, randomValue);
            }

            // Check for duplicates
            String key = randomParams.toString();
            if (!seen.contains(key)) {
                seen.add(key);
                combinations.add(randomParams);
            }
        }

        return combinations;
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
