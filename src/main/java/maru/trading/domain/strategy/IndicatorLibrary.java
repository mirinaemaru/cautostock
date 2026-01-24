package maru.trading.domain.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Technical indicator calculation library.
 *
 * Pure functions for calculating common technical indicators:
 * - MA (Simple Moving Average)
 * - EMA (Exponential Moving Average)
 * - RSI (Relative Strength Index)
 *
 * All methods are static and thread-safe.
 */
public class IndicatorLibrary {

    private static final int SCALE = 8; // Precision for intermediate calculations
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Calculate Simple Moving Average (MA).
     *
     * @param prices List of prices (must have at least 'period' elements)
     * @param period MA period (e.g., 5, 20, 200)
     * @return List of MA values (size = prices.size() - period + 1)
     * @throws IllegalArgumentException if period is invalid or insufficient data
     */
    public static List<BigDecimal> calculateMA(List<BigDecimal> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices cannot be null or empty");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive: " + period);
        }
        if (prices.size() < period) {
            throw new IllegalArgumentException(
                    "Insufficient data for MA calculation: need " + period + " prices, got " + prices.size());
        }

        List<BigDecimal> maValues = new ArrayList<>();

        for (int i = period - 1; i < prices.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(prices.get(j));
            }
            BigDecimal ma = sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING_MODE);
            maValues.add(ma);
        }

        return maValues;
    }

    /**
     * Calculate Exponential Moving Average (EMA).
     *
     * EMA = Price(t) * k + EMA(y) * (1 - k)
     * where k = 2 / (period + 1)
     *
     * @param prices List of prices (must have at least 'period' elements)
     * @param period EMA period (e.g., 12, 26)
     * @return List of EMA values (size = prices.size() - period + 1)
     * @throws IllegalArgumentException if period is invalid or insufficient data
     */
    public static List<BigDecimal> calculateEMA(List<BigDecimal> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices cannot be null or empty");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive: " + period);
        }
        if (prices.size() < period) {
            throw new IllegalArgumentException(
                    "Insufficient data for EMA calculation: need " + period + " prices, got " + prices.size());
        }

        List<BigDecimal> emaValues = new ArrayList<>();

        // Calculate smoothing factor k = 2 / (period + 1)
        BigDecimal k = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1), SCALE, ROUNDING_MODE);

        // First EMA = SMA of first 'period' prices
        BigDecimal firstEma = calculateFirstSMA(prices, period);
        emaValues.add(firstEma);

        // Calculate subsequent EMAs
        BigDecimal previousEma = firstEma;
        for (int i = period; i < prices.size(); i++) {
            BigDecimal price = prices.get(i);
            BigDecimal ema = price.multiply(k)
                    .add(previousEma.multiply(BigDecimal.ONE.subtract(k)));
            emaValues.add(ema);
            previousEma = ema;
        }

        return emaValues;
    }

    /**
     * Calculate Relative Strength Index (RSI).
     *
     * RSI = 100 - (100 / (1 + RS))
     * where RS = Average Gain / Average Loss over period
     *
     * @param prices List of prices (must have at least 'period + 1' elements)
     * @param period RSI period (typically 14)
     * @return List of RSI values (size = prices.size() - period)
     * @throws IllegalArgumentException if period is invalid or insufficient data
     */
    public static List<BigDecimal> calculateRSI(List<BigDecimal> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices cannot be null or empty");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive: " + period);
        }
        if (prices.size() < period + 1) {
            throw new IllegalArgumentException(
                    "Insufficient data for RSI calculation: need " + (period + 1) + " prices, got " + prices.size());
        }

        List<BigDecimal> rsiValues = new ArrayList<>();

        // Calculate price changes
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();

        for (int i = 1; i < prices.size(); i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i - 1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }

        // Calculate first average gain/loss (SMA)
        BigDecimal avgGain = calculateAverage(gains.subList(0, period));
        BigDecimal avgLoss = calculateAverage(losses.subList(0, period));

        // Calculate first RSI
        BigDecimal rsi = calculateRSIValue(avgGain, avgLoss);
        rsiValues.add(rsi);

        // Calculate subsequent RSIs using smoothed averages
        for (int i = period; i < gains.size(); i++) {
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gains.get(i))
                    .divide(BigDecimal.valueOf(period), SCALE, ROUNDING_MODE);

            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(losses.get(i))
                    .divide(BigDecimal.valueOf(period), SCALE, ROUNDING_MODE);

            rsi = calculateRSIValue(avgGain, avgLoss);
            rsiValues.add(rsi);
        }

        return rsiValues;
    }

    // Helper methods

    private static BigDecimal calculateFirstSMA(List<BigDecimal> prices, int period) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING_MODE);
    }

    private static BigDecimal calculateAverage(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING_MODE);
    }

    private static BigDecimal calculateRSIValue(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100); // All gains, no losses
        }

        BigDecimal rs = avgGain.divide(avgLoss, SCALE, ROUNDING_MODE);
        BigDecimal rsi = BigDecimal.valueOf(100)
                .subtract(BigDecimal.valueOf(100)
                        .divide(BigDecimal.ONE.add(rs), SCALE, ROUNDING_MODE));

        return rsi;
    }

    /**
     * Calculate Bollinger Bands.
     *
     * Bollinger Bands consist of:
     * - Middle Band: SMA(period)
     * - Upper Band: Middle Band + (stdDev * multiplier)
     * - Lower Band: Middle Band - (stdDev * multiplier)
     *
     * @param prices List of prices (must have at least 'period' elements)
     * @param period Bollinger Bands period (typically 20)
     * @param stdDevMultiplier Standard deviation multiplier (typically 2.0)
     * @return BollingerBands object containing upper, middle, lower bands
     * @throws IllegalArgumentException if period is invalid or insufficient data
     */
    public static List<BollingerBands> calculateBollingerBands(List<BigDecimal> prices, int period, double stdDevMultiplier) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices cannot be null or empty");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Period must be positive: " + period);
        }
        if (prices.size() < period) {
            throw new IllegalArgumentException(
                    "Insufficient data for Bollinger Bands calculation: need " + period + " prices, got " + prices.size());
        }
        if (stdDevMultiplier <= 0) {
            throw new IllegalArgumentException("Standard deviation multiplier must be positive: " + stdDevMultiplier);
        }

        List<BollingerBands> bbList = new ArrayList<>();
        BigDecimal multiplier = BigDecimal.valueOf(stdDevMultiplier);

        for (int i = period - 1; i < prices.size(); i++) {
            // Calculate middle band (SMA)
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(prices.get(j));
            }
            BigDecimal middleBand = sum.divide(BigDecimal.valueOf(period), SCALE, ROUNDING_MODE);

            // Calculate standard deviation
            BigDecimal variance = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                BigDecimal diff = prices.get(j).subtract(middleBand);
                variance = variance.add(diff.multiply(diff));
            }
            variance = variance.divide(BigDecimal.valueOf(period), SCALE, ROUNDING_MODE);
            BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

            // Calculate upper and lower bands
            BigDecimal upperBand = middleBand.add(stdDev.multiply(multiplier));
            BigDecimal lowerBand = middleBand.subtract(stdDev.multiply(multiplier));

            bbList.add(new BollingerBands(upperBand, middleBand, lowerBand));
        }

        return bbList;
    }

    /**
     * Calculate MACD (Moving Average Convergence Divergence).
     *
     * MACD = EMA(fast) - EMA(slow)
     * Signal = EMA(MACD, signalPeriod)
     * Histogram = MACD - Signal
     *
     * @param prices List of prices
     * @param fastPeriod Fast EMA period (typically 12)
     * @param slowPeriod Slow EMA period (typically 26)
     * @param signalPeriod Signal line EMA period (typically 9)
     * @return MACD object containing MACD line, signal line, histogram
     * @throws IllegalArgumentException if periods are invalid or insufficient data
     */
    public static List<MACD> calculateMACD(List<BigDecimal> prices, int fastPeriod, int slowPeriod, int signalPeriod) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices cannot be null or empty");
        }
        if (fastPeriod <= 0 || slowPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException("All periods must be positive");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("Fast period must be less than slow period");
        }
        int minDataPoints = slowPeriod + signalPeriod - 1;
        if (prices.size() < minDataPoints) {
            throw new IllegalArgumentException(
                    "Insufficient data for MACD calculation: need " + minDataPoints + " prices, got " + prices.size());
        }

        // Calculate fast and slow EMAs
        List<BigDecimal> fastEMA = calculateEMA(prices, fastPeriod);
        List<BigDecimal> slowEMA = calculateEMA(prices, slowPeriod);

        // Calculate MACD line (fast EMA - slow EMA)
        // Align arrays: slowEMA is shorter, so we need to skip initial fastEMA values
        int offset = fastPeriod - slowPeriod; // This will be negative since slow > fast
        List<BigDecimal> macdLine = new ArrayList<>();

        for (int i = 0; i < slowEMA.size(); i++) {
            // fastEMA starts at index (slowPeriod - fastPeriod) to align with slowEMA
            int fastIndex = i + (slowPeriod - fastPeriod);
            BigDecimal macd = fastEMA.get(fastIndex).subtract(slowEMA.get(i));
            macdLine.add(macd);
        }

        // Calculate signal line (EMA of MACD)
        List<BigDecimal> signalLine = calculateEMA(macdLine, signalPeriod);

        // Calculate histogram (MACD - Signal)
        List<MACD> macdList = new ArrayList<>();
        int signalOffset = macdLine.size() - signalLine.size();

        for (int i = 0; i < signalLine.size(); i++) {
            BigDecimal macd = macdLine.get(i + signalOffset);
            BigDecimal signal = signalLine.get(i);
            BigDecimal histogram = macd.subtract(signal);
            macdList.add(new MACD(macd, signal, histogram));
        }

        return macdList;
    }

    /**
     * Bollinger Bands data structure.
     */
    public static class BollingerBands {
        private final BigDecimal upper;
        private final BigDecimal middle;
        private final BigDecimal lower;

        public BollingerBands(BigDecimal upper, BigDecimal middle, BigDecimal lower) {
            this.upper = upper;
            this.middle = middle;
            this.lower = lower;
        }

        public BigDecimal getUpper() {
            return upper;
        }

        public BigDecimal getMiddle() {
            return middle;
        }

        public BigDecimal getLower() {
            return lower;
        }
    }

    /**
     * MACD data structure.
     */
    public static class MACD {
        private final BigDecimal macdLine;
        private final BigDecimal signalLine;
        private final BigDecimal histogram;

        public MACD(BigDecimal macdLine, BigDecimal signalLine, BigDecimal histogram) {
            this.macdLine = macdLine;
            this.signalLine = signalLine;
            this.histogram = histogram;
        }

        public BigDecimal getMacdLine() {
            return macdLine;
        }

        public BigDecimal getSignalLine() {
            return signalLine;
        }

        public BigDecimal getHistogram() {
            return histogram;
        }
    }

    // ==================== VWAP (Volume Weighted Average Price) ====================

    /**
     * VWAP calculation result.
     */
    public static class VWAPResult {
        private final BigDecimal vwap;
        private final BigDecimal cumulativeTPV; // Typical Price × Volume cumulative
        private final BigDecimal cumulativeVolume;

        public VWAPResult(BigDecimal vwap, BigDecimal cumulativeTPV, BigDecimal cumulativeVolume) {
            this.vwap = vwap;
            this.cumulativeTPV = cumulativeTPV;
            this.cumulativeVolume = cumulativeVolume;
        }

        public BigDecimal getVwap() {
            return vwap;
        }

        public BigDecimal getCumulativeTPV() {
            return cumulativeTPV;
        }

        public BigDecimal getCumulativeVolume() {
            return cumulativeVolume;
        }
    }

    /**
     * Calculate VWAP (Volume Weighted Average Price).
     *
     * VWAP = Σ(Typical Price × Volume) / Σ(Volume)
     * where Typical Price = (High + Low + Close) / 3
     *
     * @param highs List of high prices
     * @param lows List of low prices
     * @param closes List of close prices
     * @param volumes List of volumes
     * @return List of VWAP results (size = input size)
     * @throws IllegalArgumentException if inputs are invalid or sizes mismatch
     */
    public static List<VWAPResult> calculateVWAP(
            List<BigDecimal> highs,
            List<BigDecimal> lows,
            List<BigDecimal> closes,
            List<Long> volumes) {

        if (highs == null || lows == null || closes == null || volumes == null) {
            throw new IllegalArgumentException("All price and volume lists cannot be null");
        }
        if (highs.isEmpty() || lows.isEmpty() || closes.isEmpty() || volumes.isEmpty()) {
            throw new IllegalArgumentException("All price and volume lists cannot be empty");
        }
        if (highs.size() != lows.size() || lows.size() != closes.size() || closes.size() != volumes.size()) {
            throw new IllegalArgumentException("All lists must have the same size");
        }

        List<VWAPResult> results = new ArrayList<>();
        BigDecimal cumulativeTPV = BigDecimal.ZERO;
        BigDecimal cumulativeVolume = BigDecimal.ZERO;

        for (int i = 0; i < highs.size(); i++) {
            // Typical Price = (High + Low + Close) / 3
            BigDecimal typicalPrice = highs.get(i)
                    .add(lows.get(i))
                    .add(closes.get(i))
                    .divide(BigDecimal.valueOf(3), SCALE, ROUNDING_MODE);

            BigDecimal volume = BigDecimal.valueOf(volumes.get(i));
            BigDecimal tpv = typicalPrice.multiply(volume);

            cumulativeTPV = cumulativeTPV.add(tpv);
            cumulativeVolume = cumulativeVolume.add(volume);

            BigDecimal vwap;
            if (cumulativeVolume.compareTo(BigDecimal.ZERO) == 0) {
                vwap = typicalPrice; // Fallback to typical price if no volume
            } else {
                vwap = cumulativeTPV.divide(cumulativeVolume, SCALE, ROUNDING_MODE);
            }

            results.add(new VWAPResult(vwap, cumulativeTPV, cumulativeVolume));
        }

        return results;
    }

    // ==================== Volatility Breakout ====================

    /**
     * Calculate price range (High - Low).
     *
     * @param high High price
     * @param low Low price
     * @return Range (High - Low)
     * @throws IllegalArgumentException if inputs are invalid
     */
    public static BigDecimal calculateRange(BigDecimal high, BigDecimal low) {
        if (high == null || low == null) {
            throw new IllegalArgumentException("High and low cannot be null");
        }
        if (high.compareTo(low) < 0) {
            throw new IllegalArgumentException("High must be >= low: high=" + high + ", low=" + low);
        }
        return high.subtract(low);
    }

    /**
     * Calculate volatility breakout target price.
     *
     * Target = Today's Open + (Yesterday's Range × K)
     *
     * @param todayOpen Today's opening price
     * @param yesterdayHigh Yesterday's high price
     * @param yesterdayLow Yesterday's low price
     * @param k K factor (0.0 to 1.0, typically 0.5)
     * @return Breakout target price
     * @throws IllegalArgumentException if inputs are invalid
     */
    public static BigDecimal calculateBreakoutTarget(
            BigDecimal todayOpen,
            BigDecimal yesterdayHigh,
            BigDecimal yesterdayLow,
            double k) {

        if (todayOpen == null) {
            throw new IllegalArgumentException("Today's open cannot be null");
        }
        if (k < 0 || k > 1) {
            throw new IllegalArgumentException("K factor must be between 0 and 1: " + k);
        }

        BigDecimal range = calculateRange(yesterdayHigh, yesterdayLow);
        BigDecimal kFactor = BigDecimal.valueOf(k);
        BigDecimal rangeContribution = range.multiply(kFactor);

        return todayOpen.add(rangeContribution).setScale(SCALE, ROUNDING_MODE);
    }

    // ==================== Spread / Z-Score (for Mean Reversion) ====================

    /**
     * Spread calculation result for mean reversion strategy.
     */
    public static class SpreadResult {
        private final BigDecimal spread;      // Current spread (price - mean)
        private final BigDecimal meanSpread;  // Mean of lookback period (moving average)
        private final BigDecimal stdDev;      // Standard deviation
        private final BigDecimal zScore;      // Z-Score = (price - mean) / stdDev

        public SpreadResult(BigDecimal spread, BigDecimal meanSpread, BigDecimal stdDev, BigDecimal zScore) {
            this.spread = spread;
            this.meanSpread = meanSpread;
            this.stdDev = stdDev;
            this.zScore = zScore;
        }

        public BigDecimal getSpread() {
            return spread;
        }

        public BigDecimal getMeanSpread() {
            return meanSpread;
        }

        public BigDecimal getStdDev() {
            return stdDev;
        }

        public BigDecimal getZScore() {
            return zScore;
        }
    }

    /**
     * Calculate standard deviation.
     *
     * @param values List of values
     * @return Standard deviation
     * @throws IllegalArgumentException if values is null or empty
     */
    public static BigDecimal calculateStdDev(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }
        if (values.size() == 1) {
            return BigDecimal.ZERO;
        }

        // Calculate mean
        BigDecimal mean = calculateAverage(values);

        // Calculate variance
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            BigDecimal diff = value.subtract(mean);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING_MODE);

        // Standard deviation = sqrt(variance)
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(SCALE, ROUNDING_MODE);
    }

    /**
     * Calculate simple spread (price vs moving average) and Z-Score.
     *
     * Used for single-asset mean reversion strategy.
     * Spread = Current Price - Moving Average
     * Z-Score = Spread / Standard Deviation
     *
     * @param prices List of prices (must have at least 'lookbackPeriod' elements)
     * @param lookbackPeriod Period for mean and std dev calculation
     * @return SpreadResult with spread, mean, stdDev, and zScore
     * @throws IllegalArgumentException if insufficient data
     */
    public static SpreadResult calculateSimpleSpread(List<BigDecimal> prices, int lookbackPeriod) {
        if (prices == null || prices.isEmpty()) {
            throw new IllegalArgumentException("Prices cannot be null or empty");
        }
        if (lookbackPeriod <= 0) {
            throw new IllegalArgumentException("Lookback period must be positive: " + lookbackPeriod);
        }
        if (prices.size() < lookbackPeriod) {
            throw new IllegalArgumentException(
                    "Insufficient data for spread calculation: need " + lookbackPeriod + " prices, got " + prices.size());
        }

        // Get the last lookbackPeriod prices for calculation
        List<BigDecimal> lookbackPrices = prices.subList(prices.size() - lookbackPeriod, prices.size());

        // Current price is the last price
        BigDecimal currentPrice = prices.get(prices.size() - 1);

        // Calculate mean (moving average)
        BigDecimal mean = calculateAverage(lookbackPrices);

        // Calculate standard deviation
        BigDecimal stdDev = calculateStdDev(lookbackPrices);

        // Spread = Current Price - Mean
        BigDecimal spread = currentPrice.subtract(mean);

        // Z-Score = Spread / StdDev (if stdDev is 0, Z-Score is 0)
        BigDecimal zScore;
        if (stdDev.compareTo(BigDecimal.ZERO) == 0) {
            zScore = BigDecimal.ZERO;
        } else {
            zScore = spread.divide(stdDev, SCALE, ROUNDING_MODE);
        }

        return new SpreadResult(spread, mean, stdDev, zScore);
    }
}
