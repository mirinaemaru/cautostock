package maru.trading.broker.kis.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KisApiException 테스트
 *
 * 테스트 범위:
 * 1. ErrorType별 재시도 가능 여부
 * 2. HTTP 상태 코드 기반 에러 분류
 * 3. KIS 에러 코드 기반 에러 분류
 * 4. 예외 생성 및 속성 확인
 */
@DisplayName("KisApiException 테스트")
class KisApiExceptionTest {

    // ==================== 1. ErrorType별 재시도 가능 여부 ====================

    @Test
    @DisplayName("ErrorType - NETWORK는 재시도 가능")
    void testErrorType_NetworkIsRetryable() {
        assertThat(KisApiException.ErrorType.NETWORK.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("ErrorType - AUTHENTICATION은 재시도 불가")
    void testErrorType_AuthenticationNotRetryable() {
        assertThat(KisApiException.ErrorType.AUTHENTICATION.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("ErrorType - RATE_LIMIT은 재시도 가능")
    void testErrorType_RateLimitIsRetryable() {
        assertThat(KisApiException.ErrorType.RATE_LIMIT.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("ErrorType - INVALID_REQUEST는 재시도 불가")
    void testErrorType_InvalidRequestNotRetryable() {
        assertThat(KisApiException.ErrorType.INVALID_REQUEST.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("ErrorType - ORDER_REJECTED는 재시도 불가")
    void testErrorType_OrderRejectedNotRetryable() {
        assertThat(KisApiException.ErrorType.ORDER_REJECTED.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("ErrorType - INSUFFICIENT_BALANCE는 재시도 불가")
    void testErrorType_InsufficientBalanceNotRetryable() {
        assertThat(KisApiException.ErrorType.INSUFFICIENT_BALANCE.isRetryable()).isFalse();
    }

    @Test
    @DisplayName("ErrorType - SERVER_ERROR는 재시도 가능")
    void testErrorType_ServerErrorIsRetryable() {
        assertThat(KisApiException.ErrorType.SERVER_ERROR.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("ErrorType - UNKNOWN은 재시도 불가")
    void testErrorType_UnknownNotRetryable() {
        assertThat(KisApiException.ErrorType.UNKNOWN.isRetryable()).isFalse();
    }

    // ==================== 2. HTTP 상태 코드 기반 에러 분류 ====================

    @Test
    @DisplayName("classifyError - 401 Unauthorized → AUTHENTICATION")
    void testClassifyError_401() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(401, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.AUTHENTICATION);
    }

    @Test
    @DisplayName("classifyError - 403 Forbidden → AUTHENTICATION")
    void testClassifyError_403() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(403, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.AUTHENTICATION);
    }

    @Test
    @DisplayName("classifyError - 429 Too Many Requests → RATE_LIMIT")
    void testClassifyError_429() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(429, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.RATE_LIMIT);
    }

    @Test
    @DisplayName("classifyError - 400 Bad Request → INVALID_REQUEST")
    void testClassifyError_400() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(400, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.INVALID_REQUEST);
    }

    @Test
    @DisplayName("classifyError - 500 Internal Server Error → SERVER_ERROR")
    void testClassifyError_500() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(500, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.SERVER_ERROR);
    }

    @Test
    @DisplayName("classifyError - 502 Bad Gateway → SERVER_ERROR")
    void testClassifyError_502() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(502, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.SERVER_ERROR);
    }

    @Test
    @DisplayName("classifyError - 503 Service Unavailable → SERVER_ERROR")
    void testClassifyError_503() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(503, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.SERVER_ERROR);
    }

    // ==================== 3. KIS 에러 코드 기반 에러 분류 ====================

    @Test
    @DisplayName("classifyError - KIS 40xxx 에러 → INVALID_REQUEST")
    void testClassifyError_KisCode40xxx() {
        // When
        KisApiException.ErrorType type1 = KisApiException.classifyError(200, "40001");
        KisApiException.ErrorType type2 = KisApiException.classifyError(200, "40050");

        // Then
        assertThat(type1).isEqualTo(KisApiException.ErrorType.INVALID_REQUEST);
        assertThat(type2).isEqualTo(KisApiException.ErrorType.INVALID_REQUEST);
    }

    @Test
    @DisplayName("classifyError - KIS 50xxx 에러 → SERVER_ERROR")
    void testClassifyError_KisCode50xxx() {
        // When
        KisApiException.ErrorType type1 = KisApiException.classifyError(200, "50001");
        KisApiException.ErrorType type2 = KisApiException.classifyError(200, "50100");

        // Then
        assertThat(type1).isEqualTo(KisApiException.ErrorType.SERVER_ERROR);
        assertThat(type2).isEqualTo(KisApiException.ErrorType.SERVER_ERROR);
    }

    @Test
    @DisplayName("classifyError - KIS INSUFFICIENT 에러 → INSUFFICIENT_BALANCE")
    void testClassifyError_KisCodeInsufficientBalance() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(200, "INSUFFICIENT_BALANCE");

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.INSUFFICIENT_BALANCE);
    }

    @Test
    @DisplayName("classifyError - KIS REJECT 에러 → ORDER_REJECTED")
    void testClassifyError_KisCodeReject() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(200, "ORDER_REJECT");

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.ORDER_REJECTED);
    }

    @Test
    @DisplayName("classifyError - 알 수 없는 에러 → UNKNOWN")
    void testClassifyError_Unknown() {
        // When
        KisApiException.ErrorType type = KisApiException.classifyError(200, null);

        // Then
        assertThat(type).isEqualTo(KisApiException.ErrorType.UNKNOWN);
    }

    // ==================== 4. 예외 생성 및 속성 확인 ====================

    @Test
    @DisplayName("생성자 - errorType만 사용")
    void testConstructor_ErrorTypeOnly() {
        // When
        KisApiException ex = new KisApiException(
                "Network error",
                KisApiException.ErrorType.NETWORK
        );

        // Then
        assertThat(ex.getMessage()).isEqualTo("Network error");
        assertThat(ex.getErrorType()).isEqualTo(KisApiException.ErrorType.NETWORK);
        assertThat(ex.getKisErrorCode()).isNull();
        assertThat(ex.getHttpStatusCode()).isEqualTo(0);
        assertThat(ex.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("생성자 - 모든 파라미터 사용")
    void testConstructor_AllParameters() {
        // When
        KisApiException ex = new KisApiException(
                "Rate limit exceeded",
                KisApiException.ErrorType.RATE_LIMIT,
                "RATE_001",
                429
        );

        // Then
        assertThat(ex.getMessage()).isEqualTo("Rate limit exceeded");
        assertThat(ex.getErrorType()).isEqualTo(KisApiException.ErrorType.RATE_LIMIT);
        assertThat(ex.getKisErrorCode()).isEqualTo("RATE_001");
        assertThat(ex.getHttpStatusCode()).isEqualTo(429);
        assertThat(ex.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("생성자 - cause 포함")
    void testConstructor_WithCause() {
        // Given
        Exception cause = new RuntimeException("Root cause");

        // When
        KisApiException ex = new KisApiException(
                "API call failed",
                cause,
                KisApiException.ErrorType.SERVER_ERROR
        );

        // Then
        assertThat(ex.getMessage()).isEqualTo("API call failed");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getErrorType()).isEqualTo(KisApiException.ErrorType.SERVER_ERROR);
        assertThat(ex.isRetryable()).isTrue();
    }

    @Test
    @DisplayName("재시도 가능 여부 - retryable이 errorType과 일치")
    void testRetryable_MatchesErrorType() {
        // Given
        KisApiException networkEx = new KisApiException(
                "Network error",
                KisApiException.ErrorType.NETWORK
        );

        KisApiException authEx = new KisApiException(
                "Auth error",
                KisApiException.ErrorType.AUTHENTICATION
        );

        // Then
        assertThat(networkEx.isRetryable())
                .isEqualTo(KisApiException.ErrorType.NETWORK.isRetryable());

        assertThat(authEx.isRetryable())
                .isEqualTo(KisApiException.ErrorType.AUTHENTICATION.isRetryable());
    }

    // ==================== 5. 통합 시나리오 ====================

    @Test
    @DisplayName("통합 - HTTP 429 에러 분류 및 예외 생성")
    void testIntegration_RateLimitError() {
        // When
        KisApiException.ErrorType errorType = KisApiException.classifyError(429, null);
        KisApiException ex = new KisApiException(
                "Rate limit exceeded",
                errorType,
                null,
                429
        );

        // Then
        assertThat(errorType).isEqualTo(KisApiException.ErrorType.RATE_LIMIT);
        assertThat(ex.isRetryable()).isTrue();
        assertThat(ex.getHttpStatusCode()).isEqualTo(429);
    }

    @Test
    @DisplayName("통합 - KIS 에러 코드 기반 분류 및 예외 생성")
    void testIntegration_KisErrorCode() {
        // When
        KisApiException.ErrorType errorType = KisApiException.classifyError(400, "40001");
        KisApiException ex = new KisApiException(
                "Invalid parameter",
                errorType,
                "40001",
                400
        );

        // Then
        assertThat(errorType).isEqualTo(KisApiException.ErrorType.INVALID_REQUEST);
        assertThat(ex.isRetryable()).isFalse();
        assertThat(ex.getKisErrorCode()).isEqualTo("40001");
    }
}
