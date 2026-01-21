package maru.trading.application.backtest;

import maru.trading.domain.backtest.BacktestResult;
import maru.trading.domain.backtest.BacktestTrade;
import maru.trading.domain.backtest.montecarlo.MonteCarloConfig;
import maru.trading.domain.backtest.montecarlo.MonteCarloResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Monte Carlo simulation engine.
 *
 * Generates multiple possible equity curve paths by resampling
 * historical trade returns to estimate outcome distributions.
 */
@Component
public class MonteCarloSimulator {

    private static final Logger log = LoggerFactory.getLogger(MonteCarloSimulator.class);

    /**
     * Run Monte Carlo simulation.
     *
     * @param config Simulation configuration
     * @return Simulation result with statistics
     */
    public MonteCarloResult simulate(MonteCarloConfig config) {
        log.info("========================================");
        log.info("Starting Monte Carlo Simulation");
        log.info("========================================");
        log.info("Method: {}", config.getMethod());
        log.info("Simulations: {}", config.getNumSimulations());
        log.info("Confidence Level: {}", config.getConfidenceLevel());

        LocalDateTime startTime = LocalDateTime.now();

        // Extract trade returns from base backtest
        BacktestResult baseResult = config.getBaseBacktestResult();
        List<BigDecimal> tradeReturns = extractTradeReturns(baseResult);

        if (tradeReturns.isEmpty()) {
            log.warn("No trades in base backtest result");
            return createEmptyResult(config, startTime);
        }

        log.info("Base trades: {}", tradeReturns.size());

        // Initialize random generator
        Random random = config.getRandomSeed() != null
                ? new Random(config.getRandomSeed())
                : new Random();

        // Run simulations
        List<SimulationRun> simulations = new ArrayList<>();
        BigDecimal initialCapital = baseResult.getConfig() != null && baseResult.getConfig().getInitialCapital() != null
                ? baseResult.getConfig().getInitialCapital()
                : BigDecimal.valueOf(10000000);

        for (int i = 0; i < config.getNumSimulations(); i++) {
            SimulationRun run = runSingleSimulation(
                    i + 1,
                    tradeReturns,
                    initialCapital,
                    config,
                    random
            );
            simulations.add(run);

            if ((i + 1) % 100 == 0) {
                log.info("Completed {} / {} simulations", i + 1, config.getNumSimulations());
            }
        }

        // Analyze results
        MonteCarloResult result = analyzeSimulations(config, simulations, startTime);

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        log.info("========================================");
        log.info("Monte Carlo Simulation Complete");
        log.info("========================================");
        log.info("Mean Return: {}%", result.getMeanReturn());
        log.info("Probability of Profit: {}%", result.getProbabilityOfProfit());
        log.info("VaR ({}%): {}%", config.getConfidenceLevel().multiply(BigDecimal.valueOf(100)), result.getValueAtRisk());
        log.info("CVaR: {}%", result.getConditionalVaR());
        log.info("Duration: {}ms", durationMs);

        return result;
    }

    /**
     * Extract trade returns from backtest result.
     */
    private List<BigDecimal> extractTradeReturns(BacktestResult result) {
        if (result.getTrades() == null || result.getTrades().isEmpty()) {
            return Collections.emptyList();
        }

        return result.getTrades().stream()
                .filter(t -> t.getNetPnl() != null)
                .map(BacktestTrade::getNetPnl)
                .collect(Collectors.toList());
    }

    /**
     * Run a single Monte Carlo simulation.
     */
    private SimulationRun runSingleSimulation(
            int simNumber,
            List<BigDecimal> tradeReturns,
            BigDecimal initialCapital,
            MonteCarloConfig config,
            Random random) {

        List<BigDecimal> simulatedReturns;

        switch (config.getMethod()) {
            case BOOTSTRAP:
                simulatedReturns = bootstrapSample(tradeReturns, config, random);
                break;
            case PERMUTATION:
                simulatedReturns = permutationSample(tradeReturns, random);
                break;
            case PARAMETRIC:
                simulatedReturns = parametricSample(tradeReturns, random);
                break;
            default:
                simulatedReturns = bootstrapSample(tradeReturns, config, random);
        }

        // Build equity curve
        List<BigDecimal> equityCurve = new ArrayList<>();
        BigDecimal equity = initialCapital;
        BigDecimal peak = equity;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        equityCurve.add(equity);

        for (BigDecimal returnVal : simulatedReturns) {
            equity = equity.add(returnVal);
            equityCurve.add(equity);

            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }

            BigDecimal drawdown = peak.subtract(equity)
                    .divide(peak, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        BigDecimal totalReturn = equity.subtract(initialCapital)
                .divide(initialCapital, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return new SimulationRun(simNumber, totalReturn, maxDrawdown, equity, equityCurve);
    }

    /**
     * Bootstrap sampling - random sampling with replacement.
     */
    private List<BigDecimal> bootstrapSample(List<BigDecimal> returns, MonteCarloConfig config, Random random) {
        int n = returns.size();
        List<BigDecimal> sampled = new ArrayList<>(n);

        if (config.isPreserveCorrelation()) {
            // Block bootstrap
            int blockSize = config.getBlockSize();
            int numBlocks = (n + blockSize - 1) / blockSize;

            for (int b = 0; b < numBlocks && sampled.size() < n; b++) {
                int startIdx = random.nextInt(n);
                for (int i = 0; i < blockSize && sampled.size() < n; i++) {
                    int idx = (startIdx + i) % n;
                    sampled.add(returns.get(idx));
                }
            }
        } else {
            // Standard bootstrap
            for (int i = 0; i < n; i++) {
                sampled.add(returns.get(random.nextInt(n)));
            }
        }

        return sampled;
    }

    /**
     * Permutation sampling - shuffle order.
     */
    private List<BigDecimal> permutationSample(List<BigDecimal> returns, Random random) {
        List<BigDecimal> shuffled = new ArrayList<>(returns);
        Collections.shuffle(shuffled, random);
        return shuffled;
    }

    /**
     * Parametric sampling - assume normal distribution.
     */
    private List<BigDecimal> parametricSample(List<BigDecimal> returns, Random random) {
        // Calculate mean and std
        double mean = returns.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0);

        double variance = returns.stream()
                .mapToDouble(BigDecimal::doubleValue)
                .map(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        double std = Math.sqrt(variance);

        // Generate normally distributed returns
        List<BigDecimal> sampled = new ArrayList<>(returns.size());
        for (int i = 0; i < returns.size(); i++) {
            double value = mean + std * random.nextGaussian();
            sampled.add(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP));
        }

        return sampled;
    }

    /**
     * Analyze all simulation runs.
     */
    private MonteCarloResult analyzeSimulations(
            MonteCarloConfig config,
            List<SimulationRun> simulations,
            LocalDateTime startTime) {

        int n = simulations.size();

        // Extract returns and drawdowns
        List<BigDecimal> allReturns = simulations.stream()
                .map(s -> s.totalReturn)
                .sorted()
                .collect(Collectors.toList());

        List<BigDecimal> allDrawdowns = simulations.stream()
                .map(s -> s.maxDrawdown)
                .sorted()
                .collect(Collectors.toList());

        // Basic statistics
        BigDecimal meanReturn = average(allReturns);
        BigDecimal medianReturn = percentile(allReturns, 50);
        BigDecimal stdDevReturn = standardDeviation(allReturns);
        BigDecimal minReturn = allReturns.get(0);
        BigDecimal maxReturn = allReturns.get(n - 1);

        // VaR and CVaR
        double confidencePct = config.getConfidenceLevel().doubleValue() * 100;
        int varIdx = (int) Math.floor(n * (1 - config.getConfidenceLevel().doubleValue()));
        BigDecimal var = allReturns.get(Math.max(0, varIdx));

        // CVaR = average of returns below VaR
        List<BigDecimal> tailReturns = allReturns.subList(0, Math.max(1, varIdx + 1));
        BigDecimal cvar = average(tailReturns);

        // Probabilities
        long profitCount = allReturns.stream().filter(r -> r.compareTo(BigDecimal.ZERO) > 0).count();
        BigDecimal probabilityOfProfit = BigDecimal.valueOf(profitCount)
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Target return probability (assume target = mean of base)
        BigDecimal targetReturn = meanReturn;
        long targetCount = allReturns.stream().filter(r -> r.compareTo(targetReturn) >= 0).count();
        BigDecimal probabilityOfTarget = BigDecimal.valueOf(targetCount)
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Probability of ruin (loss > 50%)
        BigDecimal ruinThreshold = BigDecimal.valueOf(-50);
        long ruinCount = allReturns.stream().filter(r -> r.compareTo(ruinThreshold) < 0).count();
        BigDecimal probabilityOfRuin = BigDecimal.valueOf(ruinCount)
                .divide(BigDecimal.valueOf(n), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Percentiles
        Map<Integer, BigDecimal> returnPercentiles = new LinkedHashMap<>();
        returnPercentiles.put(5, percentile(allReturns, 5));
        returnPercentiles.put(10, percentile(allReturns, 10));
        returnPercentiles.put(25, percentile(allReturns, 25));
        returnPercentiles.put(50, percentile(allReturns, 50));
        returnPercentiles.put(75, percentile(allReturns, 75));
        returnPercentiles.put(90, percentile(allReturns, 90));
        returnPercentiles.put(95, percentile(allReturns, 95));

        Map<Integer, BigDecimal> drawdownPercentiles = new LinkedHashMap<>();
        drawdownPercentiles.put(50, percentile(allDrawdowns, 50));
        drawdownPercentiles.put(75, percentile(allDrawdowns, 75));
        drawdownPercentiles.put(90, percentile(allDrawdowns, 90));
        drawdownPercentiles.put(95, percentile(allDrawdowns, 95));

        // Drawdown statistics
        MonteCarloResult.DrawdownStatistics ddStats = MonteCarloResult.DrawdownStatistics.builder()
                .meanMaxDrawdown(average(allDrawdowns))
                .medianMaxDrawdown(percentile(allDrawdowns, 50))
                .stdDevMaxDrawdown(standardDeviation(allDrawdowns))
                .worstMaxDrawdown(allDrawdowns.get(n - 1))
                .bestMaxDrawdown(allDrawdowns.get(0))
                .build();

        // Distribution histogram
        List<MonteCarloResult.DistributionBin> returnDistribution =
                createHistogram(allReturns, config.getDistributionBins());

        // Confidence intervals
        BigDecimal[] ci95 = new BigDecimal[]{
                percentile(allReturns, 2.5),
                percentile(allReturns, 97.5)
        };
        BigDecimal[] ci99 = new BigDecimal[]{
                percentile(allReturns, 0.5),
                percentile(allReturns, 99.5)
        };

        // Best/Worst/Median cases
        SimulationRun worst = simulations.stream()
                .min(Comparator.comparing(s -> s.totalReturn))
                .orElse(simulations.get(0));

        SimulationRun best = simulations.stream()
                .max(Comparator.comparing(s -> s.totalReturn))
                .orElse(simulations.get(0));

        // Find median case
        List<SimulationRun> sortedByReturn = simulations.stream()
                .sorted(Comparator.comparing(s -> s.totalReturn))
                .collect(Collectors.toList());
        SimulationRun median = sortedByReturn.get(n / 2);

        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

        return MonteCarloResult.builder()
                .simulationId(config.getSimulationId())
                .config(config)
                .numSimulations(n)
                // Return statistics
                .meanReturn(meanReturn)
                .medianReturn(medianReturn)
                .stdDevReturn(stdDevReturn)
                .minReturn(minReturn)
                .maxReturn(maxReturn)
                // Risk metrics
                .valueAtRisk(var)
                .conditionalVaR(cvar)
                .maxDrawdownStats(ddStats)
                // Probabilities
                .probabilityOfProfit(probabilityOfProfit)
                .probabilityOfTargetReturn(probabilityOfTarget)
                .targetReturn(targetReturn)
                .probabilityOfRuin(probabilityOfRuin)
                .ruinThreshold(ruinThreshold)
                // Percentiles
                .returnPercentiles(returnPercentiles)
                .drawdownPercentiles(drawdownPercentiles)
                // Distribution
                .returnDistribution(returnDistribution)
                .equityDistribution(null)  // Optional
                // Confidence intervals
                .returnConfidenceInterval95(ci95)
                .returnConfidenceInterval99(ci99)
                // Best/Worst/Median
                .bestCase(toSimulationPath(best))
                .worstCase(toSimulationPath(worst))
                .medianCase(toSimulationPath(median))
                // Execution info
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(durationMs)
                .build();
    }

    /**
     * Calculate average.
     */
    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
    }

    /**
     * Calculate standard deviation.
     */
    private BigDecimal standardDeviation(List<BigDecimal> values) {
        if (values.size() < 2) return BigDecimal.ZERO;

        BigDecimal mean = average(values);
        BigDecimal variance = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
                .setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Calculate percentile.
     */
    private BigDecimal percentile(List<BigDecimal> sortedValues, double pct) {
        if (sortedValues.isEmpty()) return BigDecimal.ZERO;

        int idx = (int) Math.floor(sortedValues.size() * pct / 100.0);
        idx = Math.max(0, Math.min(idx, sortedValues.size() - 1));
        return sortedValues.get(idx);
    }

    /**
     * Create histogram from values.
     */
    private List<MonteCarloResult.DistributionBin> createHistogram(List<BigDecimal> values, int numBins) {
        if (values.isEmpty()) return Collections.emptyList();

        BigDecimal min = values.get(0);
        BigDecimal max = values.get(values.size() - 1);
        BigDecimal range = max.subtract(min);

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            // All values same
            return List.of(MonteCarloResult.DistributionBin.builder()
                    .binStart(min)
                    .binEnd(max)
                    .binCenter(min)
                    .count(values.size())
                    .frequency(BigDecimal.ONE)
                    .build());
        }

        BigDecimal binWidth = range.divide(BigDecimal.valueOf(numBins), 6, RoundingMode.HALF_UP);

        List<MonteCarloResult.DistributionBin> bins = new ArrayList<>();
        int total = values.size();

        for (int i = 0; i < numBins; i++) {
            BigDecimal binStart = min.add(binWidth.multiply(BigDecimal.valueOf(i)));
            BigDecimal binEnd = binStart.add(binWidth);
            BigDecimal binCenter = binStart.add(binWidth.divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP));

            final BigDecimal start = binStart;
            final BigDecimal end = binEnd;
            final int binIndex = i;

            int count = (int) values.stream()
                    .filter(v -> {
                        if (binIndex == numBins - 1) {
                            return v.compareTo(start) >= 0 && v.compareTo(end) <= 0;
                        }
                        return v.compareTo(start) >= 0 && v.compareTo(end) < 0;
                    })
                    .count();

            BigDecimal frequency = BigDecimal.valueOf(count)
                    .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP);

            bins.add(MonteCarloResult.DistributionBin.builder()
                    .binStart(binStart)
                    .binEnd(binEnd)
                    .binCenter(binCenter)
                    .count(count)
                    .frequency(frequency)
                    .build());
        }

        return bins;
    }

    /**
     * Convert SimulationRun to SimulationPath.
     */
    private MonteCarloResult.SimulationPath toSimulationPath(SimulationRun run) {
        // Sample equity curve to reduce size (max 100 points)
        List<BigDecimal> sampledCurve = sampleEquityCurve(run.equityCurve, 100);

        return MonteCarloResult.SimulationPath.builder()
                .simulationNumber(run.simNumber)
                .totalReturn(run.totalReturn)
                .maxDrawdown(run.maxDrawdown)
                .finalEquity(run.finalEquity)
                .equityCurve(sampledCurve)
                .build();
    }

    /**
     * Sample equity curve to reduce points.
     */
    private List<BigDecimal> sampleEquityCurve(List<BigDecimal> curve, int maxPoints) {
        if (curve.size() <= maxPoints) return curve;

        List<BigDecimal> sampled = new ArrayList<>();
        double step = (double) curve.size() / maxPoints;

        for (int i = 0; i < maxPoints; i++) {
            int idx = (int) (i * step);
            sampled.add(curve.get(idx));
        }

        // Always include last point
        if (!sampled.get(sampled.size() - 1).equals(curve.get(curve.size() - 1))) {
            sampled.add(curve.get(curve.size() - 1));
        }

        return sampled;
    }

    /**
     * Create empty result when no trades.
     */
    private MonteCarloResult createEmptyResult(MonteCarloConfig config, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        return MonteCarloResult.builder()
                .simulationId(config.getSimulationId())
                .config(config)
                .numSimulations(0)
                .meanReturn(BigDecimal.ZERO)
                .medianReturn(BigDecimal.ZERO)
                .stdDevReturn(BigDecimal.ZERO)
                .minReturn(BigDecimal.ZERO)
                .maxReturn(BigDecimal.ZERO)
                .valueAtRisk(BigDecimal.ZERO)
                .conditionalVaR(BigDecimal.ZERO)
                .probabilityOfProfit(BigDecimal.ZERO)
                .probabilityOfTargetReturn(BigDecimal.ZERO)
                .targetReturn(BigDecimal.ZERO)
                .probabilityOfRuin(BigDecimal.ZERO)
                .ruinThreshold(BigDecimal.valueOf(-50))
                .returnPercentiles(Collections.emptyMap())
                .drawdownPercentiles(Collections.emptyMap())
                .returnDistribution(Collections.emptyList())
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .build();
    }

    /**
     * Internal class for simulation run data.
     */
    private static class SimulationRun {
        final int simNumber;
        final BigDecimal totalReturn;
        final BigDecimal maxDrawdown;
        final BigDecimal finalEquity;
        final List<BigDecimal> equityCurve;

        SimulationRun(int simNumber, BigDecimal totalReturn, BigDecimal maxDrawdown,
                     BigDecimal finalEquity, List<BigDecimal> equityCurve) {
            this.simNumber = simNumber;
            this.totalReturn = totalReturn;
            this.maxDrawdown = maxDrawdown;
            this.finalEquity = finalEquity;
            this.equityCurve = equityCurve;
        }
    }
}
