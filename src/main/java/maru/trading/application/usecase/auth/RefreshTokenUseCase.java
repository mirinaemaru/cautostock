package maru.trading.application.usecase.auth;

import maru.trading.application.ports.repo.BrokerTokenRepository;
import maru.trading.broker.kis.auth.KisAuthenticationClient;
import maru.trading.broker.kis.dto.KisTokenResponse;
import maru.trading.domain.account.BrokerToken;
import maru.trading.infra.config.UlidGenerator;
import maru.trading.infra.messaging.outbox.OutboxEvent;
import maru.trading.infra.messaging.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Use case for refreshing broker authentication token.
 *
 * This use case:
 * 1. Calls KIS authentication client to issue new token
 * 2. Saves token to database
 * 3. Publishes TokenRefreshed event
 */
@Service
public class RefreshTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCase.class);

    private final BrokerTokenRepository tokenRepository;
    private final KisAuthenticationClient authClient;
    private final OutboxService outboxService;
    private final UlidGenerator ulidGenerator;

    public RefreshTokenUseCase(
            BrokerTokenRepository tokenRepository,
            KisAuthenticationClient authClient,
            OutboxService outboxService,
            UlidGenerator ulidGenerator) {
        this.tokenRepository = tokenRepository;
        this.authClient = authClient;
        this.outboxService = outboxService;
        this.ulidGenerator = ulidGenerator;
    }

    /**
     * Execute the use case to refresh token.
     *
     * @param broker Broker name (e.g., "KIS")
     * @param environment Environment ("PAPER" or "LIVE")
     * @param appKey Application key
     * @param appSecret Application secret
     * @return New broker token
     */
    @Transactional
    public BrokerToken execute(String broker, String environment, String appKey, String appSecret) {
        log.info("Refreshing token for broker: {}, environment: {}", broker, environment);

        // Step 1: Call KIS authentication client to issue new token
        boolean isPaper = "PAPER".equalsIgnoreCase(environment);
        KisTokenResponse response = authClient.issueToken(appKey, appSecret, isPaper);

        log.debug("Received token response: tokenType={}, expiresIn={}",
                response.getTokenType(), response.getExpiresIn());

        // Step 2: Create domain model
        String tokenId = ulidGenerator.generateInstance();
        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(response.getExpiresIn());

        BrokerToken token = new BrokerToken(
                tokenId,
                broker,
                environment,
                response.getAccessToken(),
                issuedAt,
                expiresAt
        );

        // Step 3: Save token to database
        BrokerToken savedToken = tokenRepository.save(token);
        log.info("Token saved: tokenId={}, expiresAt={}", savedToken.getTokenId(), savedToken.getExpiresAt());

        // Step 4: Publish TokenRefreshed event
        publishTokenRefreshedEvent(savedToken);

        return savedToken;
    }

    private void publishTokenRefreshedEvent(BrokerToken token) {
        String eventId = ulidGenerator.generateInstance();
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType("TokenRefreshed")
                .occurredAt(LocalDateTime.now())
                .payload(Map.of(
                        "tokenId", token.getTokenId(),
                        "broker", token.getBroker(),
                        "environment", token.getEnvironment(),
                        "expiresAt", token.getExpiresAt()
                ))
                .build();
        outboxService.save(event);
        log.debug("Published TokenRefreshed event: {}", eventId);
    }
}
