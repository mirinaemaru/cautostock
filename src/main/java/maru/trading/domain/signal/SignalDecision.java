package maru.trading.domain.signal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Signal decision output from strategy evaluation.
 *
 * Represents the strategy's trading decision (BUY/SELL/HOLD)
 * with optional target quantity/weight and reasoning.
 *
 * Immutable value object.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalDecision {

    /**
     * Signal type (BUY, SELL, HOLD).
     */
    private SignalType signalType;

    /**
     * Target type: "QTY" (absolute quantity) or "WEIGHT" (portfolio percentage).
     * Null for HOLD signals.
     */
    private String targetType;

    /**
     * Target value:
     * - If targetType = "QTY": number of shares (e.g., 10)
     * - If targetType = "WEIGHT": portfolio percentage (e.g., 0.25 for 25%)
     * Null for HOLD signals.
     */
    private BigDecimal targetValue;

    /**
     * Human-readable reason for the decision (for logging/audit).
     * Example: "MA(5)=70500 crossed above MA(20)=70000"
     */
    private String reason;

    /**
     * Signal time-to-live in seconds.
     * After this duration, the signal should not be executed.
     * Null means no expiration (not recommended).
     */
    private Integer ttlSeconds;

    /**
     * Create a HOLD decision (no action).
     */
    public static SignalDecision hold(String reason) {
        return SignalDecision.builder()
                .signalType(SignalType.HOLD)
                .reason(reason)
                .build();
    }

    /**
     * Create a BUY decision with quantity target.
     */
    public static SignalDecision buy(BigDecimal quantity, String reason, Integer ttlSeconds) {
        return SignalDecision.builder()
                .signalType(SignalType.BUY)
                .targetType("QTY")
                .targetValue(quantity)
                .reason(reason)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    /**
     * Create a SELL decision with quantity target.
     */
    public static SignalDecision sell(BigDecimal quantity, String reason, Integer ttlSeconds) {
        return SignalDecision.builder()
                .signalType(SignalType.SELL)
                .targetType("QTY")
                .targetValue(quantity)
                .reason(reason)
                .ttlSeconds(ttlSeconds)
                .build();
    }

    /**
     * Validate signal decision.
     * Throws IllegalArgumentException if invalid.
     */
    public void validate() {
        if (signalType == null) {
            throw new IllegalArgumentException("Signal type cannot be null");
        }

        if (signalType != SignalType.HOLD) {
            if (targetType == null || targetType.isBlank()) {
                throw new IllegalArgumentException("Target type cannot be null for " + signalType + " signal");
            }
            if (targetValue == null || targetValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Target value must be positive for " + signalType + " signal");
            }
        }

        if (ttlSeconds != null && ttlSeconds <= 0) {
            throw new IllegalArgumentException("TTL seconds must be positive: " + ttlSeconds);
        }
    }

    /**
     * Check if this is a tradeable signal (BUY or SELL).
     */
    public boolean isTradeable() {
        return signalType == SignalType.BUY || signalType == SignalType.SELL;
    }

    @Override
    public String toString() {
        return "SignalDecision{" +
                "signalType=" + signalType +
                ", targetType='" + targetType + '\'' +
                ", targetValue=" + targetValue +
                ", reason='" + reason + '\'' +
                ", ttlSeconds=" + ttlSeconds +
                '}';
    }
}
