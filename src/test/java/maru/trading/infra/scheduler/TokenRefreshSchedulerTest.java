package maru.trading.infra.scheduler;

import maru.trading.application.ports.repo.BrokerTokenRepository;
import maru.trading.application.usecase.auth.RefreshTokenUseCase;
import maru.trading.broker.kis.config.KisProperties;
import maru.trading.domain.account.BrokerToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshScheduler Test")
class TokenRefreshSchedulerTest {

    @Mock
    private BrokerTokenRepository tokenRepository;

    @Mock
    private RefreshTokenUseCase refreshTokenUseCase;

    @Mock
    private KisProperties kisProperties;

    @Mock
    private KisProperties.EnvironmentConfig liveConfig;

    @Mock
    private KisProperties.EnvironmentConfig paperConfig;

    private TokenRefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TokenRefreshScheduler(tokenRepository, refreshTokenUseCase, kisProperties);
    }

    @Nested
    @DisplayName("checkAndRefreshTokens() Tests")
    class CheckAndRefreshTokensTests {

        @Test
        @DisplayName("Should skip when no tokens need refresh")
        void shouldSkipWhenNoTokensNeedRefresh() {
            // Given
            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            scheduler.checkAndRefreshTokens();

            // Then
            verify(tokenRepository).findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class));
            verifyNoInteractions(refreshTokenUseCase);
        }

        @Test
        @DisplayName("Should refresh PAPER token")
        void shouldRefreshPaperToken() {
            // Given
            BrokerToken expiringToken = createToken("TOKEN_001", "KIS", "PAPER",
                    LocalDateTime.now().plusMinutes(3));

            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(expiringToken));
            when(kisProperties.getPaper()).thenReturn(paperConfig);
            when(paperConfig.getAppKey()).thenReturn("paper-app-key");
            when(paperConfig.getAppSecret()).thenReturn("paper-app-secret");
            when(refreshTokenUseCase.execute(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(createToken("TOKEN_001", "KIS", "PAPER", LocalDateTime.now().plusHours(24)));

            // When
            scheduler.checkAndRefreshTokens();

            // Then
            verify(refreshTokenUseCase).execute("KIS", "PAPER", "paper-app-key", "paper-app-secret");
        }

        @Test
        @DisplayName("Should refresh LIVE token")
        void shouldRefreshLiveToken() {
            // Given
            BrokerToken expiringToken = createToken("TOKEN_002", "KIS", "LIVE",
                    LocalDateTime.now().plusMinutes(3));

            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(expiringToken));
            when(kisProperties.getLive()).thenReturn(liveConfig);
            when(liveConfig.getAppKey()).thenReturn("live-app-key");
            when(liveConfig.getAppSecret()).thenReturn("live-app-secret");
            when(refreshTokenUseCase.execute(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(createToken("TOKEN_002", "KIS", "LIVE", LocalDateTime.now().plusHours(24)));

            // When
            scheduler.checkAndRefreshTokens();

            // Then
            verify(refreshTokenUseCase).execute("KIS", "LIVE", "live-app-key", "live-app-secret");
        }

        @Test
        @DisplayName("Should refresh multiple tokens")
        void shouldRefreshMultipleTokens() {
            // Given
            BrokerToken token1 = createToken("TOKEN_001", "KIS", "PAPER", LocalDateTime.now().plusMinutes(3));
            BrokerToken token2 = createToken("TOKEN_002", "KIS", "PAPER", LocalDateTime.now().plusMinutes(4));

            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(token1, token2));
            when(kisProperties.getPaper()).thenReturn(paperConfig);
            when(paperConfig.getAppKey()).thenReturn("paper-app-key");
            when(paperConfig.getAppSecret()).thenReturn("paper-app-secret");
            when(refreshTokenUseCase.execute(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(createToken("TOKEN", "KIS", "PAPER", LocalDateTime.now().plusHours(24)));

            // When
            scheduler.checkAndRefreshTokens();

            // Then
            verify(refreshTokenUseCase, times(2)).execute(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should skip token when app credentials are missing")
        void shouldSkipTokenWhenAppCredentialsAreMissing() {
            // Given
            BrokerToken token = createToken("TOKEN_001", "KIS", "PAPER", LocalDateTime.now().plusMinutes(3));

            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(token));
            when(kisProperties.getPaper()).thenReturn(paperConfig);
            when(paperConfig.getAppKey()).thenReturn(null);
            when(paperConfig.getAppSecret()).thenReturn(null);

            // When
            scheduler.checkAndRefreshTokens();

            // Then
            verifyNoInteractions(refreshTokenUseCase);
        }

        @Test
        @DisplayName("Should handle refresh failure gracefully")
        void shouldHandleRefreshFailureGracefully() {
            // Given
            BrokerToken token = createToken("TOKEN_001", "KIS", "PAPER", LocalDateTime.now().plusMinutes(3));

            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(token));
            when(kisProperties.getPaper()).thenReturn(paperConfig);
            when(paperConfig.getAppKey()).thenReturn("paper-app-key");
            when(paperConfig.getAppSecret()).thenReturn("paper-app-secret");
            when(refreshTokenUseCase.execute(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("API error"));

            // When - should not throw exception
            scheduler.checkAndRefreshTokens();

            // Then
            verify(refreshTokenUseCase).execute(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle repository exception gracefully")
        void shouldHandleRepositoryExceptionGracefully() {
            // Given
            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenThrow(new RuntimeException("Database error"));

            // When - should not throw exception
            scheduler.checkAndRefreshTokens();

            // Then
            verifyNoInteractions(refreshTokenUseCase);
        }

        @Test
        @DisplayName("Should continue refreshing other tokens after one failure")
        void shouldContinueRefreshingOtherTokensAfterOneFailure() {
            // Given
            BrokerToken token1 = createToken("TOKEN_001", "KIS", "PAPER", LocalDateTime.now().plusMinutes(3));
            BrokerToken token2 = createToken("TOKEN_002", "KIS", "PAPER", LocalDateTime.now().plusMinutes(4));

            when(tokenRepository.findTokensNeedingRefresh(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(token1, token2));
            when(kisProperties.getPaper()).thenReturn(paperConfig);
            when(paperConfig.getAppKey()).thenReturn("paper-app-key");
            when(paperConfig.getAppSecret()).thenReturn("paper-app-secret");

            // First token fails, second succeeds
            when(refreshTokenUseCase.execute(anyString(), anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("API error"))
                    .thenReturn(createToken("TOKEN_002", "KIS", "PAPER", LocalDateTime.now().plusHours(24)));

            // When
            scheduler.checkAndRefreshTokens();

            // Then - both tokens should be attempted
            verify(refreshTokenUseCase, times(2)).execute(anyString(), anyString(), anyString(), anyString());
        }
    }

    // ==================== Helper Methods ====================

    private BrokerToken createToken(String tokenId, String broker, String environment, LocalDateTime expiresAt) {
        return new BrokerToken(
                tokenId,
                broker,
                environment,
                "access-token-value",
                LocalDateTime.now().minusHours(23), // issuedAt
                expiresAt
        );
    }
}
