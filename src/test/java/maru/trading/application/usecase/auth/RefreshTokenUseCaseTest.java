package maru.trading.application.usecase.auth;

import maru.trading.application.ports.repo.BrokerTokenRepository;
import maru.trading.broker.kis.auth.KisAuthenticationClient;
import maru.trading.broker.kis.dto.KisTokenResponse;
import maru.trading.domain.account.BrokerToken;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenUseCase Test")
class RefreshTokenUseCaseTest {

    @Mock
    private BrokerTokenRepository tokenRepository;

    @Mock
    private KisAuthenticationClient authClient;

    @Mock
    private OutboxService outboxService;

    @Mock
    private UlidGenerator ulidGenerator;

    @InjectMocks
    private RefreshTokenUseCase refreshTokenUseCase;

    @Test
    @DisplayName("Should refresh token successfully for PAPER environment")
    void shouldRefreshTokenSuccessfullyForPaperEnvironment() {
        // Given
        String appKey = "TEST_APP_KEY";
        String appSecret = "TEST_APP_SECRET";

        KisTokenResponse response = new KisTokenResponse();
        response.setAccessToken("NEW_ACCESS_TOKEN");
        response.setTokenType("Bearer");
        response.setExpiresIn(86400);

        when(authClient.issueToken(appKey, appSecret, true)).thenReturn(response);
        when(ulidGenerator.generateInstance()).thenReturn("TOKEN_001", "EVENT_001");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BrokerToken result = refreshTokenUseCase.execute("KIS", "PAPER", appKey, appSecret);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTokenId()).isEqualTo("TOKEN_001");
        assertThat(result.getBroker()).isEqualTo("KIS");
        assertThat(result.getEnvironment()).isEqualTo("PAPER");
        assertThat(result.getAccessToken()).isEqualTo("NEW_ACCESS_TOKEN");
        verify(tokenRepository).save(any());
    }

    @Test
    @DisplayName("Should refresh token successfully for LIVE environment")
    void shouldRefreshTokenSuccessfullyForLiveEnvironment() {
        // Given
        String appKey = "TEST_APP_KEY";
        String appSecret = "TEST_APP_SECRET";

        KisTokenResponse response = new KisTokenResponse();
        response.setAccessToken("LIVE_ACCESS_TOKEN");
        response.setTokenType("Bearer");
        response.setExpiresIn(86400);

        when(authClient.issueToken(appKey, appSecret, false)).thenReturn(response);
        when(ulidGenerator.generateInstance()).thenReturn("TOKEN_001", "EVENT_001");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BrokerToken result = refreshTokenUseCase.execute("KIS", "LIVE", appKey, appSecret);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEnvironment()).isEqualTo("LIVE");
        verify(authClient).issueToken(appKey, appSecret, false);
    }

    @Test
    @DisplayName("Should publish TokenRefreshed event")
    void shouldPublishTokenRefreshedEvent() {
        // Given
        KisTokenResponse response = new KisTokenResponse();
        response.setAccessToken("NEW_ACCESS_TOKEN");
        response.setTokenType("Bearer");
        response.setExpiresIn(86400);

        when(authClient.issueToken(any(), any(), anyBoolean())).thenReturn(response);
        when(ulidGenerator.generateInstance()).thenReturn("TOKEN_001", "EVENT_001");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        refreshTokenUseCase.execute("KIS", "PAPER", "key", "secret");

        // Then
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxService).save(eventCaptor.capture());

        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getEventType()).isEqualTo("TokenRefreshed");
        assertThat(event.getPayload()).containsEntry("tokenId", "TOKEN_001");
        assertThat(event.getPayload()).containsEntry("broker", "KIS");
    }

    @Test
    @DisplayName("Should set correct expiration time")
    void shouldSetCorrectExpirationTime() {
        // Given
        KisTokenResponse response = new KisTokenResponse();
        response.setAccessToken("NEW_ACCESS_TOKEN");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600); // 1 hour

        when(authClient.issueToken(any(), any(), anyBoolean())).thenReturn(response);
        when(ulidGenerator.generateInstance()).thenReturn("TOKEN_001", "EVENT_001");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        BrokerToken result = refreshTokenUseCase.execute("KIS", "PAPER", "key", "secret");

        // Then
        assertThat(result.getExpiresAt()).isAfter(result.getIssuedAt());
        // Should expire roughly 1 hour from now
        assertThat(result.getExpiresAt()).isAfterOrEqualTo(result.getIssuedAt().plusSeconds(3600));
    }
}
