package maru.trading.application.usecase.auth;

import maru.trading.broker.kis.auth.KisAuthenticationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Use case for issuing WebSocket approval key.
 *
 * This use case:
 * 1. Calls KIS authentication client to issue approval key
 * 2. Returns approval key (no database storage per spec)
 *
 * Used by WebSocket connection initialization.
 */
@Service
public class IssueApprovalKeyUseCase {

    private static final Logger log = LoggerFactory.getLogger(IssueApprovalKeyUseCase.class);

    private final KisAuthenticationClient authClient;

    public IssueApprovalKeyUseCase(KisAuthenticationClient authClient) {
        this.authClient = authClient;
    }

    /**
     * Execute the use case to issue approval key.
     *
     * @param broker Broker name (e.g., "KIS")
     * @param environment Environment ("PAPER" or "LIVE")
     * @param appKey Application key
     * @param appSecret Application secret
     * @return Approval key for WebSocket connection
     */
    public String execute(String broker, String environment, String appKey, String appSecret) {
        log.info("Issuing approval key for broker: {}, environment: {}", broker, environment);

        // Call KIS authentication client
        String approvalKey = authClient.issueApprovalKey(appKey, appSecret);

        log.debug("Approval key issued: {}", approvalKey.substring(0, Math.min(10, approvalKey.length())) + "...");

        return approvalKey;
    }
}
