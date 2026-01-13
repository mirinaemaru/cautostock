package maru.trading.broker.kis.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * Classifies WebSocket errors and determines recovery actions.
 */
public class WebSocketErrorClassifier {

    private static final Logger log = LoggerFactory.getLogger(WebSocketErrorClassifier.class);

    /**
     * Classify error and determine recovery action.
     *
     * @param throwable The error that occurred
     * @param message Additional error message
     * @return Classified WebSocket error
     */
    public WebSocketError classify(Throwable throwable, String message) {
        log.debug("Classifying error: {}", message);

        WebSocketError.ErrorType type = determineErrorType(throwable, message);
        WebSocketError.RecoveryAction action = determineRecoveryAction(type, throwable, message);

        return WebSocketError.builder()
                .type(type)
                .message(message)
                .throwable(throwable)
                .recoveryAction(action)
                .build();
    }

    /**
     * Determine error type from exception and message.
     */
    private WebSocketError.ErrorType determineErrorType(Throwable throwable, String message) {
        if (throwable == null && message == null) {
            return WebSocketError.ErrorType.UNKNOWN;
        }

        // Network errors
        if (throwable instanceof SocketException ||
                throwable instanceof SocketTimeoutException ||
                throwable instanceof TimeoutException ||
                throwable instanceof IOException) {
            return WebSocketError.ErrorType.NETWORK;
        }

        // Authentication errors (check message)
        if (message != null &&
                (message.contains("authentication") ||
                        message.contains("unauthorized") ||
                        message.contains("approval_key") ||
                        message.contains("401") ||
                        message.contains("403"))) {
            return WebSocketError.ErrorType.AUTHENTICATION;
        }

        // Data parsing errors
        if (message != null &&
                (message.contains("parse") ||
                        message.contains("invalid JSON") ||
                        message.contains("malformed"))) {
            return WebSocketError.ErrorType.DATA_PARSING;
        }

        // Protocol errors
        if (message != null &&
                (message.contains("protocol") ||
                        message.contains("invalid message") ||
                        message.contains("handshake"))) {
            return WebSocketError.ErrorType.PROTOCOL;
        }

        return WebSocketError.ErrorType.UNKNOWN;
    }

    /**
     * Determine recovery action based on error type.
     */
    private WebSocketError.RecoveryAction determineRecoveryAction(
            WebSocketError.ErrorType type,
            Throwable throwable,
            String message) {

        switch (type) {
            case NETWORK:
                // Network errors: attempt reconnection
                return WebSocketError.RecoveryAction.RECONNECT;

            case AUTHENTICATION:
                // Authentication errors: activate Kill Switch (critical)
                log.error("Authentication error detected. Kill Switch will be activated.");
                return WebSocketError.RecoveryAction.KILL_SWITCH;

            case DATA_PARSING:
                // Data parsing errors: log and continue (non-critical)
                return WebSocketError.RecoveryAction.LOG_AND_CONTINUE;

            case PROTOCOL:
                // Protocol errors: reconnect (may be transient)
                return WebSocketError.RecoveryAction.RECONNECT;

            case UNKNOWN:
            default:
                // Unknown errors: send alert for investigation
                return WebSocketError.RecoveryAction.ALERT;
        }
    }

    /**
     * Check if error is critical and requires immediate action.
     *
     * @param error The WebSocket error
     * @return true if error is critical
     */
    public boolean isCritical(WebSocketError error) {
        return error.getRecoveryAction() == WebSocketError.RecoveryAction.KILL_SWITCH ||
                error.getType() == WebSocketError.ErrorType.AUTHENTICATION;
    }

    /**
     * Check if error is recoverable by reconnection.
     *
     * @param error The WebSocket error
     * @return true if recoverable
     */
    public boolean isRecoverable(WebSocketError error) {
        return error.getRecoveryAction() == WebSocketError.RecoveryAction.RECONNECT;
    }
}
