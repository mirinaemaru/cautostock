package maru.trading.broker.kis.websocket;

import lombok.Builder;
import lombok.Getter;

/**
 * WebSocket reconnection policy.
 *
 * Defines the strategy for reconnecting after connection failure.
 */
@Getter
@Builder
public class ReconnectionPolicy {

    /**
     * Maximum number of reconnection attempts.
     */
    @Builder.Default
    private final int maxRetries = 10;

    /**
     * Initial delay before first reconnection attempt (milliseconds).
     */
    @Builder.Default
    private final long initialDelayMs = 1000;

    /**
     * Maximum delay between reconnection attempts (milliseconds).
     */
    @Builder.Default
    private final long maxDelayMs = 60000;

    /**
     * Backoff multiplier for exponential backoff.
     */
    @Builder.Default
    private final double backoffMultiplier = 2.0;

    /**
     * Calculate delay for given attempt number using exponential backoff.
     *
     * @param attemptNumber The attempt number (0-based)
     * @return Delay in milliseconds
     */
    public long calculateDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("Attempt number must be non-negative");
        }

        // Exponential backoff: initialDelay * (multiplier ^ attemptNumber)
        double delay = initialDelayMs * Math.pow(backoffMultiplier, attemptNumber);

        // Cap at maxDelay
        return Math.min((long) delay, maxDelayMs);
    }

    /**
     * Check if retry should be attempted.
     *
     * @param attemptNumber Current attempt number (0-based)
     * @return true if retry should be attempted
     */
    public boolean shouldRetry(int attemptNumber) {
        return attemptNumber < maxRetries;
    }
}
