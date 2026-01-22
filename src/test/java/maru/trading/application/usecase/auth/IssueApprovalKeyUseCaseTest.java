package maru.trading.application.usecase.auth;

import maru.trading.broker.kis.auth.KisAuthenticationClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IssueApprovalKeyUseCase Test")
class IssueApprovalKeyUseCaseTest {

    @Mock
    private KisAuthenticationClient authClient;

    @InjectMocks
    private IssueApprovalKeyUseCase issueApprovalKeyUseCase;

    @Test
    @DisplayName("Should issue approval key for PAPER environment")
    void shouldIssueApprovalKeyForPaperEnvironment() {
        // Given
        String appKey = "TEST_APP_KEY";
        String appSecret = "TEST_APP_SECRET";
        String expectedKey = "APPROVAL_KEY_12345";

        when(authClient.issueApprovalKey(appKey, appSecret, false)).thenReturn(expectedKey);

        // When
        String result = issueApprovalKeyUseCase.execute("KIS", "PAPER", appKey, appSecret);

        // Then
        assertThat(result).isEqualTo(expectedKey);
        verify(authClient).issueApprovalKey(appKey, appSecret, false);
    }

    @Test
    @DisplayName("Should issue approval key for LIVE environment")
    void shouldIssueApprovalKeyForLiveEnvironment() {
        // Given
        String appKey = "TEST_APP_KEY";
        String appSecret = "TEST_APP_SECRET";
        String expectedKey = "APPROVAL_KEY_LIVE_12345";

        when(authClient.issueApprovalKey(appKey, appSecret, true)).thenReturn(expectedKey);

        // When
        String result = issueApprovalKeyUseCase.execute("KIS", "LIVE", appKey, appSecret);

        // Then
        assertThat(result).isEqualTo(expectedKey);
        verify(authClient).issueApprovalKey(appKey, appSecret, true);
    }

    @Test
    @DisplayName("Should throw exception when auth client fails")
    void shouldThrowExceptionWhenAuthClientFails() {
        // Given
        when(authClient.issueApprovalKey(any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("Authentication failed"));

        // When & Then
        assertThatThrownBy(() ->
                issueApprovalKeyUseCase.execute("KIS", "PAPER", "key", "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Approval key issuance failed");
    }

    @Test
    @DisplayName("Should handle case-insensitive environment")
    void shouldHandleCaseInsensitiveEnvironment() {
        // Given
        String expectedKey = "APPROVAL_KEY_12345";
        when(authClient.issueApprovalKey(any(), any(), eq(true))).thenReturn(expectedKey);

        // When
        String result = issueApprovalKeyUseCase.execute("KIS", "live", "key", "secret");

        // Then
        assertThat(result).isEqualTo(expectedKey);
        verify(authClient).issueApprovalKey(any(), any(), eq(true));
    }
}
