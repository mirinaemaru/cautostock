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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Bayesian Optimization implementation.
 *
 * Uses a surrogate model (simplified Gaussian Process approximation)
 * to model the objective function and Expected Improvement (EI)
 * as the acquisition function to select next evaluation points.
 *
 * This approach is more sample-efficient than Grid or Random search
 * for expensive objective function evaluations like backtests.
 */
@Component
public class BayesianOptimizer implements ParameterOptimizer {

    private static final Logger log = LoggerFactory.getLogger(BayesianOptimizer.class);

    private final BacktestEngine backtestEngine;

    // Bayesian optimization parameters
    private static final int INITIAL_RANDOM_SAMPLES = 5;
    private static final double EXPLORATION_FACTOR = 0.1; // Balance exploration vs exploitation
    private static final double LENGTH_SCALE = 1.0; // RBF kernel length scale

    public BayesianOptimizer(BacktestEngine backtestEngine) {
        this.backtestEngine = backtestEngine;
    }

    @Override
    public OptimizationResult optimize(OptimizationConfig config) throws OptimizationException {
        log.info("========================================");
        log.info("Starting Bayesian Optimization");
        log.info("========================================");
        log.info("Parameters: {}", config.getParameterRanges().keySet());
        log.info("Objective: {}", config.getObjective());
        log.info("Max runs: {}", config.getMaxRuns());

        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Convert parameter ranges to numeric arrays for optimization
            List<String> paramNames = new ArrayList<>(config.getParameterRanges().keySet());
            List<List<Object>> paramValues = new ArrayList<>();
            for (String name : paramNames) {
                paramValues.add(config.getParameterRanges().get(name));
            }

            // Store evaluated points and their objective values
            List<double[]> evaluatedPoints = new ArrayList<>();
            List<Double> objectiveValues = new ArrayList<>();

            Map<String, Object> bestParams = null;
            BigDecimal bestObjective = null;
            BacktestResult bestResult = null;
            int runsExecuted = 0;

            // Phase 1: Initial random sampling
            log.info("Phase 1: Initial random sampling ({} points)", INITIAL_RANDOM_SAMPLES);
            int initialSamples = Math.min(INITIAL_RANDOM_SAMPLES, config.getMaxRuns());

            for (int i = 0; i < initialSamples; i++) {
                Map<String, Object> params = sampleRandomPoint(paramNames, paramValues);
                double[] normalizedPoint = normalizePoint(params, paramNames, paramValues);

                BacktestResult result = runBacktest(config.getBaseConfig(), params);
                BigDecimal objective = extractObjective(result, config.getObjective());
                double objectiveDouble = objective.doubleValue();

                evaluatedPoints.add(normalizedPoint);
                objectiveValues.add(objectiveDouble);
                runsExecuted++;

                log.info("Initial sample {}/{}: objective = {}", i + 1, initialSamples, objective);

                if (bestObjective == null || objective.compareTo(bestObjective) > 0) {
                    bestObjective = objective;
                    bestParams = params;
                    bestResult = result;
                }
            }

            // Phase 2: Bayesian optimization loop
            log.info("Phase 2: Bayesian optimization");
            int remainingRuns = config.getMaxRuns() - runsExecuted;

            for (int i = 0; i < remainingRuns; i++) {
                // Find next point using acquisition function
                double[] nextPoint = findNextPoint(evaluatedPoints, objectiveValues, paramValues.size());

                // Convert normalized point back to parameter values
                Map<String, Object> params = denormalizePoint(nextPoint, paramNames, paramValues);

                // Evaluate the point
                BacktestResult result = runBacktest(config.getBaseConfig(), params);
                BigDecimal objective = extractObjective(result, config.getObjective());
                double objectiveDouble = objective.doubleValue();

                evaluatedPoints.add(nextPoint);
                objectiveValues.add(objectiveDouble);
                runsExecuted++;

                if ((i + 1) % 10 == 0 || i == remainingRuns - 1) {
                    log.info("Bayesian iteration {}/{}: objective = {}, best = {}",
                            i + 1, remainingRuns, objective, bestObjective);
                }

                if (objective.compareTo(bestObjective) > 0) {
                    bestObjective = objective;
                    bestParams = params;
                    bestResult = result;
                    log.info("New best found: {} with params {}", bestObjective, bestParams);
                }
            }

            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("========================================");
            log.info("Bayesian Optimization Complete");
            log.info("========================================");
            log.info("Best objective: {}", bestObjective);
            log.info("Best parameters: {}", bestParams);
            log.info("Total runs: {}", runsExecuted);
            log.info("Duration: {}ms", durationMs);

            return OptimizationResult.builder()
                    .optimizationId(config.getOptimizationId())
                    .config(config)
                    .bestParameters(bestParams)
                    .bestObjectiveValue(bestObjective)
                    .bestBacktestResult(bestResult)
                    .totalRuns(runsExecuted)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(durationMs)
                    .build();

        } catch (BacktestException e) {
            throw new OptimizationException("Backtest failed during optimization", e);
        }
    }

    /**
     * Sample a random parameter combination.
     */
    private Map<String, Object> sampleRandomPoint(List<String> paramNames, List<List<Object>> paramValues) {
        Random random = new Random();
        Map<String, Object> params = new HashMap<>();

        for (int i = 0; i < paramNames.size(); i++) {
            List<Object> values = paramValues.get(i);
            Object value = values.get(random.nextInt(values.size()));
            params.put(paramNames.get(i), value);
        }

        return params;
    }

    /**
     * Normalize parameter values to [0, 1] range.
     */
    private double[] normalizePoint(Map<String, Object> params, List<String> paramNames, List<List<Object>> paramValues) {
        double[] point = new double[paramNames.size()];

        for (int i = 0; i < paramNames.size(); i++) {
            Object value = params.get(paramNames.get(i));
            List<Object> values = paramValues.get(i);
            int index = values.indexOf(value);
            point[i] = (double) index / Math.max(1, values.size() - 1);
        }

        return point;
    }

    /**
     * Convert normalized point back to parameter values.
     */
    private Map<String, Object> denormalizePoint(double[] point, List<String> paramNames, List<List<Object>> paramValues) {
        Map<String, Object> params = new HashMap<>();

        for (int i = 0; i < paramNames.size(); i++) {
            List<Object> values = paramValues.get(i);
            int index = (int) Math.round(point[i] * (values.size() - 1));
            index = Math.max(0, Math.min(index, values.size() - 1));
            params.put(paramNames.get(i), values.get(index));
        }

        return params;
    }

    /**
     * Find the next point to evaluate using acquisition function (Expected Improvement).
     *
     * Uses a simplified approach:
     * 1. Generate candidate points
     * 2. For each candidate, compute expected improvement
     * 3. Return point with highest EI
     */
    private double[] findNextPoint(List<double[]> evaluatedPoints, List<Double> objectiveValues, int dimensions) {
        int numCandidates = 100;
        Random random = new Random();

        double currentBest = objectiveValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        double[] bestCandidate = null;
        double bestEI = Double.NEGATIVE_INFINITY;

        for (int c = 0; c < numCandidates; c++) {
            // Generate random candidate
            double[] candidate = new double[dimensions];
            for (int d = 0; d < dimensions; d++) {
                candidate[d] = random.nextDouble();
            }

            // Compute surrogate model prediction
            double[] prediction = predictWithSurrogate(candidate, evaluatedPoints, objectiveValues);
            double mean = prediction[0];
            double std = prediction[1];

            // Compute Expected Improvement
            double ei = computeExpectedImprovement(mean, std, currentBest);

            if (ei > bestEI) {
                bestEI = ei;
                bestCandidate = candidate;
            }
        }

        return bestCandidate != null ? bestCandidate : new double[dimensions];
    }

    /**
     * Predict objective value using surrogate model (simplified GP).
     *
     * Uses RBF kernel weighted average of observed points.
     * Returns [mean, standard_deviation].
     */
    private double[] predictWithSurrogate(double[] point, List<double[]> observedPoints, List<Double> observedValues) {
        if (observedPoints.isEmpty()) {
            return new double[]{0.0, 1.0};
        }

        // Compute kernel weights
        double[] weights = new double[observedPoints.size()];
        double totalWeight = 0.0;

        for (int i = 0; i < observedPoints.size(); i++) {
            double dist = euclideanDistance(point, observedPoints.get(i));
            weights[i] = rbfKernel(dist);
            totalWeight += weights[i];
        }

        // Normalize weights
        if (totalWeight > 0) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] /= totalWeight;
            }
        }

        // Compute weighted mean
        double mean = 0.0;
        for (int i = 0; i < observedValues.size(); i++) {
            mean += weights[i] * observedValues.get(i);
        }

        // Compute weighted variance
        double variance = 0.0;
        for (int i = 0; i < observedValues.size(); i++) {
            double diff = observedValues.get(i) - mean;
            variance += weights[i] * diff * diff;
        }

        // Add uncertainty based on distance from observed points
        double minDist = Double.MAX_VALUE;
        for (double[] observed : observedPoints) {
            minDist = Math.min(minDist, euclideanDistance(point, observed));
        }
        double uncertaintyBoost = minDist * EXPLORATION_FACTOR;

        double std = Math.sqrt(variance + uncertaintyBoost);
        return new double[]{mean, Math.max(std, 0.01)}; // Ensure minimum std
    }

    /**
     * Compute Expected Improvement acquisition function.
     */
    private double computeExpectedImprovement(double mean, double std, double currentBest) {
        if (std < 1e-10) {
            return 0.0;
        }

        double z = (mean - currentBest) / std;
        double ei = (mean - currentBest) * normalCDF(z) + std * normalPDF(z);

        return Math.max(ei, 0.0);
    }

    /**
     * RBF (Gaussian) kernel.
     */
    private double rbfKernel(double distance) {
        return Math.exp(-0.5 * Math.pow(distance / LENGTH_SCALE, 2));
    }

    /**
     * Euclidean distance between two points.
     */
    private double euclideanDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += Math.pow(a[i] - b[i], 2);
        }
        return Math.sqrt(sum);
    }

    /**
     * Standard normal PDF.
     */
    private double normalPDF(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }

    /**
     * Standard normal CDF approximation.
     */
    private double normalCDF(double x) {
        // Approximation using error function
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(x));
        double d = 0.3989423 * Math.exp(-x * x / 2.0);
        double p = d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))));
        return x > 0 ? 1 - p : p;
    }

    /**
     * Run a single backtest with given parameters.
     */
    private BacktestResult runBacktest(BacktestConfig baseConfig, Map<String, Object> params)
            throws BacktestException {

        BacktestConfig config = BacktestConfig.builder()
                .backtestId(UlidGenerator.generate())
                .strategyId(baseConfig.getStrategyId())
                .strategyType(baseConfig.getStrategyType())
                .symbols(baseConfig.getSymbols())
                .startDate(baseConfig.getStartDate())
                .endDate(baseConfig.getEndDate())
                .timeframe(baseConfig.getTimeframe())
                .initialCapital(baseConfig.getInitialCapital())
                .commission(baseConfig.getCommission())
                .slippage(baseConfig.getSlippage())
                .strategyParams(params)
                .build();

        return backtestEngine.run(config);
    }

    /**
     * Extract objective metric from backtest result.
     */
    private BigDecimal extractObjective(BacktestResult result, OptimizationConfig.OptimizationObjective objective) {
        switch (objective) {
            case TOTAL_RETURN:
                return result.getTotalReturn() != null ? result.getTotalReturn() : BigDecimal.ZERO;
            case SHARPE_RATIO:
                return result.getPerformanceMetrics() != null && result.getPerformanceMetrics().getSharpeRatio() != null
                        ? result.getPerformanceMetrics().getSharpeRatio()
                        : BigDecimal.ZERO;
            case SORTINO_RATIO:
                return result.getPerformanceMetrics() != null && result.getPerformanceMetrics().getSortinoRatio() != null
                        ? result.getPerformanceMetrics().getSortinoRatio()
                        : BigDecimal.ZERO;
            case PROFIT_FACTOR:
                return result.getPerformanceMetrics() != null && result.getPerformanceMetrics().getProfitFactor() != null
                        ? result.getPerformanceMetrics().getProfitFactor()
                        : BigDecimal.ZERO;
            case CALMAR_RATIO:
                return result.getRiskMetrics() != null && result.getRiskMetrics().getCalmarRatio() != null
                        ? result.getRiskMetrics().getCalmarRatio()
                        : BigDecimal.ZERO;
            default:
                return BigDecimal.ZERO;
        }
    }
}
