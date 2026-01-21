package maru.trading.application.backtest;

import maru.trading.domain.backtest.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance Analyzer implementation.
 *
 * Calculates comprehensive performance and risk metrics from backtest results.
 */
@Component
public class PerformanceAnalyzerImpl implements PerformanceAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAnalyzerImpl.class);

    private static final BigDecimal RISK_FREE_RATE = BigDecimal.valueOf(0.03); // 3% annual
    private static final int SCALE = 8;
    private static final int DISPLAY_SCALE = 4;

    @Override
    public PerformanceMetrics analyze(BacktestResult result) {
        log.info("Analyzing backtest performance...");

        // Calculate returns (always calculate, regardless of trades)
        BigDecimal totalReturn = calculateTotalReturn(result);
        BigDecimal annualReturn = calculateAnnualReturn(result);

        List<BacktestTrade> trades = result.getTrades();
        if (trades == null || trades.isEmpty()) {
            log.warn("No trades to analyze");
            return PerformanceMetrics.builder()
                    .totalReturn(totalReturn)
                    .annualReturn(annualReturn)
                    .build();
        }

        // Calculate trade statistics
        int totalTrades = trades.size();
        List<BacktestTrade> winningTrades = trades.stream()
                .filter(BacktestTrade::isWinner)
                .toList();
        List<BacktestTrade> losingTrades = trades.stream()
                .filter(BacktestTrade::isLoser)
                .toList();

        int winningCount = winningTrades.size();
        int losingCount = losingTrades.size();

        BigDecimal winRate = totalTrades > 0
                ? BigDecimal.valueOf(winningCount).divide(BigDecimal.valueOf(totalTrades), SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate profit/loss statistics
        BigDecimal totalProfit = winningTrades.stream()
                .map(BacktestTrade::getNetPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLoss = losingTrades.stream()
                .map(BacktestTrade::getNetPnl)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgWin = winningCount > 0
                ? totalProfit.divide(BigDecimal.valueOf(winningCount), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgLoss = losingCount > 0
                ? totalLoss.divide(BigDecimal.valueOf(losingCount), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal avgTrade = totalTrades > 0
                ? trades.stream()
                .map(BacktestTrade::getNetPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(totalTrades), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal profitFactor = totalLoss.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalLoss, SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Find largest win/loss
        BigDecimal largestWin = winningTrades.stream()
                .map(BacktestTrade::getNetPnl)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal largestLoss = losingTrades.stream()
                .map(BacktestTrade::getNetPnl)
                .map(BigDecimal::abs)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Calculate consecutive wins/losses
        int maxConsecutiveWins = calculateMaxConsecutiveWins(trades);
        int maxConsecutiveLosses = calculateMaxConsecutiveLosses(trades);

        // Calculate Sharpe and Sortino ratios
        BigDecimal sharpeRatio = calculateSharpeRatio(trades, annualReturn);
        BigDecimal sortinoRatio = calculateSortinoRatio(trades, annualReturn);

        // Calculate drawdown
        BigDecimal maxDrawdown = calculateMaxDrawdown(result);
        Integer maxDrawdownDuration = calculateMaxDrawdownDuration(result);

        return PerformanceMetrics.builder()
                .totalReturn(totalReturn)
                .annualReturn(annualReturn)
                .sharpeRatio(sharpeRatio)
                .sortinoRatio(sortinoRatio)
                .maxDrawdown(maxDrawdown)
                .maxDrawdownDuration(maxDrawdownDuration)
                .totalTrades(totalTrades)
                .winningTrades(winningCount)
                .losingTrades(losingCount)
                .winRate(winRate)
                .profitFactor(profitFactor)
                .avgWin(avgWin)
                .avgLoss(avgLoss)
                .avgTrade(avgTrade)
                .largestWin(largestWin)
                .largestLoss(largestLoss)
                .totalProfit(totalProfit)
                .totalLoss(totalLoss)
                .maxConsecutiveWins(maxConsecutiveWins)
                .maxConsecutiveLosses(maxConsecutiveLosses)
                .build();
    }

    @Override
    public RiskMetrics analyzeRisk(BacktestResult result) {
        log.info("Analyzing backtest risk...");

        List<BacktestTrade> trades = result.getTrades();
        if (trades == null || trades.isEmpty()) {
            return RiskMetrics.builder().build();
        }

        // Calculate volatility
        BigDecimal volatility = calculateVolatility(trades);
        BigDecimal downsideDeviation = calculateDownsideDeviation(trades);

        // Calculate VaR and CVaR (95% confidence)
        BigDecimal var95 = calculateVaR(trades, 0.95);
        BigDecimal cvar95 = calculateCVaR(trades, 0.95);

        // Calculate ratios
        BigDecimal maxDrawdown = calculateMaxDrawdown(result);
        BigDecimal annualReturn = calculateAnnualReturn(result);

        BigDecimal calmarRatio = maxDrawdown.compareTo(BigDecimal.ZERO) > 0
                ? annualReturn.divide(maxDrawdown.abs(), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal recoveryFactor = maxDrawdown.compareTo(BigDecimal.ZERO) > 0
                ? result.getTotalReturn().divide(maxDrawdown.abs(), SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate advanced risk metrics
        BigDecimal omegaRatio = calculateOmegaRatio(trades);
        BigDecimal skewness = calculateSkewness(trades);
        BigDecimal kurtosis = calculateKurtosis(trades);
        BigDecimal excessKurtosis = kurtosis.subtract(BigDecimal.valueOf(3));
        BigDecimal kellyFraction = calculateKellyFraction(trades);
        BigDecimal halfKelly = kellyFraction.divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
        BigDecimal tailRatio = calculateTailRatio(trades);
        BigDecimal gainToPainRatio = calculateGainToPainRatio(trades);

        return RiskMetrics.builder()
                .volatility(volatility)
                .downsideDeviation(downsideDeviation)
                .var95(var95)
                .cvar95(cvar95)
                .calmarRatio(calmarRatio)
                .recoveryFactor(recoveryFactor)
                .omegaRatio(omegaRatio)
                .skewness(skewness)
                .kurtosis(kurtosis)
                .excessKurtosis(excessKurtosis)
                .kellyFraction(kellyFraction)
                .halfKelly(halfKelly)
                .tailRatio(tailRatio)
                .gainToPainRatio(gainToPainRatio)
                .build();
    }

    @Override
    public EquityCurve generateEquityCurve(BacktestResult result) {
        log.info("Generating equity curve...");

        EquityCurve curve = EquityCurve.builder().build();
        BigDecimal currentEquity = result.getConfig().getInitialCapital();

        // Add initial point
        if (result.getStartTime() != null) {
            curve.addPoint(result.getStartTime(), currentEquity);
        }

        // Add point for each trade
        for (BacktestTrade trade : result.getTrades()) {
            if (trade.getExitTime() != null && trade.getNetPnl() != null) {
                currentEquity = currentEquity.add(trade.getNetPnl());
                curve.addPoint(trade.getExitTime(), currentEquity);
            }
        }

        return curve;
    }

    // ==================== Helper Methods ====================

    private BigDecimal calculateTotalReturn(BacktestResult result) {
        BigDecimal initialCapital = result.getConfig().getInitialCapital();
        BigDecimal finalCapital = result.getFinalCapital();

        if (initialCapital.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return finalCapital.subtract(initialCapital)
                .divide(initialCapital, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateAnnualReturn(BacktestResult result) {
        if (result.getStartTime() == null || result.getEndTime() == null) {
            return BigDecimal.ZERO;
        }

        long days = ChronoUnit.DAYS.between(result.getStartTime(), result.getEndTime());
        if (days <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal years = BigDecimal.valueOf(days).divide(BigDecimal.valueOf(365), SCALE, RoundingMode.HALF_UP);
        BigDecimal totalReturn = calculateTotalReturn(result).divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);

        // Annualized return = (1 + total_return) ^ (1/years) - 1
        // Simplified: total_return / years for small periods
        return totalReturn.divide(years, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal calculateSharpeRatio(List<BacktestTrade> trades, BigDecimal annualReturn) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal volatility = calculateVolatility(trades);
        if (volatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal excessReturn = annualReturn.subtract(RISK_FREE_RATE.multiply(BigDecimal.valueOf(100)));
        return excessReturn.divide(volatility, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSortinoRatio(List<BacktestTrade> trades, BigDecimal annualReturn) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal downsideDeviation = calculateDownsideDeviation(trades);
        if (downsideDeviation.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal excessReturn = annualReturn.subtract(RISK_FREE_RATE.multiply(BigDecimal.valueOf(100)));
        return excessReturn.divide(downsideDeviation, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateVolatility(List<BacktestTrade> trades) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Calculate mean return
        BigDecimal meanReturn = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(trades.size()), SCALE, RoundingMode.HALF_UP);

        // Calculate variance
        BigDecimal variance = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .map(r -> r.subtract(meanReturn).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(trades.size()), SCALE, RoundingMode.HALF_UP);

        // Standard deviation (volatility)
        double stdDev = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(stdDev);
    }

    private BigDecimal calculateDownsideDeviation(List<BacktestTrade> trades) {
        List<BigDecimal> negativeReturns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null && r.compareTo(BigDecimal.ZERO) < 0)
                .toList();

        if (negativeReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal variance = negativeReturns.stream()
                .map(r -> r.pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(negativeReturns.size()), SCALE, RoundingMode.HALF_UP);

        double stdDev = Math.sqrt(variance.doubleValue());
        return BigDecimal.valueOf(stdDev);
    }

    private BigDecimal calculateMaxDrawdown(BacktestResult result) {
        EquityCurve curve = generateEquityCurve(result);
        List<EquityCurve.EquityPoint> points = curve.getPoints();

        if (points.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal peak = points.get(0).getEquity();

        for (EquityCurve.EquityPoint point : points) {
            if (point.getEquity().compareTo(peak) > 0) {
                peak = point.getEquity();
            }

            BigDecimal drawdown = peak.subtract(point.getEquity())
                    .divide(peak, SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown.negate(); // Return as negative value
    }

    private Integer calculateMaxDrawdownDuration(BacktestResult result) {
        EquityCurve curve = generateEquityCurve(result);
        List<EquityCurve.EquityPoint> points = curve.getPoints();

        if (points.size() < 2) {
            return 0;
        }

        int maxDuration = 0;
        int currentDuration = 0;
        BigDecimal peak = points.get(0).getEquity();
        LocalDateTime peakTime = points.get(0).getTimestamp();
        boolean inDrawdown = false;

        for (EquityCurve.EquityPoint point : points) {
            if (point.getEquity().compareTo(peak) >= 0) {
                // New peak reached or recovered
                if (inDrawdown) {
                    // Calculate duration from peak to recovery
                    long days = ChronoUnit.DAYS.between(peakTime, point.getTimestamp());
                    currentDuration = (int) Math.max(days, 1); // At least 1 day if there was a drawdown
                    maxDuration = Math.max(maxDuration, currentDuration);
                    inDrawdown = false;
                }
                // Update peak
                peak = point.getEquity();
                peakTime = point.getTimestamp();
                currentDuration = 0;
            } else {
                // In drawdown
                if (!inDrawdown) {
                    inDrawdown = true;
                }
            }
        }

        // If still in drawdown at end, calculate duration to last point
        if (inDrawdown && !points.isEmpty()) {
            LocalDateTime lastTime = points.get(points.size() - 1).getTimestamp();
            long days = ChronoUnit.DAYS.between(peakTime, lastTime);
            currentDuration = (int) Math.max(days, 1);
            maxDuration = Math.max(maxDuration, currentDuration);
        }

        return maxDuration;
    }

    private BigDecimal calculateVaR(List<BacktestTrade> trades, double confidence) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> returns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .sorted()
                .toList();

        int index = (int) ((1 - confidence) * returns.size());
        if (index >= returns.size()) {
            index = returns.size() - 1;
        }

        return returns.get(index);
    }

    private BigDecimal calculateCVaR(List<BacktestTrade> trades, double confidence) {
        BigDecimal var = calculateVaR(trades, confidence);

        List<BigDecimal> worseThanVaR = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null && r.compareTo(var) < 0)
                .toList();

        if (worseThanVaR.isEmpty()) {
            return var;
        }

        return worseThanVaR.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(worseThanVaR.size()), SCALE, RoundingMode.HALF_UP);
    }

    private int calculateMaxConsecutiveWins(List<BacktestTrade> trades) {
        int maxWins = 0;
        int currentWins = 0;

        for (BacktestTrade trade : trades) {
            if (trade.isWinner()) {
                currentWins++;
                maxWins = Math.max(maxWins, currentWins);
            } else {
                currentWins = 0;
            }
        }

        return maxWins;
    }

    private int calculateMaxConsecutiveLosses(List<BacktestTrade> trades) {
        int maxLosses = 0;
        int currentLosses = 0;

        for (BacktestTrade trade : trades) {
            if (trade.isLoser()) {
                currentLosses++;
                maxLosses = Math.max(maxLosses, currentLosses);
            } else {
                currentLosses = 0;
            }
        }

        return maxLosses;
    }

    // ==================== Advanced Risk Metrics ====================

    /**
     * Calculate Omega Ratio.
     *
     * Omega = Sum of returns above threshold / Sum of returns below threshold
     * Threshold is typically 0 (risk-free rate adjusted).
     */
    private BigDecimal calculateOmegaRatio(List<BacktestTrade> trades) {
        BigDecimal threshold = BigDecimal.ZERO; // Can be set to MAR (minimum acceptable return)

        List<BigDecimal> returns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .toList();

        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal gains = BigDecimal.ZERO;
        BigDecimal losses = BigDecimal.ZERO;

        for (BigDecimal ret : returns) {
            if (ret.compareTo(threshold) > 0) {
                gains = gains.add(ret.subtract(threshold));
            } else {
                losses = losses.add(threshold.subtract(ret));
            }
        }

        if (losses.compareTo(BigDecimal.ZERO) == 0) {
            return gains.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(999.99) : BigDecimal.ONE;
        }

        return gains.divide(losses, SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Skewness (third standardized moment).
     *
     * Skewness = E[(X - μ)³] / σ³
     */
    private BigDecimal calculateSkewness(List<BacktestTrade> trades) {
        List<BigDecimal> returns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .toList();

        if (returns.size() < 3) {
            return BigDecimal.ZERO;
        }

        int n = returns.size();

        // Calculate mean
        BigDecimal sum = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);

        // Calculate standard deviation
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(mean);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);
        double stdDev = Math.sqrt(variance.doubleValue());

        if (stdDev == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate third moment
        double thirdMoment = 0.0;
        for (BigDecimal ret : returns) {
            double diff = ret.subtract(mean).doubleValue();
            thirdMoment += Math.pow(diff, 3);
        }
        thirdMoment /= n;

        // Skewness = third moment / std^3
        double skewness = thirdMoment / Math.pow(stdDev, 3);

        return BigDecimal.valueOf(skewness).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Kurtosis (fourth standardized moment).
     *
     * Kurtosis = E[(X - μ)⁴] / σ⁴
     * Normal distribution has kurtosis = 3.
     */
    private BigDecimal calculateKurtosis(List<BacktestTrade> trades) {
        List<BigDecimal> returns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .toList();

        if (returns.size() < 4) {
            return BigDecimal.valueOf(3); // Return normal distribution kurtosis
        }

        int n = returns.size();

        // Calculate mean
        BigDecimal sum = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);

        // Calculate standard deviation
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(mean);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(n), SCALE, RoundingMode.HALF_UP);
        double stdDev = Math.sqrt(variance.doubleValue());

        if (stdDev == 0) {
            return BigDecimal.valueOf(3);
        }

        // Calculate fourth moment
        double fourthMoment = 0.0;
        for (BigDecimal ret : returns) {
            double diff = ret.subtract(mean).doubleValue();
            fourthMoment += Math.pow(diff, 4);
        }
        fourthMoment /= n;

        // Kurtosis = fourth moment / std^4
        double kurtosis = fourthMoment / Math.pow(stdDev, 4);

        return BigDecimal.valueOf(kurtosis).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Kelly Fraction - optimal bet size for geometric growth.
     *
     * Kelly = (W * AvgWin - L * AvgLoss) / AvgWin
     *       = (p * b - q) / b
     * where p = win probability, q = 1-p, b = avg win/avg loss ratio
     */
    private BigDecimal calculateKellyFraction(List<BacktestTrade> trades) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long winners = trades.stream().filter(BacktestTrade::isWinner).count();
        long losers = trades.stream().filter(BacktestTrade::isLoser).count();
        int total = trades.size();

        if (total == 0 || losers == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal winProb = BigDecimal.valueOf(winners).divide(BigDecimal.valueOf(total), SCALE, RoundingMode.HALF_UP);
        BigDecimal lossProb = BigDecimal.ONE.subtract(winProb);

        // Calculate average win and loss
        BigDecimal avgWin = trades.stream()
                .filter(BacktestTrade::isWinner)
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        avgWin = winners > 0 ? avgWin.divide(BigDecimal.valueOf(winners), SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal avgLoss = trades.stream()
                .filter(BacktestTrade::isLoser)
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        avgLoss = losers > 0 ? avgLoss.divide(BigDecimal.valueOf(losers), SCALE, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Win/Loss ratio (b)
        BigDecimal b = avgWin.divide(avgLoss, SCALE, RoundingMode.HALF_UP);

        if (b.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Kelly = (p * b - q) / b = p - q/b
        BigDecimal kelly = winProb.subtract(lossProb.divide(b, SCALE, RoundingMode.HALF_UP));

        // Clamp to reasonable range [0, 1]
        if (kelly.compareTo(BigDecimal.ZERO) < 0) {
            kelly = BigDecimal.ZERO;
        } else if (kelly.compareTo(BigDecimal.ONE) > 0) {
            kelly = BigDecimal.ONE;
        }

        return kelly.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Tail Ratio.
     *
     * Tail Ratio = |95th percentile return| / |5th percentile return|
     * Higher is better - indicates larger gains than losses in extreme cases.
     */
    private BigDecimal calculateTailRatio(List<BacktestTrade> trades) {
        List<BigDecimal> returns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .sorted()
                .toList();

        if (returns.size() < 20) { // Need enough data points
            return BigDecimal.ONE;
        }

        int n = returns.size();
        int idx5 = (int) (0.05 * n);
        int idx95 = (int) (0.95 * n);

        if (idx95 >= n) idx95 = n - 1;

        BigDecimal percentile5 = returns.get(idx5).abs();
        BigDecimal percentile95 = returns.get(idx95).abs();

        if (percentile5.compareTo(BigDecimal.ZERO) == 0) {
            return percentile95.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(999.99) : BigDecimal.ONE;
        }

        return percentile95.divide(percentile5, SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Calculate Gain to Pain Ratio.
     *
     * GtP = Sum of all returns / Sum of absolute negative returns
     * Similar to Omega but simpler.
     */
    private BigDecimal calculateGainToPainRatio(List<BacktestTrade> trades) {
        List<BigDecimal> returns = trades.stream()
                .map(BacktestTrade::getReturnPct)
                .filter(r -> r != null)
                .toList();

        if (returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalReturn = returns.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPain = returns.stream()
                .filter(r -> r.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPain.compareTo(BigDecimal.ZERO) == 0) {
            return totalReturn.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(999.99) : BigDecimal.ZERO;
        }

        return totalReturn.divide(totalPain, SCALE, RoundingMode.HALF_UP)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
