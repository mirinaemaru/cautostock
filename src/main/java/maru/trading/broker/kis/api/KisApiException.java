package maru.trading.broker.kis.api;

import lombok.Getter;

/**
 * KIS API Exception.
 *
 * Represents errors from KIS API calls.
 */
@Getter
public class KisApiException extends RuntimeException {

    private final ErrorType errorType;
    private final String kisErrorCode;
    private final int httpStatusCode;
    private final boolean retryable;

    public KisApiException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.kisErrorCode = null;
        this.httpStatusCode = 0;
        this.retryable = errorType.isRetryable();
    }

    public KisApiException(String message, ErrorType errorType, String kisErrorCode, int httpStatusCode) {
        super(message);
        this.errorType = errorType;
        this.kisErrorCode = kisErrorCode;
        this.httpStatusCode = httpStatusCode;
        this.retryable = errorType.isRetryable();
    }

    public KisApiException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.kisErrorCode = null;
        this.httpStatusCode = 0;
        this.retryable = errorType.isRetryable();
    }

    /**
     * API error types.
     */
    @Getter
    public enum ErrorType {
        /**
         * Network errors (connection failure, timeout).
         * Retryable.
         */
        NETWORK(true),

        /**
         * Authentication errors (invalid token, expired).
         * Not retryable (requires re-authentication).
         */
        AUTHENTICATION(false),

        /**
         * Rate limit exceeded.
         * Retryable after delay.
         */
        RATE_LIMIT(true),

        /**
         * Invalid request (bad parameters).
         * Not retryable.
         */
        INVALID_REQUEST(false),

        /**
         * Order rejected by exchange.
         * Not retryable.
         */
        ORDER_REJECTED(false),

        /**
         * Insufficient balance.
         * Not retryable.
         */
        INSUFFICIENT_BALANCE(false),

        /**
         * Server error (5xx).
         * Retryable.
         */
        SERVER_ERROR(true),

        /**
         * Unknown error.
         * Not retryable by default.
         */
        UNKNOWN(false);

        private final boolean retryable;

        ErrorType(boolean retryable) {
            this.retryable = retryable;
        }
    }

    /**
     * Classify error from HTTP status code and KIS error code.
     */
    public static ErrorType classifyError(int httpStatusCode, String kisErrorCode) {
        // Authentication errors
        if (httpStatusCode == 401 || httpStatusCode == 403) {
            return ErrorType.AUTHENTICATION;
        }

        // Rate limit
        if (httpStatusCode == 429) {
            return ErrorType.RATE_LIMIT;
        }

        // Invalid request
        if (httpStatusCode == 400) {
            return ErrorType.INVALID_REQUEST;
        }

        // Server errors
        if (httpStatusCode >= 500) {
            return ErrorType.SERVER_ERROR;
        }

        // KIS-specific error codes
        if (kisErrorCode != null) {
            if (kisErrorCode.startsWith("40")) {
                return ErrorType.INVALID_REQUEST;
            } else if (kisErrorCode.startsWith("50")) {
                return ErrorType.SERVER_ERROR;
            } else if (kisErrorCode.contains("INSUFFICIENT")) {
                return ErrorType.INSUFFICIENT_BALANCE;
            } else if (kisErrorCode.contains("REJECT")) {
                return ErrorType.ORDER_REJECTED;
            }
        }

        return ErrorType.UNKNOWN;
    }
}
