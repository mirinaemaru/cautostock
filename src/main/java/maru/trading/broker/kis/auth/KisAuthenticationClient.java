package maru.trading.broker.kis.auth;

import maru.trading.broker.kis.dto.KisTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * STUB implementation of KIS authentication client.
 *
 * In MVP, this returns mock tokens without making real API calls.
 * In production, this would use RestTemplate/WebClient to call:
 * - POST /oauth2/tokenP (for PAPER environment)
 * - POST /oauth2/tokenL (for LIVE environment)
 * - POST /oauth2/Approval (for WebSocket approval key)
 */
@Component
public class KisAuthenticationClient {

    private static final Logger log = LoggerFactory.getLogger(KisAuthenticationClient.class);

    /**
     * Issue OAuth2 access token (STUB).
     *
     * @param appKey Application key
     * @param appSecret Application secret
     * @param isPaper True for PAPER environment, false for LIVE
     * @return Token response with mock token
     */
    public KisTokenResponse issueToken(String appKey, String appSecret, boolean isPaper) {
        log.info("[STUB] Issuing token for appKey={}, isPaper={}", appKey, isPaper);

        // Generate mock token
        String mockToken = "STUB_TOKEN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        int expiresIn = 3600; // 1 hour

        log.info("[STUB] Token issued: expiresIn={} seconds", expiresIn);

        return new KisTokenResponse(mockToken, "Bearer", expiresIn);
    }

    /**
     * Issue WebSocket approval key (STUB).
     *
     * @param appKey Application key
     * @param appSecret Application secret
     * @return Approval key for WebSocket subscription
     */
    public String issueApprovalKey(String appKey, String appSecret) {
        log.info("[STUB] Issuing approval key for appKey={}", appKey);

        // Generate mock approval key
        String mockApprovalKey = "STUB_APPROVAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        log.info("[STUB] Approval key issued: {}", mockApprovalKey.substring(0, 15) + "...");

        return mockApprovalKey;
    }
}
