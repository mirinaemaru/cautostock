package maru.trading.broker.kis.fill;

import maru.trading.domain.execution.Fill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Duplicate Fill Filter.
 *
 * Detects and filters duplicate fill notifications from WebSocket.
 * Uses in-memory cache with automatic cleanup of old entries.
 */
@Component
public class DuplicateFillFilter {

    private static final Logger log = LoggerFactory.getLogger(DuplicateFillFilter.class);

    /**
     * Cache of recently processed fill IDs.
     * Key: fillId
     * Value: timestamp when fill was first seen
     */
    private final Map<String, LocalDateTime> processedFills = new ConcurrentHashMap<>();

    /**
     * Maximum age for cached fill IDs (1 hour).
     * After this time, entries are eligible for cleanup.
     */
    private static final long MAX_CACHE_AGE_MINUTES = 60;

    /**
     * Maximum cache size before triggering cleanup.
     */
    private static final int MAX_CACHE_SIZE = 10000;

    /**
     * Check if a fill is a duplicate.
     *
     * @param fill Fill to check
     * @return true if duplicate, false if first time seeing this fill
     */
    public boolean isDuplicate(Fill fill) {
        if (fill == null || fill.getFillId() == null) {
            log.warn("Cannot check duplicate for null fill or fillId");
            return false;
        }

        String fillId = fill.getFillId();
        LocalDateTime now = LocalDateTime.now();

        // Try to add fill to cache
        LocalDateTime previousTimestamp = processedFills.putIfAbsent(fillId, now);

        if (previousTimestamp != null) {
            // Fill was already in cache - this is a duplicate
            log.debug("Duplicate fill detected: fillId={}, first seen at {}, duplicate at {}",
                    fillId, previousTimestamp, now);
            return true;
        }

        // First time seeing this fill - not a duplicate
        log.debug("New fill recorded: fillId={}", fillId);

        // Trigger cleanup if cache is getting too large
        if (processedFills.size() > MAX_CACHE_SIZE) {
            cleanupOldEntries();
        }

        return false;
    }

    /**
     * Clean up old entries from cache.
     * Removes fills older than MAX_CACHE_AGE_MINUTES.
     */
    private void cleanupOldEntries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(MAX_CACHE_AGE_MINUTES);
        int initialSize = processedFills.size();

        Set<String> keysToRemove = ConcurrentHashMap.newKeySet();

        processedFills.forEach((fillId, timestamp) -> {
            if (timestamp.isBefore(cutoffTime)) {
                keysToRemove.add(fillId);
            }
        });

        keysToRemove.forEach(processedFills::remove);

        int removedCount = keysToRemove.size();
        if (removedCount > 0) {
            log.info("Cleaned up {} old fill entries from cache (before: {}, after: {})",
                    removedCount, initialSize, processedFills.size());
        }
    }

    /**
     * Manually mark a fill as processed.
     * Useful for initialization or recovery scenarios.
     *
     * @param fillId Fill ID to mark as processed
     */
    public void markAsProcessed(String fillId) {
        if (fillId != null) {
            processedFills.putIfAbsent(fillId, LocalDateTime.now());
            log.debug("Manually marked fill as processed: fillId={}", fillId);
        }
    }

    /**
     * Check if a fill ID is in the cache.
     *
     * @param fillId Fill ID to check
     * @return true if in cache, false otherwise
     */
    public boolean isInCache(String fillId) {
        return fillId != null && processedFills.containsKey(fillId);
    }

    /**
     * Get cache size.
     *
     * @return Number of fill IDs currently in cache
     */
    public int getCacheSize() {
        return processedFills.size();
    }

    /**
     * Clear all cached fill IDs.
     * WARNING: Only use for testing or recovery scenarios.
     */
    public void clearCache() {
        int previousSize = processedFills.size();
        processedFills.clear();
        log.warn("Cleared all {} fill IDs from duplicate filter cache", previousSize);
    }

    /**
     * Get cache statistics.
     *
     * @return Cache statistics string
     */
    public String getCacheStats() {
        return String.format("DuplicateFillFilter cache: size=%d, max=%d",
                processedFills.size(), MAX_CACHE_SIZE);
    }
}
