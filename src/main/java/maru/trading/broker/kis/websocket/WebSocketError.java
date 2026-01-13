package maru.trading.broker.kis.websocket;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Represents a WebSocket error.
 */
@Getter
@Builder
public class WebSocketError {

    /**
     * Error type.
     */
    private final ErrorType type;

    /**
     * Error message.
     */
    private final String message;

    /**
     * Exception (if any).
     */
    private final Throwable throwable;

    /**
     * Timestamp when error occurred.
     */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Error recovery action.
     */
    private final RecoveryAction recoveryAction;

    /**
     * WebSocket error types.
     */
    public enum ErrorType {
        /**
         * Network-related errors (connection failure, timeout).
         */
        NETWORK,

        /**
         * Authentication/authorization errors.
         */
        AUTHENTICATION,

        /**
         * Data parsing errors.
         */
        DATA_PARSING,

        /**
         * Protocol errors (invalid message format).
         */
        PROTOCOL,

        /**
         * Unknown errors.
         */
        UNKNOWN
    }

    /**
     * Recovery actions for errors.
     */
    public enum RecoveryAction {
        /**
         * Attempt to reconnect.
         */
        RECONNECT,

        /**
         * Activate Kill Switch and stop trading.
         */
        KILL_SWITCH,

        /**
         * Log error and continue.
         */
        LOG_AND_CONTINUE,

        /**
         * Send alert to administrators.
         */
        ALERT
    }
}
