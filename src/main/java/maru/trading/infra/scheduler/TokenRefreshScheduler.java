package maru.trading.infra.scheduler;

import maru.trading.application.ports.repo.BrokerTokenRepository;
import maru.trading.application.usecase.auth.RefreshTokenUseCase;
import maru.trading.broker.kis.config.KisProperties;
import maru.trading.domain.account.BrokerToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for proactive token refresh.
 *
 * Runs every 1 minute to check for tokens expiring soon.
 * Refreshes tokens that expire within 5 minutes.
 *
 * Prevents token expiration during active trading.
 */
@Component
public class TokenRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshScheduler.class);
    private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(5);

    private final BrokerTokenRepository tokenRepository;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final KisProperties kisProperties;

    public TokenRefreshScheduler(
            BrokerTokenRepository tokenRepository,
            RefreshTokenUseCase refreshTokenUseCase,
            KisProperties kisProperties) {
        this.tokenRepository = tokenRepository;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.kisProperties = kisProperties;
    }

    /**
     * Check and refresh tokens every 1 minute.
     * Optimized: Only queries tokens that actually need refresh (expiring within threshold).
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    public void checkAndRefreshTokens() {
        log.debug("Checking tokens for refresh");

        try {
            // Optimized: Only query tokens expiring within threshold (not yet expired)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresBeforeThreshold = now.plus(REFRESH_THRESHOLD);

            List<BrokerToken> tokensNeedingRefresh = tokenRepository.findTokensNeedingRefresh(now, expiresBeforeThreshold);

            if (tokensNeedingRefresh.isEmpty()) {
                log.debug("No tokens need refresh at this time");
                return;
            }

            log.info("Found {} tokens needing refresh", tokensNeedingRefresh.size());

            // Refresh each token
            for (BrokerToken token : tokensNeedingRefresh) {
                log.info("Token needs refresh: tokenId={}, broker={}, environment={}, expiresAt={}",
                        token.getTokenId(),
                        token.getBroker(),
                        token.getEnvironment(),
                        token.getExpiresAt());

                refreshToken(token);
            }

        } catch (Exception e) {
            log.error("Error in token refresh scheduler", e);
        }
    }

    /**
     * Refresh a single token.
     */
    private void refreshToken(BrokerToken token) {
        try {
            // Get app credentials from config based on environment
            KisProperties.EnvironmentConfig envConfig = "LIVE".equalsIgnoreCase(token.getEnvironment())
                    ? kisProperties.getLive()
                    : kisProperties.getPaper();

            String appKey = envConfig.getAppKey();
            String appSecret = envConfig.getAppSecret();

            if (appKey == null || appSecret == null) {
                log.warn("Missing app credentials, skipping token refresh: tokenId={}", token.getTokenId());
                return;
            }

            // Refresh token
            BrokerToken refreshedToken = refreshTokenUseCase.execute(
                    token.getBroker(),
                    token.getEnvironment(),
                    appKey,
                    appSecret
            );

            log.info("Token refreshed successfully: tokenId={}, newExpiresAt={}",
                    refreshedToken.getTokenId(),
                    refreshedToken.getExpiresAt());

        } catch (Exception e) {
            log.error("Failed to refresh token: tokenId={}", token.getTokenId(), e);
            // In production, could trigger alert for manual intervention
        }
    }
}
