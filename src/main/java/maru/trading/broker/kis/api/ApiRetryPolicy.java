package maru.trading.broker.kis.api;

import lombok.Builder;
import lombok.Getter;

/**
 * API retry policy for KIS API calls.
 */
@Getter
@Builder
public class ApiRetryPolicy {

    /**
     * Maximum retry attempts.
     */
    @Builder.Default
    private final int maxRetries = 3;

    /**
     * Initial delay before first retry (milliseconds).
     */
    @Builder.Default
    private final long initialDelayMs = 1000;

    /**
     * Maximum delay between retries (milliseconds).
     */
    @Builder.Default
    private final long maxDelayMs = 10000;

    /**
     * Backoff multiplier.
     */
    @Builder.Default
    private final double backoffMultiplier = 2.0;

    /**
     * Calculate delay for given retry attempt.
     *
     * @param attemptNumber Retry attempt number (0-based)
     * @return Delay in milliseconds
     */
    public long calculateDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            return 0;
        }

        double delay = initialDelayMs * Math.pow(backoffMultiplier, attemptNumber);
        return Math.min((long) delay, maxDelayMs);
    }

    /**
     * Check if retry should be attempted.
     *
     * @param attemptNumber Current attempt number (0-based)
     * @return true if should retry
     */
    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber < maxRetries;
    }

    /**
     * Default policy for order operations.
     */
    public static ApiRetryPolicy defaultOrderPolicy() {
        return ApiRetryPolicy.builder()
                .maxRetries(3)
                .initialDelayMs(1000)
                .maxDelayMs(10000)
                .backoffMultiplier(2.0)
                .build();
    }

    /**
     * Default policy for query operations.
     */
    public static ApiRetryPolicy defaultQueryPolicy() {
        return ApiRetryPolicy.builder()
                .maxRetries(5)
                .initialDelayMs(500)
                .maxDelayMs(5000)
                .backoffMultiplier(1.5)
                .build();
    }
}
