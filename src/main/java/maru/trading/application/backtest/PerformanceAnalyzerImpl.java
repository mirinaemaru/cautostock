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

        return RiskMetrics.builder()
                .volatility(volatility)
                .downsideDeviation(downsideDeviation)
                .var95(var95)
                .cvar95(cvar95)
                .calmarRatio(calmarRatio)
                .recoveryFactor(recoveryFactor)
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
        // Simplified: return 0 for now
        // Full implementation would track days from peak to recovery
        return 0;
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
}
