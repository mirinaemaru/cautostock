package maru.trading.broker.kis.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketErrorClassifier Test")
class WebSocketErrorClassifierTest {

    private WebSocketErrorClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new WebSocketErrorClassifier();
    }

    @Nested
    @DisplayName("classify() Tests")
    class ClassifyTests {

        @Test
        @DisplayName("Should classify SocketException as NETWORK error")
        void shouldClassifySocketExceptionAsNetworkError() {
            // Given
            SocketException exception = new SocketException("Connection reset");

            // When
            WebSocketError error = classifier.classify(exception, "Connection reset");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.NETWORK);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.RECONNECT);
        }

        @Test
        @DisplayName("Should classify SocketTimeoutException as NETWORK error")
        void shouldClassifySocketTimeoutExceptionAsNetworkError() {
            // Given
            SocketTimeoutException exception = new SocketTimeoutException("Connection timed out");

            // When
            WebSocketError error = classifier.classify(exception, "Timeout");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.NETWORK);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.RECONNECT);
        }

        @Test
        @DisplayName("Should classify TimeoutException as NETWORK error")
        void shouldClassifyTimeoutExceptionAsNetworkError() {
            // Given
            TimeoutException exception = new TimeoutException("Timed out");

            // When
            WebSocketError error = classifier.classify(exception, "Timeout");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.NETWORK);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.RECONNECT);
        }

        @Test
        @DisplayName("Should classify IOException as NETWORK error")
        void shouldClassifyIOExceptionAsNetworkError() {
            // Given
            IOException exception = new IOException("I/O error");

            // When
            WebSocketError error = classifier.classify(exception, "I/O error");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.NETWORK);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.RECONNECT);
        }

        @Test
        @DisplayName("Should classify authentication error from message")
        void shouldClassifyAuthenticationErrorFromMessage() {
            // Given
            RuntimeException exception = new RuntimeException("Auth failed");

            // When
            WebSocketError error = classifier.classify(exception, "authentication failed");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.AUTHENTICATION);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.KILL_SWITCH);
        }

        @Test
        @DisplayName("Should classify 401 error as AUTHENTICATION")
        void shouldClassify401ErrorAsAuthentication() {
            // Given
            RuntimeException exception = new RuntimeException("Unauthorized");

            // When
            WebSocketError error = classifier.classify(exception, "HTTP 401 unauthorized");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.AUTHENTICATION);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.KILL_SWITCH);
        }

        @Test
        @DisplayName("Should classify 403 error as AUTHENTICATION")
        void shouldClassify403ErrorAsAuthentication() {
            // Given
            RuntimeException exception = new RuntimeException("Forbidden");

            // When
            WebSocketError error = classifier.classify(exception, "HTTP 403 forbidden");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.AUTHENTICATION);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.KILL_SWITCH);
        }

        @Test
        @DisplayName("Should classify approval_key error as AUTHENTICATION")
        void shouldClassifyApprovalKeyErrorAsAuthentication() {
            // Given
            RuntimeException exception = new RuntimeException("Invalid key");

            // When
            WebSocketError error = classifier.classify(exception, "approval_key is invalid");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.AUTHENTICATION);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.KILL_SWITCH);
        }

        @Test
        @DisplayName("Should classify parse error as DATA_PARSING")
        void shouldClassifyParseErrorAsDataParsing() {
            // Given
            RuntimeException exception = new RuntimeException("Parse error");

            // When
            WebSocketError error = classifier.classify(exception, "Failed to parse response");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.DATA_PARSING);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.LOG_AND_CONTINUE);
        }

        @Test
        @DisplayName("Should classify invalid JSON error as DATA_PARSING")
        void shouldClassifyInvalidJsonErrorAsDataParsing() {
            // Given
            RuntimeException exception = new RuntimeException("JSON error");

            // When
            WebSocketError error = classifier.classify(exception, "invalid JSON format");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.DATA_PARSING);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.LOG_AND_CONTINUE);
        }

        @Test
        @DisplayName("Should classify malformed error as DATA_PARSING")
        void shouldClassifyMalformedErrorAsDataParsing() {
            // Given
            RuntimeException exception = new RuntimeException("Malformed");

            // When
            WebSocketError error = classifier.classify(exception, "malformed message");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.DATA_PARSING);
        }

        @Test
        @DisplayName("Should classify protocol error as PROTOCOL")
        void shouldClassifyProtocolErrorAsProtocol() {
            // Given
            RuntimeException exception = new RuntimeException("Protocol error");

            // When
            WebSocketError error = classifier.classify(exception, "protocol violation");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.PROTOCOL);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.RECONNECT);
        }

        @Test
        @DisplayName("Should classify handshake error as PROTOCOL")
        void shouldClassifyHandshakeErrorAsProtocol() {
            // Given
            RuntimeException exception = new RuntimeException("Handshake failed");

            // When
            WebSocketError error = classifier.classify(exception, "handshake failed");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.PROTOCOL);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.RECONNECT);
        }

        @Test
        @DisplayName("Should classify unknown error")
        void shouldClassifyUnknownError() {
            // Given
            RuntimeException exception = new RuntimeException("Unknown error");

            // When
            WebSocketError error = classifier.classify(exception, "Something went wrong");

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.UNKNOWN);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.ALERT);
        }

        @Test
        @DisplayName("Should handle null throwable and message")
        void shouldHandleNullThrowableAndMessage() {
            // When
            WebSocketError error = classifier.classify(null, null);

            // Then
            assertThat(error.getType()).isEqualTo(WebSocketError.ErrorType.UNKNOWN);
            assertThat(error.getRecoveryAction()).isEqualTo(WebSocketError.RecoveryAction.ALERT);
        }
    }

    @Nested
    @DisplayName("isCritical() Tests")
    class IsCriticalTests {

        @Test
        @DisplayName("Should return true for AUTHENTICATION error")
        void shouldReturnTrueForAuthenticationError() {
            // Given
            WebSocketError error = classifier.classify(null, "authentication failed");

            // When/Then
            assertThat(classifier.isCritical(error)).isTrue();
        }

        @Test
        @DisplayName("Should return true for KILL_SWITCH recovery action")
        void shouldReturnTrueForKillSwitchRecoveryAction() {
            // Given
            WebSocketError error = classifier.classify(null, "401 unauthorized");

            // When/Then
            assertThat(classifier.isCritical(error)).isTrue();
        }

        @Test
        @DisplayName("Should return false for NETWORK error")
        void shouldReturnFalseForNetworkError() {
            // Given
            WebSocketError error = classifier.classify(new SocketException("Connection reset"), "Connection reset");

            // When/Then
            assertThat(classifier.isCritical(error)).isFalse();
        }

        @Test
        @DisplayName("Should return false for DATA_PARSING error")
        void shouldReturnFalseForDataParsingError() {
            // Given
            WebSocketError error = classifier.classify(null, "parse error");

            // When/Then
            assertThat(classifier.isCritical(error)).isFalse();
        }
    }

    @Nested
    @DisplayName("isRecoverable() Tests")
    class IsRecoverableTests {

        @Test
        @DisplayName("Should return true for NETWORK error")
        void shouldReturnTrueForNetworkError() {
            // Given
            WebSocketError error = classifier.classify(new SocketException("Connection reset"), "Connection reset");

            // When/Then
            assertThat(classifier.isRecoverable(error)).isTrue();
        }

        @Test
        @DisplayName("Should return true for PROTOCOL error")
        void shouldReturnTrueForProtocolError() {
            // Given
            WebSocketError error = classifier.classify(null, "protocol error");

            // When/Then
            assertThat(classifier.isRecoverable(error)).isTrue();
        }

        @Test
        @DisplayName("Should return false for AUTHENTICATION error")
        void shouldReturnFalseForAuthenticationError() {
            // Given
            WebSocketError error = classifier.classify(null, "authentication failed");

            // When/Then
            assertThat(classifier.isRecoverable(error)).isFalse();
        }

        @Test
        @DisplayName("Should return false for DATA_PARSING error")
        void shouldReturnFalseForDataParsingError() {
            // Given
            WebSocketError error = classifier.classify(null, "parse error");

            // When/Then
            assertThat(classifier.isRecoverable(error)).isFalse();
        }
    }
}
