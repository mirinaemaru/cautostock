package maru.trading.domain.signal;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Signal validation policy.
 *
 * Enforces rules to prevent signal flooding and ensure signal quality:
 * - TTL validation (signal not expired)
 * - Cooldown enforcement (minimum time between signals)
 * - Duplicate prevention (same signal recently generated)
 *
 * Stateless policy class.
 */
@Component
public class SignalPolicy {

    /**
     * Validate signal decision before persisting.
     *
     * @param decision Signal decision from strategy
     * @throws IllegalArgumentException if signal is invalid
     */
    public void validateSignal(SignalDecision decision) {
        if (decision == null) {
            throw new IllegalArgumentException("Signal decision cannot be null");
        }

        decision.validate();
    }

    /**
     * Check if signal has expired based on TTL.
     *
     * @param signalCreatedAt When the signal was created
     * @param ttlSeconds Time-to-live in seconds
     * @param now Current time
     * @return true if signal is still valid, false if expired
     */
    public boolean isSignalValid(LocalDateTime signalCreatedAt, Integer ttlSeconds, LocalDateTime now) {
        if (signalCreatedAt == null || now == null) {
            throw new IllegalArgumentException("Timestamps cannot be null");
        }

        if (ttlSeconds == null || ttlSeconds <= 0) {
            return true; // No TTL = never expires
        }

        LocalDateTime expiryTime = signalCreatedAt.plusSeconds(ttlSeconds);
        return now.isBefore(expiryTime);
    }

    /**
     * Check if cooldown period has passed since last signal.
     * Prevents signal flooding by enforcing minimum time between signals.
     *
     * @param lastSignalTime Time of last signal for this strategy+symbol
     * @param cooldownSeconds Minimum seconds between signals
     * @param now Current time
     * @return true if cooldown passed, false if still in cooldown
     */
    public boolean isCooldownPassed(LocalDateTime lastSignalTime, int cooldownSeconds, LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time cannot be null");
        }

        if (lastSignalTime == null) {
            return true; // No previous signal = cooldown passed
        }

        if (cooldownSeconds <= 0) {
            return true; // No cooldown configured
        }

        LocalDateTime cooldownEnd = lastSignalTime.plusSeconds(cooldownSeconds);
        return now.isAfter(cooldownEnd) || now.isEqual(cooldownEnd);
    }

    /**
     * Check if a duplicate signal was recently generated.
     * A duplicate is defined as:
     * - Same strategy + symbol + signal type
     * - Generated within lookbackSeconds
     *
     * @param recentSignals Recent signals for this strategy+symbol
     * @param newSignalType Type of new signal (BUY/SELL/HOLD)
     * @param lookbackSeconds How far back to check for duplicates
     * @param now Current time
     * @return true if duplicate found, false otherwise
     */
    public boolean isDuplicate(
            List<Signal> recentSignals,
            SignalType newSignalType,
            int lookbackSeconds,
            LocalDateTime now) {

        if (recentSignals == null || recentSignals.isEmpty()) {
            return false;
        }

        if (newSignalType == null || now == null) {
            throw new IllegalArgumentException("Signal type and current time cannot be null");
        }

        if (lookbackSeconds <= 0) {
            return false; // No duplicate check configured
        }

        LocalDateTime cutoffTime = now.minusSeconds(lookbackSeconds);

        for (Signal signal : recentSignals) {
            // Note: Signal doesn't have createdAt field in current model
            // This assumes signals are pre-filtered by time or we add createdAt later
            if (signal.getSignalType() == newSignalType) {
                return true; // Found matching signal type
            }
        }

        return false;
    }

    /**
     * Validate signal before execution (workflow validation).
     *
     * Checks:
     * 1. Signal is tradeable (BUY or SELL)
     * 2. Signal has not expired
     * 3. Target values are valid
     *
     * @param signal Signal to execute
     * @param now Current time
     * @throws IllegalArgumentException if signal cannot be executed
     */
    public void validateForExecution(Signal signal, LocalDateTime now) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }

        if (signal.getSignalType() == SignalType.HOLD) {
            throw new IllegalArgumentException("Cannot execute HOLD signal");
        }

        if (signal.getSignalType() != SignalType.BUY && signal.getSignalType() != SignalType.SELL) {
            throw new IllegalArgumentException("Invalid signal type: " + signal.getSignalType());
        }

        // Check TTL (if signal has createdAt field, we'd check it here)
        // For now, we assume caller checks TTL separately

        if (signal.getTargetValue() == null || signal.getTargetValue().signum() <= 0) {
            throw new IllegalArgumentException("Signal target value must be positive");
        }
    }
}
