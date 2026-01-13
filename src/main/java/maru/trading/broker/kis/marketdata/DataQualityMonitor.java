package maru.trading.broker.kis.marketdata;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Data Quality Monitor.
 *
 * Monitors quality of incoming market data per symbol.
 */
@Component
public class DataQualityMonitor {

    private static final Logger log = LoggerFactory.getLogger(DataQualityMonitor.class);

    private final Map<String, SymbolQualityMetrics> symbolMetrics = new ConcurrentHashMap<>();

    /**
     * Record a valid tick.
     */
    public void recordValidTick(String symbol) {
        SymbolQualityMetrics metrics = getOrCreateMetrics(symbol);
        metrics.validTickCount.incrementAndGet();
        metrics.lastTickTimestamp = LocalDateTime.now();
    }

    /**
     * Record an invalid tick.
     */
    public void recordInvalidTick(String symbol, String reason) {
        SymbolQualityMetrics metrics = getOrCreateMetrics(symbol);
        metrics.invalidTickCount.incrementAndGet();
        metrics.lastError = reason;
        metrics.lastErrorTimestamp = LocalDateTime.now();
    }

    /**
     * Record a duplicate tick.
     */
    public void recordDuplicateTick(String symbol) {
        SymbolQualityMetrics metrics = getOrCreateMetrics(symbol);
        metrics.duplicateTickCount.incrementAndGet();
    }

    /**
     * Record an out-of-sequence tick.
     */
    public void recordOutOfSequenceTick(String symbol) {
        SymbolQualityMetrics metrics = getOrCreateMetrics(symbol);
        metrics.outOfSequenceTickCount.incrementAndGet();
    }

    /**
     * Record an error.
     */
    public void recordError(String symbol, String errorMessage) {
        SymbolQualityMetrics metrics = getOrCreateMetrics(symbol);
        metrics.errorCount.incrementAndGet();
        metrics.lastError = errorMessage;
        metrics.lastErrorTimestamp = LocalDateTime.now();
    }

    /**
     * Get quality metrics for a symbol.
     */
    public SymbolQualityMetrics getMetrics(String symbol) {
        return symbolMetrics.get(symbol);
    }

    /**
     * Get all symbol metrics.
     */
    public Map<String, SymbolQualityMetrics> getAllMetrics() {
        return new ConcurrentHashMap<>(symbolMetrics);
    }

    /**
     * Reset metrics for a symbol.
     */
    public void resetMetrics(String symbol) {
        symbolMetrics.remove(symbol);
        log.info("Reset quality metrics for symbol: {}", symbol);
    }

    /**
     * Reset all metrics.
     */
    public void resetAllMetrics() {
        symbolMetrics.clear();
        log.info("Reset all quality metrics");
    }

    /**
     * Get or create metrics for a symbol.
     */
    private SymbolQualityMetrics getOrCreateMetrics(String symbol) {
        return symbolMetrics.computeIfAbsent(symbol, s -> new SymbolQualityMetrics(s));
    }

    /**
     * Quality metrics for a single symbol.
     */
    @Getter
    public static class SymbolQualityMetrics {
        private final String symbol;
        private final AtomicLong validTickCount = new AtomicLong(0);
        private final AtomicLong invalidTickCount = new AtomicLong(0);
        private final AtomicLong duplicateTickCount = new AtomicLong(0);
        private final AtomicLong outOfSequenceTickCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);

        private volatile LocalDateTime lastTickTimestamp;
        private volatile String lastError;
        private volatile LocalDateTime lastErrorTimestamp;

        public SymbolQualityMetrics(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Calculate data quality score (0-100).
         *
         * 100 = perfect quality
         * 0 = terrible quality
         */
        public double getQualityScore() {
            long total = validTickCount.get() + invalidTickCount.get() +
                    duplicateTickCount.get() + outOfSequenceTickCount.get();

            if (total == 0) {
                return 100.0;
            }

            double validRatio = (double) validTickCount.get() / total;
            return validRatio * 100.0;
        }

        /**
         * Check if data quality is acceptable.
         */
        public boolean isQualityAcceptable() {
            return getQualityScore() >= 95.0; // 95% threshold
        }
    }
}
