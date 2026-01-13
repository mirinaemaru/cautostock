package maru.trading.broker.kis.auth;

import maru.trading.application.ports.repo.BrokerTokenRepository;
import maru.trading.application.usecase.auth.RefreshTokenUseCase;
import maru.trading.broker.kis.config.KisProperties;
import maru.trading.domain.account.BrokerToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token lifecycle manager with in-memory cache.
 *
 * Responsibilities:
 * - Get valid token from cache or database
 * - Refresh token if expired or expiring soon
 * - Cache tokens in memory for fast access
 */
@Component
public class KisTokenManager {

    private static final Logger log = LoggerFactory.getLogger(KisTokenManager.class);
    private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(5);

    private final BrokerTokenRepository tokenRepository;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final KisProperties kisProperties;
    private final Map<TokenKey, BrokerToken> tokenCache;

    @Value("${spring.profiles.active:paper}")
    private String activeProfile;

    public KisTokenManager(
            BrokerTokenRepository tokenRepository,
            RefreshTokenUseCase refreshTokenUseCase,
            KisProperties kisProperties) {
        this.tokenRepository = tokenRepository;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.kisProperties = kisProperties;
        this.tokenCache = new ConcurrentHashMap<>();
    }

    /**
     * Get access token for current active profile.
     * Convenience method that uses the active profile to determine environment.
     *
     * @return Valid access token string
     */
    public String getAccessToken() {
        String environment = "live".equalsIgnoreCase(activeProfile) ? "LIVE" : "PAPER";
        KisProperties.EnvironmentConfig envConfig = "LIVE".equals(environment)
                ? kisProperties.getLive()
                : kisProperties.getPaper();

        BrokerToken token = getValidToken("KIS", environment,
                envConfig.getAppKey(), envConfig.getAppSecret());
        return token.getAccessToken();
    }

    /**
     * Get a valid token for the broker and environment.
     * Returns cached token if valid, otherwise fetches from database or refreshes.
     *
     * @param broker Broker name (e.g., "KIS")
     * @param environment Environment ("PAPER" or "LIVE")
     * @param appKey Application key (for refresh)
     * @param appSecret Application secret (for refresh)
     * @return Valid broker token
     */
    public BrokerToken getValidToken(String broker, String environment, String appKey, String appSecret) {
        TokenKey key = new TokenKey(broker, environment);

        // Check cache first
        BrokerToken cachedToken = tokenCache.get(key);
        if (cachedToken != null && !cachedToken.needsRefresh(REFRESH_THRESHOLD)) {
            log.debug("Returning cached token: broker={}, environment={}", broker, environment);
            return cachedToken;
        }

        // Check database
        Optional<BrokerToken> dbToken = tokenRepository.findValidToken(broker, environment);
        if (dbToken.isPresent() && !dbToken.get().needsRefresh(REFRESH_THRESHOLD)) {
            log.debug("Returning token from database: broker={}, environment={}", broker, environment);
            BrokerToken token = dbToken.get();
            tokenCache.put(key, token); // Update cache
            return token;
        }

        // Refresh token
        log.info("Token expired or expiring soon, refreshing: broker={}, environment={}", broker, environment);
        BrokerToken refreshedToken = refreshTokenUseCase.execute(broker, environment, appKey, appSecret);
        tokenCache.put(key, refreshedToken); // Update cache
        return refreshedToken;
    }

    /**
     * Refresh token if it needs refresh.
     *
     * @param token Token to check
     * @param appKey Application key
     * @param appSecret Application secret
     * @return Refreshed token if needed, otherwise original token
     */
    public BrokerToken refreshIfNeeded(BrokerToken token, String appKey, String appSecret) {
        if (token.needsRefresh(REFRESH_THRESHOLD)) {
            log.info("Refreshing token: tokenId={}", token.getTokenId());
            BrokerToken refreshedToken = refreshTokenUseCase.execute(
                    token.getBroker(),
                    token.getEnvironment(),
                    appKey,
                    appSecret
            );
            TokenKey key = new TokenKey(token.getBroker(), token.getEnvironment());
            tokenCache.put(key, refreshedToken); // Update cache
            return refreshedToken;
        }
        return token;
    }

    /**
     * Clear cache (for testing or manual refresh).
     */
    public void clearCache() {
        log.info("Clearing token cache");
        tokenCache.clear();
    }

    /**
     * Cache key for token lookup.
     */
    private static class TokenKey {
        private final String broker;
        private final String environment;

        public TokenKey(String broker, String environment) {
            this.broker = broker;
            this.environment = environment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TokenKey tokenKey = (TokenKey) o;
            return broker.equals(tokenKey.broker) && environment.equals(tokenKey.environment);
        }

        @Override
        public int hashCode() {
            return 31 * broker.hashCode() + environment.hashCode();
        }
    }
}
